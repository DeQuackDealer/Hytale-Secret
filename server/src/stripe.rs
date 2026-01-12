use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use uuid::Uuid;

#[derive(Debug, Serialize, Deserialize)]
pub struct StripeCredentials {
    pub publishable: String,
    pub secret: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ConnectionItem {
    pub settings: StripeCredentials,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ConnectionResponse {
    pub items: Vec<ConnectionItem>,
}

pub async fn get_stripe_credentials() -> Result<StripeCredentials, String> {
    let hostname = std::env::var("REPLIT_CONNECTORS_HOSTNAME")
        .map_err(|_| "REPLIT_CONNECTORS_HOSTNAME not set")?;
    
    let token = if let Ok(repl_identity) = std::env::var("REPL_IDENTITY") {
        format!("repl {}", repl_identity)
    } else if let Ok(web_renewal) = std::env::var("WEB_REPL_RENEWAL") {
        format!("depl {}", web_renewal)
    } else {
        return Err("No auth token available".to_string());
    };
    
    let is_production = std::env::var("REPLIT_DEPLOYMENT").map(|v| v == "1").unwrap_or(false);
    let environment = if is_production { "production" } else { "development" };
    
    let url = format!(
        "https://{}/api/v2/connection?include_secrets=true&connector_names=stripe&environment={}",
        hostname, environment
    );
    
    let client = reqwest::Client::new();
    let response = client
        .get(&url)
        .header("Accept", "application/json")
        .header("X_REPLIT_TOKEN", &token)
        .send()
        .await
        .map_err(|e| format!("Failed to fetch credentials: {}", e))?;
    
    let data: ConnectionResponse = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse credentials: {}", e))?;
    
    data.items
        .into_iter()
        .next()
        .map(|item| item.settings)
        .ok_or_else(|| "Stripe connection not found".to_string())
}

pub async fn create_checkout_session(
    customer_email: &str,
    price_id: &str,
    success_url: &str,
    cancel_url: &str,
) -> Result<String, String> {
    let creds = get_stripe_credentials().await?;
    
    let client = reqwest::Client::new();
    let params = [
        ("customer_email", customer_email),
        ("line_items[0][price]", price_id),
        ("line_items[0][quantity]", "1"),
        ("mode", "subscription"),
        ("success_url", success_url),
        ("cancel_url", cancel_url),
    ];
    
    let response = client
        .post("https://api.stripe.com/v1/checkout/sessions")
        .basic_auth(&creds.secret, Option::<&str>::None)
        .form(&params)
        .send()
        .await
        .map_err(|e| format!("Stripe API error: {}", e))?;
    
    let json: serde_json::Value = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse Stripe response: {}", e))?;
    
    json.get("url")
        .and_then(|v| v.as_str())
        .map(|s| s.to_string())
        .ok_or_else(|| "No checkout URL in response".to_string())
}

pub async fn create_customer_portal_session(
    customer_id: &str,
    return_url: &str,
) -> Result<String, String> {
    let creds = get_stripe_credentials().await?;
    
    let client = reqwest::Client::new();
    let params = [
        ("customer", customer_id),
        ("return_url", return_url),
    ];
    
    let response = client
        .post("https://api.stripe.com/v1/billing_portal/sessions")
        .basic_auth(&creds.secret, Option::<&str>::None)
        .form(&params)
        .send()
        .await
        .map_err(|e| format!("Stripe API error: {}", e))?;
    
    let json: serde_json::Value = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse Stripe response: {}", e))?;
    
    json.get("url")
        .and_then(|v| v.as_str())
        .map(|s| s.to_string())
        .ok_or_else(|| "No portal URL in response".to_string())
}

#[allow(dead_code)]
pub async fn get_or_create_stripe_customer(
    db: &PgPool,
    user_id: Uuid,
    email: &str,
) -> Result<String, String> {
    let existing = sqlx::query_scalar::<_, Option<String>>(
        "SELECT stripe_customer_id FROM subscriptions WHERE user_id = $1"
    )
        .bind(user_id)
        .fetch_optional(db)
        .await
        .map_err(|e| format!("Database error: {}", e))?
        .flatten();
    
    if let Some(customer_id) = existing {
        return Ok(customer_id);
    }
    
    let creds = get_stripe_credentials().await?;
    let client = reqwest::Client::new();
    
    let params = [
        ("email", email),
        ("metadata[user_id]", &user_id.to_string()),
    ];
    
    let response = client
        .post("https://api.stripe.com/v1/customers")
        .basic_auth(&creds.secret, Option::<&str>::None)
        .form(&params)
        .send()
        .await
        .map_err(|e| format!("Stripe API error: {}", e))?;
    
    let json: serde_json::Value = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse Stripe response: {}", e))?;
    
    let customer_id = json.get("id")
        .and_then(|v| v.as_str())
        .map(|s| s.to_string())
        .ok_or_else(|| "No customer ID in response".to_string())?;
    
    sqlx::query(
        "INSERT INTO subscriptions (user_id, tier, status, stripe_customer_id, created_at, updated_at)
         VALUES ($1, 'free', 'active', $2, NOW(), NOW())
         ON CONFLICT (user_id) DO UPDATE SET stripe_customer_id = $2, updated_at = NOW()"
    )
        .bind(user_id)
        .bind(&customer_id)
        .execute(db)
        .await
        .map_err(|e| format!("Database error: {}", e))?;
    
    Ok(customer_id)
}

pub async fn verify_payment_completed(session_id: &str) -> Result<bool, String> {
    let creds = get_stripe_credentials().await?;
    
    let client = reqwest::Client::new();
    let url = format!("https://api.stripe.com/v1/checkout/sessions/{}", session_id);
    
    let response = client
        .get(&url)
        .basic_auth(&creds.secret, Option::<&str>::None)
        .send()
        .await
        .map_err(|e| format!("Stripe API error: {}", e))?;
    
    let json: serde_json::Value = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse Stripe response: {}", e))?;
    
    if let Some(error) = json.get("error") {
        return Err(format!("Stripe error: {}", error.get("message").and_then(|m| m.as_str()).unwrap_or("Unknown")));
    }
    
    let payment_status = json.get("payment_status")
        .and_then(|v| v.as_str())
        .unwrap_or("unpaid");
    
    Ok(payment_status == "paid")
}

pub struct CheckoutResult {
    pub url: String,
    pub session_id: String,
}

pub async fn create_marketplace_checkout(
    customer_email: &str,
    amount: f64,
    item_name: &str,
    success_url: &str,
    cancel_url: &str,
) -> Result<CheckoutResult, String> {
    let creds = get_stripe_credentials().await?;
    
    let amount_cents = (amount * 100.0) as i64;
    
    let client = reqwest::Client::new();
    let params = [
        ("customer_email", customer_email.to_string()),
        ("line_items[0][price_data][currency]", "usd".to_string()),
        ("line_items[0][price_data][unit_amount]", amount_cents.to_string()),
        ("line_items[0][price_data][product_data][name]", item_name.to_string()),
        ("line_items[0][quantity]", "1".to_string()),
        ("mode", "payment".to_string()),
        ("success_url", success_url.to_string()),
        ("cancel_url", cancel_url.to_string()),
    ];
    
    let response = client
        .post("https://api.stripe.com/v1/checkout/sessions")
        .basic_auth(&creds.secret, Option::<&str>::None)
        .form(&params)
        .send()
        .await
        .map_err(|e| format!("Stripe API error: {}", e))?;
    
    let json: serde_json::Value = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse Stripe response: {}", e))?;
    
    if let Some(error) = json.get("error") {
        return Err(format!("Stripe error: {}", error.get("message").and_then(|m| m.as_str()).unwrap_or("Unknown")));
    }
    
    let url = json.get("url")
        .and_then(|v| v.as_str())
        .map(|s| s.to_string())
        .ok_or_else(|| "No checkout URL in response".to_string())?;
    
    let session_id = json.get("id")
        .and_then(|v| v.as_str())
        .map(|s| s.to_string())
        .ok_or_else(|| "No session ID in response".to_string())?;
    
    Ok(CheckoutResult { url, session_id })
}

#[allow(dead_code)]
pub async fn update_subscription_from_webhook(
    db: &PgPool,
    customer_id: &str,
    subscription_id: &str,
    status: &str,
    current_period_end: i64,
) -> Result<(), String> {
    let tier = if status == "active" || status == "trialing" {
        "premium"
    } else {
        "free"
    };
    
    let period_end = chrono::DateTime::from_timestamp(current_period_end, 0)
        .map(|dt| dt.with_timezone(&chrono::Utc));
    
    sqlx::query(
        "UPDATE subscriptions SET 
            tier = $1, 
            status = $2, 
            stripe_subscription_id = $3, 
            current_period_end = $4,
            updated_at = NOW()
         WHERE stripe_customer_id = $5"
    )
        .bind(tier)
        .bind(status)
        .bind(subscription_id)
        .bind(period_end)
        .bind(customer_id)
        .execute(db)
        .await
        .map_err(|e| format!("Database error: {}", e))?;
    
    Ok(())
}
