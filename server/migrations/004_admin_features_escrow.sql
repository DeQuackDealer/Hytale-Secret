-- Admin, Feature Flags, and Escrow System Tables

-- Add is_admin column to users if not exists
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_admin BOOLEAN DEFAULT FALSE;

-- Feature flags table
CREATE TABLE IF NOT EXISTS feature_flags (
    name VARCHAR(100) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    requires_premium BOOLEAN NOT NULL DEFAULT FALSE,
    category VARCHAR(50) NOT NULL DEFAULT 'general',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- User bans table
CREATE TABLE IF NOT EXISTS user_bans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    lifted_at TIMESTAMP WITH TIME ZONE,
    lifted_by UUID REFERENCES users(id)
);

-- Escrow transactions table
CREATE TABLE IF NOT EXISTS escrow_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id UUID NOT NULL REFERENCES users(id),
    seller_id UUID NOT NULL REFERENCES users(id),
    item_id UUID NOT NULL,
    amount_cents INTEGER NOT NULL,
    platform_fee_cents INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    stripe_payment_intent_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    funded_at TIMESTAMP WITH TIME ZONE,
    released_at TIMESTAMP WITH TIME ZONE
);

-- Escrow disputes table
CREATE TABLE IF NOT EXISTS escrow_disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    escrow_id UUID NOT NULL REFERENCES escrow_transactions(id),
    initiator_id UUID NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    resolution TEXT,
    resolved_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- Seller payouts table
CREATE TABLE IF NOT EXISTS seller_payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    escrow_id UUID NOT NULL REFERENCES escrow_transactions(id),
    seller_id UUID NOT NULL REFERENCES users(id),
    amount_cents INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    stripe_transfer_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Payments tracking table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    amount_cents INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_feature_flags_category ON feature_flags(category);
CREATE INDEX IF NOT EXISTS idx_user_bans_user_id ON user_bans(user_id);
CREATE INDEX IF NOT EXISTS idx_user_bans_expires ON user_bans(expires_at);
CREATE INDEX IF NOT EXISTS idx_escrow_buyer ON escrow_transactions(buyer_id);
CREATE INDEX IF NOT EXISTS idx_escrow_seller ON escrow_transactions(seller_id);
CREATE INDEX IF NOT EXISTS idx_escrow_status ON escrow_transactions(status);
CREATE INDEX IF NOT EXISTS idx_seller_payouts_seller ON seller_payouts(seller_id);
CREATE INDEX IF NOT EXISTS idx_payments_user ON payments(user_id);

-- Insert default feature flags
INSERT INTO feature_flags (name, enabled, description, requires_premium, category) VALUES
    ('replay_system', true, 'Record and replay game sessions', false, 'gameplay'),
    ('minimap', true, 'In-game minimap overlay', false, 'interface'),
    ('worldmap', true, 'Full world map view', false, 'interface'),
    ('waypoints', true, 'Custom waypoint markers', false, 'navigation'),
    ('party_system', true, 'Create and manage parties', false, 'social'),
    ('friend_activity', true, 'See friends online status', false, 'social'),
    ('cinema_camera', true, 'Cinematic camera controls', false, 'creative'),
    ('mod_marketplace', true, 'Access mod marketplace', false, 'mods'),
    ('cosmetics', true, 'Custom cosmetic items', false, 'premium'),
    ('premium_capes', false, 'Exclusive premium capes', true, 'premium'),
    ('server_queue_priority', false, 'Priority server queue', true, 'premium'),
    ('anticheat_bypass_report', true, 'Report false positives', false, 'anticheat'),
    ('voice_chat', false, 'In-game voice chat', false, 'social'),
    ('stream_mode', true, 'Hide sensitive info while streaming', false, 'interface')
ON CONFLICT (name) DO NOTHING;
