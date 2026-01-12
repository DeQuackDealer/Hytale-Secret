#![allow(dead_code)]

use axum::{
    extract::State,
    response::Json,
};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use uuid::Uuid;

use crate::auth::hash_token;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "snake_case")]
pub enum EscrowStatus {
    Pending,
    Funded,
    Released,
    Disputed,
    Refunded,
    Cancelled,
}

impl From<String> for EscrowStatus {
    fn from(s: String) -> Self {
        match s.as_str() {
            "pending" => EscrowStatus::Pending,
            "funded" => EscrowStatus::Funded,
            "released" => EscrowStatus::Released,
            "disputed" => EscrowStatus::Disputed,
            "refunded" => EscrowStatus::Refunded,
            "cancelled" => EscrowStatus::Cancelled,
            _ => EscrowStatus::Pending,
        }
    }
}

impl ToString for EscrowStatus {
    fn to_string(&self) -> String {
        match self {
            EscrowStatus::Pending => "pending".to_string(),
            EscrowStatus::Funded => "funded".to_string(),
            EscrowStatus::Released => "released".to_string(),
            EscrowStatus::Disputed => "disputed".to_string(),
            EscrowStatus::Refunded => "refunded".to_string(),
            EscrowStatus::Cancelled => "cancelled".to_string(),
        }
    }
}

#[derive(Debug, Clone, Serialize)]
pub struct EscrowTransaction {
    pub id: Uuid,
    pub buyer_id: Uuid,
    pub seller_id: Uuid,
    pub item_id: Uuid,
    pub amount_cents: i32,
    pub platform_fee_cents: i32,
    pub status: EscrowStatus,
    pub stripe_payment_intent_id: Option<String>,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub funded_at: Option<chrono::DateTime<chrono::Utc>>,
    pub released_at: Option<chrono::DateTime<chrono::Utc>>,
}

#[derive(Debug, Deserialize)]
pub struct CreateEscrowRequest {
    pub token: String,
    pub item_id: Uuid,
}

#[derive(Debug, Deserialize)]
pub struct EscrowActionRequest {
    pub token: String,
    pub escrow_id: Uuid,
}

#[derive(Debug, Deserialize)]
pub struct DisputeRequest {
    pub token: String,
    pub escrow_id: Uuid,
    pub reason: String,
}

const PLATFORM_FEE_PERCENT: f64 = 0.10;

pub async fn create_escrow(
    State(db): State<PgPool>,
    Json(req): Json<CreateEscrowRequest>,
) -> Json<crate::ApiResponse<EscrowTransaction>> {
    let token_hash = hash_token(&req.token);
    
    let buyer = sqlx::query!(
        r#"
        SELECT u.id
        FROM users u
        JOIN user_sessions s ON u.id = s.user_id
        WHERE s.token_hash = $1 AND s.expires_at > NOW()
        "#,
        token_hash
    )
    .fetch_optional(&db)
    .await;
    
    let buyer_id = match buyer {
        Ok(Some(row)) => row.id,
        _ => return crate::ApiResponse::error("Invalid session"),
    };
    
    let item = sqlx::query!(
        r#"
        SELECT id, seller_id, price_cents, status
        FROM marketplace_items
        WHERE id = $1
        "#,
        req.item_id
    )
    .fetch_optional(&db)
    .await;
    
    let item = match item {
        Ok(Some(i)) if i.status == "approved" => i,
        Ok(Some(_)) => return crate::ApiResponse::error("Item not available for purchase"),
        _ => return crate::ApiResponse::error("Item not found"),
    };
    
    let seller_id = match item.seller_id {
        Some(id) => id,
        None => return crate::ApiResponse::error("Item has no seller"),
    };
    
    if seller_id == buyer_id {
        return crate::ApiResponse::error("Cannot purchase your own item");
    }
    
    let price_cents = item.price_cents.unwrap_or(0);
    let platform_fee = ((price_cents as f64) * PLATFORM_FEE_PERCENT) as i32;
    let escrow_id = Uuid::new_v4();
    
    let result = sqlx::query!(
        r#"
        INSERT INTO escrow_transactions 
        (id, buyer_id, seller_id, item_id, amount_cents, platform_fee_cents, status, created_at)
        VALUES ($1, $2, $3, $4, $5, $6, 'pending', NOW())
        RETURNING id, buyer_id, seller_id, item_id, amount_cents, platform_fee_cents, 
                  status, stripe_payment_intent_id, created_at, funded_at, released_at
        "#,
        escrow_id,
        buyer_id,
        seller_id,
        item.id,
        price_cents,
        platform_fee
    )
    .fetch_one(&db)
    .await;
    
    match result {
        Ok(row) => crate::ApiResponse::success(EscrowTransaction {
            id: row.id,
            buyer_id: row.buyer_id,
            seller_id: row.seller_id,
            item_id: row.item_id,
            amount_cents: row.amount_cents.unwrap_or(0),
            platform_fee_cents: row.platform_fee_cents.unwrap_or(0),
            status: row.status.into(),
            stripe_payment_intent_id: row.stripe_payment_intent_id,
            created_at: row.created_at,
            funded_at: row.funded_at,
            released_at: row.released_at,
        }),
        Err(e) => crate::ApiResponse::error(format!("Failed to create escrow: {}", e)),
    }
}

