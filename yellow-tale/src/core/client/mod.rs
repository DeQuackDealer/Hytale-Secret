use reqwest::Client;
use serde::{Deserialize, Serialize};
use thiserror::Error;
use uuid::Uuid;

#[derive(Debug, Error)]
pub enum ClientError {
    #[error("Network error: {0}")]
    Network(#[from] reqwest::Error),
    #[error("API error: {0}")]
    Api(String),
    #[error("Not authenticated")]
    NotAuthenticated,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: Uuid,
    pub username: String,
    pub display_name: Option<String>,
    pub avatar_url: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
struct ApiResponse<T> {
    success: bool,
    data: Option<T>,
    error: Option<String>,
}

#[derive(Debug, Serialize)]
struct SignupRequest {
    username: String,
    email: String,
    password: String,
}

#[derive(Debug, Serialize)]
struct LoginRequest {
    username: String,
    password: String,
}

#[derive(Debug, Serialize)]
struct TokenRequest {
    token: String,
}

#[derive(Debug, Serialize)]
struct FriendRequest {
    token: String,
    target_user_id: Uuid,
}

#[derive(Debug, Serialize)]
struct ProfileUpdateRequest {
    token: String,
    display_name: Option<String>,
    avatar_url: Option<String>,
}

#[derive(Debug, Deserialize)]
struct AuthResponse {
    user: User,
    token: String,
}

#[derive(Debug, Deserialize)]
struct FriendsResponse {
    friends: Vec<User>,
}

#[derive(Debug, Deserialize)]
struct PendingResponse {
    incoming: Vec<User>,
    outgoing: Vec<User>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ReleaseInfo {
    pub version: String,
    pub date: String,
    pub downloads: Downloads,
    pub changelog: String,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Downloads {
    pub windows: String,
    pub macos: String,
    pub linux: String,
}

pub struct ApiClient {
    client: Client,
    base_url: String,
    token: Option<String>,
    current_user: Option<User>,
}

impl ApiClient {
    pub fn new(base_url: &str) -> Self {
        Self {
            client: Client::new(),
            base_url: base_url.trim_end_matches('/').to_string(),
            token: None,
            current_user: None,
        }
    }
    
    pub fn with_token(base_url: &str, token: String) -> Self {
        Self {
            client: Client::new(),
            base_url: base_url.trim_end_matches('/').to_string(),
            token: Some(token),
            current_user: None,
        }
    }
    
    pub fn is_authenticated(&self) -> bool {
        self.token.is_some()
    }
    
    pub fn token(&self) -> Option<&str> {
        self.token.as_deref()
    }
    
    pub fn current_user(&self) -> Option<&User> {
        self.current_user.as_ref()
    }
    
    pub async fn signup(&mut self, username: &str, email: &str, password: &str) -> Result<User, ClientError> {
        let resp: ApiResponse<AuthResponse> = self.client
            .post(format!("{}/api/v1/auth/signup", self.base_url))
            .json(&SignupRequest {
                username: username.to_string(),
                email: email.to_string(),
                password: password.to_string(),
            })
            .send()
            .await?
            .json()
            .await?;
        
        if let Some(data) = resp.data {
            self.token = Some(data.token);
            self.current_user = Some(data.user.clone());
            Ok(data.user)
        } else {
            Err(ClientError::Api(resp.error.unwrap_or_default()))
        }
    }
    
    pub async fn login(&mut self, username: &str, password: &str) -> Result<User, ClientError> {
        let resp: ApiResponse<AuthResponse> = self.client
            .post(format!("{}/api/v1/auth/login", self.base_url))
            .json(&LoginRequest {
                username: username.to_string(),
                password: password.to_string(),
            })
            .send()
            .await?
            .json()
            .await?;
        
        if let Some(data) = resp.data {
            self.token = Some(data.token);
            self.current_user = Some(data.user.clone());
            Ok(data.user)
        } else {
            Err(ClientError::Api(resp.error.unwrap_or_default()))
        }
    }
    
    pub async fn logout(&mut self) -> Result<(), ClientError> {
        if let Some(token) = &self.token {
            let _ = self.client
                .post(format!("{}/api/v1/auth/logout", self.base_url))
                .json(&TokenRequest { token: token.clone() })
                .send()
                .await;
        }
        self.token = None;
        self.current_user = None;
        Ok(())
    }
    
    pub async fn get_me(&mut self) -> Result<User, ClientError> {
        let token = self.token.clone().ok_or(ClientError::NotAuthenticated)?;
        
        let resp: ApiResponse<User> = self.client
            .post(format!("{}/api/v1/auth/me", self.base_url))
            .json(&TokenRequest { token })
            .send()
            .await?
            .json()
            .await?;
        
        if let Some(user) = resp.data {
            self.current_user = Some(user.clone());
            Ok(user)
        } else {
            Err(ClientError::Api(resp.error.unwrap_or_default()))
        }
    }
    
    pub async fn update_profile(&mut self, display_name: Option<&str>, avatar_url: Option<&str>) -> Result<User, ClientError> {
        let token = self.token.clone().ok_or(ClientError::NotAuthenticated)?;
        
        let resp: ApiResponse<User> = self.client
            .post(format!("{}/api/v1/profile", self.base_url))
            .json(&ProfileUpdateRequest {
                token,
                display_name: display_name.map(String::from),
                avatar_url: avatar_url.map(String::from),
            })
            .send()
            .await?
            .json()
            .await?;
        
        if let Some(user) = resp.data {
            self.current_user = Some(user.clone());
            Ok(user)
        } else {
            Err(ClientError::Api(resp.error.unwrap_or_default()))
        }
    }
    
    pub async fn get_friends(&self) -> Result<Vec<User>, ClientError> {
        let token = self.token.clone().ok_or(ClientError::NotAuthenticated)?;
        
        let resp: ApiResponse<FriendsResponse> = self.client
            .post(format!("{}/api/v1/friends", self.base_url))
            .json(&TokenRequest { token })
            .send()
            .await?
            .json()
            .await?;
        
        if let Some(data) = resp.data {
            Ok(data.friends)
        } else {
            Err(ClientError::Api(resp.error.unwrap_or_default()))
        }
    }
    
    pub async fn send_friend_request(&self, target_user_id: Uuid) -> Result<(), ClientError> {
        let token = self.token.clone().ok_or(ClientError::NotAuthenticated)?;
        
        let resp: ApiResponse<serde_json::Value> = self.client
            .post(format!("{}/api/v1/friends/request", self.base_url))
            .json(&FriendRequest { token, target_user_id })
            .send()
            .await?
            .json()
            .await?;
        
        if resp.success {
            Ok(())
        } else {
            Err(ClientError::Api(resp.error.unwrap_or_default()))
        }
    }
    
    pub async fn accept_friend_request(&self, from_user_id: Uuid) -> Result<(), ClientError> {
        let token = self.token.clone().ok_or(ClientError::NotAuthenticated)?;
        
        let resp: ApiResponse<serde_json::Value> = self.client
            .post(format!("{}/api/v1/friends/accept", self.base_url))
            .json(&FriendRequest { token, target_user_id: from_user_id })
            .send()
            .await?
            .json()
            .await?;
        
        if resp.success {
            Ok(())
        } else {
            Err(ClientError::Api(resp.error.unwrap_or_default()))
        }
    }
    
    pub async fn decline_friend_request(&self, from_user_id: Uuid) -> Result<(), ClientError> {
        let token = self.token.clone().ok_or(ClientError::NotAuthenticated)?;
        
        let resp: ApiResponse<serde_json::Value> = self.client
            .post(format!("{}/api/v1/friends/decline", self.base_url))
            .json(&FriendRequest { token, target_user_id: from_user_id })
            .send()
            .await?
            .json()
            .await?;
        
        if resp.success {
            Ok(())
        } else {
            Err(ClientError::Api(resp.error.unwrap_or_default()))
        }
    }
    
    pub async fn get_pending_requests(&self) -> Result<(Vec<User>, Vec<User>), ClientError> {
        let token = self.token.clone().ok_or(ClientError::NotAuthenticated)?;
        
        let resp: ApiResponse<PendingResponse> = self.client
            .post(format!("{}/api/v1/friends/pending", self.base_url))
            .json(&TokenRequest { token })
            .send()
            .await?
            .json()
            .await?;
        
        if let Some(data) = resp.data {
            Ok((data.incoming, data.outgoing))
        } else {
            Err(ClientError::Api(resp.error.unwrap_or_default()))
        }
    }
    
    pub async fn get_releases(&self) -> Result<ReleaseInfo, ClientError> {
        #[derive(Deserialize)]
        struct ReleasesResponse {
            latest: ReleaseInfo,
        }
        
        let resp: ReleasesResponse = self.client
            .get(format!("{}/api/v1/releases", self.base_url))
            .send()
            .await?
            .json()
            .await?;
        
        Ok(resp.latest)
    }
    
    pub async fn health_check(&self) -> Result<bool, ClientError> {
        let resp = self.client
            .get(format!("{}/health", self.base_url))
            .send()
            .await?;
        
        Ok(resp.status().is_success())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_client_creation() {
        let client = ApiClient::new("http://localhost:8080");
        assert!(!client.is_authenticated());
        assert!(client.current_user().is_none());
    }
    
    #[test]
    fn test_client_with_token() {
        let client = ApiClient::with_token("http://localhost:8080", "test_token".to_string());
        assert!(client.is_authenticated());
        assert_eq!(client.token(), Some("test_token"));
    }
}
