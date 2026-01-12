use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use thiserror::Error;
use tracing::info;
use uuid::Uuid;

#[derive(Error, Debug)]
pub enum FriendsError {
    #[error("Friend request already exists")]
    RequestExists,
    
    #[error("Already friends")]
    AlreadyFriends,
    
    #[error("Cannot friend yourself")]
    SelfFriend,
    
    #[error("User is blocked")]
    UserBlocked,
    
    #[error("Friend request not found")]
    RequestNotFound,
    
    #[error("User not found")]
    UserNotFound,
    
    #[error("Not friends")]
    NotFriends,
    
    #[error("Already blocked")]
    AlreadyBlocked,
    
    #[error("Database error: {0}")]
    Database(#[from] sqlx::Error),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum FriendshipStatus {
    Pending,
    Accepted,
    Declined,
}

impl std::fmt::Display for FriendshipStatus {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Pending => write!(f, "pending"),
            Self::Accepted => write!(f, "accepted"),
            Self::Declined => write!(f, "declined"),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Friendship {
    pub id: Uuid,
    pub user_id: Uuid,
    pub friend_id: Uuid,
    pub status: FriendshipStatus,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FriendInfo {
    pub user_id: Uuid,
    pub username: String,
    pub display_name: String,
    pub avatar_url: Option<String>,
    pub status: String,
    pub last_seen_at: Option<DateTime<Utc>>,
    pub friendship_since: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FriendRequest {
    pub id: Uuid,
    pub from_user_id: Uuid,
    pub from_username: String,
    pub from_display_name: String,
    pub from_avatar_url: Option<String>,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BlockedUser {
    pub user_id: Uuid,
    pub username: String,
    pub blocked_at: DateTime<Utc>,
    pub reason: Option<String>,
}

pub struct FriendsService {
    pool: PgPool,
}

impl FriendsService {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
    
    pub async fn send_friend_request(&self, from_user: Uuid, to_user: Uuid) -> Result<Uuid, FriendsError> {
        if from_user == to_user {
            return Err(FriendsError::SelfFriend);
        }
        
        let blocked = sqlx::query_scalar::<_, i64>(
            "SELECT COUNT(*) FROM blocks WHERE (blocker_id = $1 AND blocked_id = $2) OR (blocker_id = $2 AND blocked_id = $1)"
        )
        .bind(from_user)
        .bind(to_user)
        .fetch_one(&self.pool)
        .await?;
        
        if blocked > 0 {
            return Err(FriendsError::UserBlocked);
        }
        
        let existing = sqlx::query_as::<_, (String,)>(
            "SELECT status FROM friendships WHERE user_id = $1 AND friend_id = $2"
        )
        .bind(from_user)
        .bind(to_user)
        .fetch_optional(&self.pool)
        .await?;
        
        if let Some((status,)) = existing {
            if status == "accepted" {
                return Err(FriendsError::AlreadyFriends);
            } else if status == "pending" {
                return Err(FriendsError::RequestExists);
            }
        }
        
        let reverse = sqlx::query_as::<_, (Uuid, String)>(
            "SELECT id, status FROM friendships WHERE user_id = $1 AND friend_id = $2"
        )
        .bind(to_user)
        .bind(from_user)
        .fetch_optional(&self.pool)
        .await?;
        
        if let Some((id, status)) = reverse {
            if status == "pending" {
                sqlx::query("UPDATE friendships SET status = 'accepted', updated_at = NOW() WHERE id = $1")
                    .bind(id)
                    .execute(&self.pool)
                    .await?;
                
                sqlx::query(
                    "INSERT INTO friendships (user_id, friend_id, status) VALUES ($1, $2, 'accepted') ON CONFLICT DO NOTHING"
                )
                .bind(from_user)
                .bind(to_user)
                .execute(&self.pool)
                .await?;
                
                info!("Friend request auto-accepted: {} <-> {}", from_user, to_user);
                return Ok(id);
            }
        }
        
        let request_id: Uuid = sqlx::query_scalar(
            "INSERT INTO friendships (user_id, friend_id, status) VALUES ($1, $2, 'pending') RETURNING id"
        )
        .bind(from_user)
        .bind(to_user)
        .fetch_one(&self.pool)
        .await?;
        
        info!("Friend request sent: {} -> {}", from_user, to_user);
        Ok(request_id)
    }
    
    pub async fn accept_friend_request(&self, user_id: Uuid, from_user: Uuid) -> Result<(), FriendsError> {
        let friendship = sqlx::query_as::<_, (Uuid, String)>(
            "SELECT id, status FROM friendships WHERE user_id = $1 AND friend_id = $2"
        )
        .bind(from_user)
        .bind(user_id)
        .fetch_optional(&self.pool)
        .await?
        .ok_or(FriendsError::RequestNotFound)?;
        
        if friendship.1 != "pending" {
            return Err(FriendsError::RequestNotFound);
        }
        
        sqlx::query("UPDATE friendships SET status = 'accepted', updated_at = NOW() WHERE id = $1")
            .bind(friendship.0)
            .execute(&self.pool)
            .await?;
        
        sqlx::query(
            "INSERT INTO friendships (user_id, friend_id, status) VALUES ($1, $2, 'accepted') ON CONFLICT (user_id, friend_id) DO UPDATE SET status = 'accepted', updated_at = NOW()"
        )
        .bind(user_id)
        .bind(from_user)
        .execute(&self.pool)
        .await?;
        
        info!("Friend request accepted: {} accepted {}", user_id, from_user);
        Ok(())
    }
    
    pub async fn decline_friend_request(&self, user_id: Uuid, from_user: Uuid) -> Result<(), FriendsError> {
        let result = sqlx::query(
            "UPDATE friendships SET status = 'declined', updated_at = NOW() WHERE user_id = $1 AND friend_id = $2 AND status = 'pending'"
        )
        .bind(from_user)
        .bind(user_id)
        .execute(&self.pool)
        .await?;
        
        if result.rows_affected() == 0 {
            return Err(FriendsError::RequestNotFound);
        }
        
        info!("Friend request declined: {} declined {}", user_id, from_user);
        Ok(())
    }
    
    pub async fn remove_friend(&self, user_id: Uuid, friend_id: Uuid) -> Result<(), FriendsError> {
        let result = sqlx::query(
            "DELETE FROM friendships WHERE (user_id = $1 AND friend_id = $2) OR (user_id = $2 AND friend_id = $1)"
        )
        .bind(user_id)
        .bind(friend_id)
        .execute(&self.pool)
        .await?;
        
        if result.rows_affected() == 0 {
            return Err(FriendsError::NotFriends);
        }
        
        info!("Friendship removed: {} <-> {}", user_id, friend_id);
        Ok(())
    }
    
    pub async fn get_friends(&self, user_id: Uuid) -> Result<Vec<FriendInfo>, FriendsError> {
        let rows = sqlx::query_as::<_, (Uuid, String, String, Option<String>, String, Option<DateTime<Utc>>, DateTime<Utc>)>(
            r#"
            SELECT u.id, u.username, u.display_name, u.avatar_url, u.status, u.last_seen_at, f.created_at
            FROM friendships f
            JOIN users u ON u.id = f.friend_id
            WHERE f.user_id = $1 AND f.status = 'accepted'
            ORDER BY u.status DESC, u.last_seen_at DESC NULLS LAST
            "#
        )
        .bind(user_id)
        .fetch_all(&self.pool)
        .await?;
        
        Ok(rows.into_iter().map(|r| FriendInfo {
            user_id: r.0,
            username: r.1,
            display_name: r.2,
            avatar_url: r.3,
            status: r.4,
            last_seen_at: r.5,
            friendship_since: r.6,
        }).collect())
    }
    
    pub async fn get_pending_requests(&self, user_id: Uuid) -> Result<Vec<FriendRequest>, FriendsError> {
        let rows = sqlx::query_as::<_, (Uuid, Uuid, String, String, Option<String>, DateTime<Utc>)>(
            r#"
            SELECT f.id, u.id, u.username, u.display_name, u.avatar_url, f.created_at
            FROM friendships f
            JOIN users u ON u.id = f.user_id
            WHERE f.friend_id = $1 AND f.status = 'pending'
            ORDER BY f.created_at DESC
            "#
        )
        .bind(user_id)
        .fetch_all(&self.pool)
        .await?;
        
        Ok(rows.into_iter().map(|r| FriendRequest {
            id: r.0,
            from_user_id: r.1,
            from_username: r.2,
            from_display_name: r.3,
            from_avatar_url: r.4,
            created_at: r.5,
        }).collect())
    }
    
    pub async fn get_outgoing_requests(&self, user_id: Uuid) -> Result<Vec<FriendRequest>, FriendsError> {
        let rows = sqlx::query_as::<_, (Uuid, Uuid, String, String, Option<String>, DateTime<Utc>)>(
            r#"
            SELECT f.id, u.id, u.username, u.display_name, u.avatar_url, f.created_at
            FROM friendships f
            JOIN users u ON u.id = f.friend_id
            WHERE f.user_id = $1 AND f.status = 'pending'
            ORDER BY f.created_at DESC
            "#
        )
        .bind(user_id)
        .fetch_all(&self.pool)
        .await?;
        
        Ok(rows.into_iter().map(|r| FriendRequest {
            id: r.0,
            from_user_id: r.1,
            from_username: r.2,
            from_display_name: r.3,
            from_avatar_url: r.4,
            created_at: r.5,
        }).collect())
    }
    
    pub async fn block_user(&self, blocker: Uuid, blocked: Uuid, reason: Option<&str>) -> Result<(), FriendsError> {
        if blocker == blocked {
            return Err(FriendsError::SelfFriend);
        }
        
        sqlx::query(
            "DELETE FROM friendships WHERE (user_id = $1 AND friend_id = $2) OR (user_id = $2 AND friend_id = $1)"
        )
        .bind(blocker)
        .bind(blocked)
        .execute(&self.pool)
        .await?;
        
        let result = sqlx::query(
            "INSERT INTO blocks (blocker_id, blocked_id, reason) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING"
        )
        .bind(blocker)
        .bind(blocked)
        .bind(reason)
        .execute(&self.pool)
        .await?;
        
        if result.rows_affected() == 0 {
            return Err(FriendsError::AlreadyBlocked);
        }
        
        info!("User blocked: {} blocked {}", blocker, blocked);
        Ok(())
    }
    
    pub async fn unblock_user(&self, blocker: Uuid, blocked: Uuid) -> Result<(), FriendsError> {
        let result = sqlx::query(
            "DELETE FROM blocks WHERE blocker_id = $1 AND blocked_id = $2"
        )
        .bind(blocker)
        .bind(blocked)
        .execute(&self.pool)
        .await?;
        
        if result.rows_affected() == 0 {
            return Err(FriendsError::UserNotFound);
        }
        
        info!("User unblocked: {} unblocked {}", blocker, blocked);
        Ok(())
    }
    
    pub async fn get_blocked_users(&self, user_id: Uuid) -> Result<Vec<BlockedUser>, FriendsError> {
        let rows = sqlx::query_as::<_, (Uuid, String, DateTime<Utc>, Option<String>)>(
            r#"
            SELECT u.id, u.username, b.created_at, b.reason
            FROM blocks b
            JOIN users u ON u.id = b.blocked_id
            WHERE b.blocker_id = $1
            ORDER BY b.created_at DESC
            "#
        )
        .bind(user_id)
        .fetch_all(&self.pool)
        .await?;
        
        Ok(rows.into_iter().map(|r| BlockedUser {
            user_id: r.0,
            username: r.1,
            blocked_at: r.2,
            reason: r.3,
        }).collect())
    }
    
    pub async fn is_blocked(&self, user1: Uuid, user2: Uuid) -> Result<bool, FriendsError> {
        let count = sqlx::query_scalar::<_, i64>(
            "SELECT COUNT(*) FROM blocks WHERE (blocker_id = $1 AND blocked_id = $2) OR (blocker_id = $2 AND blocked_id = $1)"
        )
        .bind(user1)
        .bind(user2)
        .fetch_one(&self.pool)
        .await?;
        
        Ok(count > 0)
    }
    
    pub async fn are_friends(&self, user1: Uuid, user2: Uuid) -> Result<bool, FriendsError> {
        let count = sqlx::query_scalar::<_, i64>(
            "SELECT COUNT(*) FROM friendships WHERE user_id = $1 AND friend_id = $2 AND status = 'accepted'"
        )
        .bind(user1)
        .bind(user2)
        .fetch_one(&self.pool)
        .await?;
        
        Ok(count > 0)
    }
    
    pub async fn get_online_friends(&self, user_id: Uuid) -> Result<Vec<FriendInfo>, FriendsError> {
        let rows = sqlx::query_as::<_, (Uuid, String, String, Option<String>, String, Option<DateTime<Utc>>, DateTime<Utc>)>(
            r#"
            SELECT u.id, u.username, u.display_name, u.avatar_url, u.status, u.last_seen_at, f.created_at
            FROM friendships f
            JOIN users u ON u.id = f.friend_id
            WHERE f.user_id = $1 AND f.status = 'accepted' AND u.status = 'online'
            ORDER BY u.username
            "#
        )
        .bind(user_id)
        .fetch_all(&self.pool)
        .await?;
        
        Ok(rows.into_iter().map(|r| FriendInfo {
            user_id: r.0,
            username: r.1,
            display_name: r.2,
            avatar_url: r.3,
            status: r.4,
            last_seen_at: r.5,
            friendship_since: r.6,
        }).collect())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_friendship_status_display() {
        assert_eq!(FriendshipStatus::Pending.to_string(), "pending");
        assert_eq!(FriendshipStatus::Accepted.to_string(), "accepted");
    }
}