pub async fn fund_escrow(
    State(db): State<PgPool>,
    Json(req): Json<EscrowActionRequest>,
) -> Json<crate::ApiResponse<EscrowTransaction>> {
    let token_hash = hash_token(&req.token);
    
    let user = sqlx::query!(
        r#"
        SELECT u.id
        FROM users u
        JOIN user_sessions s ON u.id = s.user_id
        WHERE s.token_hash = $1 AND s.expires_at > NOW()
        "#,
        token_hash
    )
    .fetch_optional(&db)
    .await;
    
    let user_id = match user {
        Ok(Some(row)) => row.id,
        _ => return crate::ApiResponse::error("Invalid session"),
    };
    
    let result = sqlx::query!(
        r#"
        UPDATE escrow_transactions
        SET status = 'funded', funded_at = NOW()
        WHERE id = $1 AND buyer_id = $2 AND status = 'pending'
        RETURNING id, buyer_id, seller_id, item_id, amount_cents, platform_fee_cents,
                  status, stripe_payment_intent_id, created_at, funded_at, released_at
        "#,
        req.escrow_id,
        user_id
    )
    .fetch_optional(&db)
    .await;
    
    match result {
        Ok(Some(row)) => crate::ApiResponse::success(EscrowTransaction {
            id: row.id,
            buyer_id: row.buyer_id,
            seller_id: row.seller_id,
            item_id: row.item_id,
            amount_cents: row.amount_cents.unwrap_or(0),
            platform_fee_cents: row.platform_fee_cents.unwrap_or(0),
            status: row.status.into(),
            stripe_payment_intent_id: row.stripe_payment_intent_id,
            created_at: row.created_at,
            funded_at: row.funded_at,
            released_at: row.released_at,
        }),
        Ok(None) => crate::ApiResponse::error("Escrow not found or not in pending state"),
        Err(e) => crate::ApiResponse::error(format!("Failed to fund escrow: {}", e)),
    }
}

pub async fn release_escrow(
    State(db): State<PgPool>,
    Json(req): Json<EscrowActionRequest>,
) -> Json<crate::ApiResponse<EscrowTransaction>> {
    let token_hash = hash_token(&req.token);
    
    let user = sqlx::query!(
        r#"
        SELECT u.id
        FROM users u
        JOIN user_sessions s ON u.id = s.user_id
        WHERE s.token_hash = $1 AND s.expires_at > NOW()
        "#,
        token_hash
    )
    .fetch_optional(&db)
    .await;
    
    let user_id = match user {
        Ok(Some(row)) => row.id,
        _ => return crate::ApiResponse::error("Invalid session"),
    };
    
    let result = sqlx::query!(
        r#"
        UPDATE escrow_transactions
        SET status = 'released', released_at = NOW()
        WHERE id = $1 AND buyer_id = $2 AND status = 'funded'
        RETURNING id, buyer_id, seller_id, item_id, amount_cents, platform_fee_cents,
                  status, stripe_payment_intent_id, created_at, funded_at, released_at
        "#,
        req.escrow_id,
        user_id
    )
    .fetch_optional(&db)
    .await;
    
    match result {
        Ok(Some(row)) => {
            let amount = row.amount_cents.unwrap_or(0);
            let fee = row.platform_fee_cents.unwrap_or(0);
            let seller_payout = amount - fee;
            sqlx::query!(
                r#"
                INSERT INTO seller_payouts (escrow_id, seller_id, amount_cents, status, created_at)
                VALUES ($1, $2, $3, 'pending', NOW())
                "#,
                row.id,
                row.seller_id,
                seller_payout
            )
            .execute(&db)
            .await
            .ok();
            
            crate::ApiResponse::success(EscrowTransaction {
                id: row.id,
                buyer_id: row.buyer_id,
                seller_id: row.seller_id,
                item_id: row.item_id,
                amount_cents: amount,
                platform_fee_cents: fee,
                status: row.status.into(),
                stripe_payment_intent_id: row.stripe_payment_intent_id,
                created_at: row.created_at,
                funded_at: row.funded_at,
                released_at: row.released_at,
            })
        }
        Ok(None) => crate::ApiResponse::error("Escrow not found or not in funded state"),
        Err(e) => crate::ApiResponse::error(format!("Failed to release escrow: {}", e)),
    }
}

