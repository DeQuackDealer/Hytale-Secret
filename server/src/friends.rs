use sqlx::PgPool;
use uuid::Uuid;

#[allow(dead_code)]
pub struct FriendsService {
    db: PgPool,
}

#[allow(dead_code)]
impl FriendsService {
    pub fn new(db: PgPool) -> Self {
        Self { db }
    }
    
    pub async fn get_friends(&self, _user_id: Uuid) -> Result<Vec<Uuid>, sqlx::Error> {
        Ok(vec![])
    }
}