pub async fn dispute_escrow(
    State(db): State<PgPool>,
    Json(req): Json<DisputeRequest>,
) -> Json<crate::ApiResponse<()>> {
    let token_hash = hash_token(&req.token);
    
    let user = sqlx::query!(
        r#"
        SELECT u.id
        FROM users u
        JOIN user_sessions s ON u.id = s.user_id
        WHERE s.token_hash = $1 AND s.expires_at > NOW()
        "#,
        token_hash
    )
    .fetch_optional(&db)
    .await;
    
    let user_id = match user {
        Ok(Some(row)) => row.id,
        _ => return crate::ApiResponse::error("Invalid session"),
    };
    
    let escrow = sqlx::query!(
        "SELECT buyer_id, seller_id, status FROM escrow_transactions WHERE id = $1",
        req.escrow_id
    )
    .fetch_optional(&db)
    .await;
    
    let escrow = match escrow {
        Ok(Some(e)) if e.buyer_id == user_id || e.seller_id == user_id => e,
        _ => return crate::ApiResponse::error("Escrow not found or unauthorized"),
    };
    
    if escrow.status != "funded" {
        return crate::ApiResponse::error("Can only dispute funded escrows");
    }
    
    let result = sqlx::query!(
        "UPDATE escrow_transactions SET status = 'disputed' WHERE id = $1",
        req.escrow_id
    )
    .execute(&db)
    .await;
    
    if result.is_err() {
        return crate::ApiResponse::error("Failed to dispute escrow");
    }
    
    sqlx::query!(
        r#"
        INSERT INTO escrow_disputes (escrow_id, initiator_id, reason, created_at)
        VALUES ($1, $2, $3, NOW())
        "#,
        req.escrow_id,
        user_id,
        req.reason
    )
    .execute(&db)
    .await
    .ok();
    
    crate::ApiResponse::success(())
}

pub async fn get_user_escrows(
    State(db): State<PgPool>,
    Json(req): Json<crate::TokenRequest>,
) -> Json<crate::ApiResponse<Vec<EscrowTransaction>>> {
    let token_hash = hash_token(&req.token);
    
    let user = sqlx::query!(
        r#"
        SELECT u.id
        FROM users u
        JOIN user_sessions s ON u.id = s.user_id
        WHERE s.token_hash = $1 AND s.expires_at > NOW()
        "#,
        token_hash
    )
    .fetch_optional(&db)
    .await;
    
    let user_id = match user {
        Ok(Some(row)) => row.id,
        _ => return crate::ApiResponse::error("Invalid session"),
    };
    
    let escrows = sqlx::query!(
        r#"
        SELECT id, buyer_id, seller_id, item_id, amount_cents, platform_fee_cents,
               status, stripe_payment_intent_id, created_at, funded_at, released_at
        FROM escrow_transactions
        WHERE buyer_id = $1 OR seller_id = $1
        ORDER BY created_at DESC
        "#,
        user_id
    )
    .fetch_all(&db)
    .await;
    
    match escrows {
        Ok(rows) => {
            let transactions: Vec<EscrowTransaction> = rows
                .into_iter()
                .map(|row| EscrowTransaction {
                    id: row.id,
                    buyer_id: row.buyer_id,
                    seller_id: row.seller_id,
                    item_id: row.item_id,
                    amount_cents: row.amount_cents.unwrap_or(0),
                    platform_fee_cents: row.platform_fee_cents.unwrap_or(0),
                    status: row.status.into(),
                    stripe_payment_intent_id: row.stripe_payment_intent_id,
                    created_at: row.created_at,
                    funded_at: row.funded_at,
                    released_at: row.released_at,
                })
                .collect();
            crate::ApiResponse::success(transactions)
        }
        Err(e) => crate::ApiResponse::error(format!("Failed to get escrows: {}", e)),
    }
}
