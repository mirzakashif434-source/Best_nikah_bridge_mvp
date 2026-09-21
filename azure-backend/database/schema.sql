-- Step 1: production PostgreSQL schema for the Azure backend.
-- Additive only. Existing Firebase/Android data is not modified.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  firebase_uid TEXT UNIQUE,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT,
  phone_e164 TEXT UNIQUE,
  role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user','admin','moderator')),
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active','suspended','deleted')),
  email_verified_at TIMESTAMPTZ,
  phone_verified_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS profiles (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  display_name TEXT NOT NULL,
  date_of_birth DATE NOT NULL,
  gender TEXT NOT NULL CHECK (gender IN ('male','female')),
  country TEXT,
  city TEXT,
  bio TEXT,
  marriage_intention TEXT,
  readiness_score SMALLINT CHECK (readiness_score BETWEEN 0 AND 100),
  profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
  is_visible BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS partner_preferences (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  min_age SMALLINT,
  max_age SMALLINT,
  preferred_gender TEXT CHECK (preferred_gender IN ('male','female','any')),
  countries TEXT[] NOT NULL DEFAULT '{}',
  cities TEXT[] NOT NULL DEFAULT '{}',
  preferred_marriage_timeline TEXT,
  deal_breakers JSONB NOT NULL DEFAULT '{}'::jsonb,
  preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS interests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  receiver_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','accepted','declined','cancelled')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  responded_at TIMESTAMPTZ,
  UNIQUE (sender_user_id, receiver_user_id)
);

CREATE TABLE IF NOT EXISTS conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_a_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  user_b_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'mutual' CHECK (status IN ('mutual','blocked','closed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (user_a_id <> user_b_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_conversations_canonical_pair
  ON conversations (LEAST(user_a_id, user_b_id), GREATEST(user_a_id, user_b_id));

CREATE TABLE IF NOT EXISTS messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  sender_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  read_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS family_links (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  wali_email TEXT,
  wali_phone_e164 TEXT,
  wali_name TEXT,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','verified','revoked')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  verified_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS verifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  verification_type TEXT NOT NULL CHECK (verification_type IN ('email','phone','identity','selfie','liveness','manual')),
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','rejected','expired')),
  provider TEXT,
  provider_reference TEXT,
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS photos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  blob_key TEXT NOT NULL UNIQUE,
  visibility TEXT NOT NULL DEFAULT 'private' CHECK (visibility IN ('private','matches','public')),
  moderation_status TEXT NOT NULL DEFAULT 'pending' CHECK (moderation_status IN ('pending','approved','rejected')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS safety_reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reported_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  reason TEXT NOT NULL,
  details TEXT,
  status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open','reviewing','resolved','dismissed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at TIMESTAMPTZ
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS firebase_uid TEXT UNIQUE;
ALTER TABLE partner_preferences ADD COLUMN IF NOT EXISTS preferred_gender TEXT;
ALTER TABLE partner_preferences DROP CONSTRAINT IF EXISTS partner_preferences_preferred_gender_check;
ALTER TABLE partner_preferences ADD CONSTRAINT partner_preferences_preferred_gender_check CHECK (preferred_gender IN ('male','female','any'));

CREATE INDEX IF NOT EXISTS idx_profiles_visible_gender ON profiles (is_visible, gender);
CREATE INDEX IF NOT EXISTS idx_interests_receiver_status ON interests (receiver_user_id, status);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_created ON messages (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_verifications_user_status ON verifications (user_id, status);
CREATE INDEX IF NOT EXISTS idx_safety_reports_status ON safety_reports (status);

ALTER TABLE verifications ADD COLUMN IF NOT EXISTS document_blob_key TEXT;
ALTER TABLE verifications ADD COLUMN IF NOT EXISTS document_content_type TEXT;
ALTER TABLE verifications ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_verifications_pending_type ON verifications (status, verification_type);
CREATE TABLE IF NOT EXISTS blocked_users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  blocker_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  blocked_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (blocker_user_id, blocked_user_id),
  CHECK (blocker_user_id <> blocked_user_id)
);

CREATE INDEX IF NOT EXISTS idx_blocked_users_blocker ON blocked_users (blocker_user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_blocked_users_blocked ON blocked_users (blocked_user_id, created_at);
CREATE TABLE IF NOT EXISTS privacy_settings (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  profile_discoverable BOOLEAN NOT NULL DEFAULT TRUE,
  show_city BOOLEAN NOT NULL DEFAULT TRUE,
  show_photo_to_matches BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE photos ADD COLUMN IF NOT EXISTS moderation_provider TEXT;
ALTER TABLE photos ADD COLUMN IF NOT EXISTS moderation_result JSONB;
ALTER TABLE photos ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_photos_moderation_status ON photos (moderation_status, created_at);


-- Step 18 / Firebase migration #1: additive Azure External ID identity mapping.
-- Existing Firebase identities remain intact until a tested production cutover.
ALTER TABLE users ADD COLUMN IF NOT EXISTS azure_subject TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_azure_subject ON users (azure_subject) WHERE azure_subject IS NOT NULL;


-- Firebase migration #1A: production Rewarded Ad / message-credit backend.
CREATE TABLE IF NOT EXISTS entitlements (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  message_credits INTEGER NOT NULL DEFAULT 0 CHECK (message_credits >= 0),
  paid_tier TEXT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS rewarded_ad_transactions (
  transaction_id TEXT PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  ad_unit TEXT NOT NULL,
  reward_amount NUMERIC NOT NULL DEFAULT 0,
  reward_item TEXT,
  verified_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS daily_reward_claims (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  claim_date DATE NOT NULL,
  claim_count INTEGER NOT NULL DEFAULT 0 CHECK (claim_count BETWEEN 0 AND 2),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, claim_date)
);

CREATE INDEX IF NOT EXISTS idx_rewarded_ad_transactions_user
  ON rewarded_ad_transactions (user_id, verified_at DESC);


-- Step 2: production Premium plan catalog.
-- Product IDs/base-plan IDs are configured from GitHub/Azure secrets and must match Play Console.
CREATE TABLE IF NOT EXISTS premium_plans (
  plan_key TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  billing_period TEXT NOT NULL CHECK (billing_period IN ('monthly','yearly')),
  features JSONB NOT NULL DEFAULT '{}'::jsonb,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order SMALLINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO premium_plans (plan_key, display_name, billing_period, features, active, sort_order)
VALUES
  ('premium_monthly', 'Premium Monthly', 'monthly',
   '{"advanced_matching":true,"why_we_matched":true,"enhanced_privacy":true,"priority_support":true}'::jsonb,
   TRUE, 10),
  ('premium_yearly', 'Premium Yearly', 'yearly',
   '{"advanced_matching":true,"why_we_matched":true,"enhanced_privacy":true,"priority_support":true}'::jsonb,
   TRUE, 20)
ON CONFLICT (plan_key) DO UPDATE SET
  display_name = EXCLUDED.display_name,
  billing_period = EXCLUDED.billing_period,
  features = EXCLUDED.features,
  active = EXCLUDED.active,
  sort_order = EXCLUDED.sort_order,
  updated_at = now();

CREATE INDEX IF NOT EXISTS idx_premium_plans_active_sort
  ON premium_plans (active, sort_order);


-- Step 2: real Google Play subscription verification and paid entitlement ledger.
CREATE TABLE IF NOT EXISTS premium_purchase_tokens (
  purchase_token_hash TEXT PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  product_id TEXT NOT NULL,
  base_plan_id TEXT NOT NULL,
  order_id TEXT,
  subscription_state TEXT NOT NULL,
  expiry_time TIMESTAMPTZ NOT NULL,
  last_verified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_premium_purchase_tokens_user
  ON premium_purchase_tokens (user_id, expiry_time DESC);

CREATE TABLE IF NOT EXISTS premium_entitlements (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  plan_key TEXT NOT NULL,
  product_id TEXT NOT NULL,
  base_plan_id TEXT NOT NULL,
  purchase_token_hash TEXT NOT NULL REFERENCES premium_purchase_tokens(purchase_token_hash),
  order_id TEXT,
  status TEXT NOT NULL CHECK (status IN ('active','expired','revoked')),
  expires_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_premium_entitlements_status_expiry
  ON premium_entitlements (status, expires_at);


-- Real Play Console 20/40/60 monthly tiers.
INSERT INTO premium_plans (plan_key, display_name, billing_period, features, active, sort_order)
VALUES
  ('premium_basic_20', 'Premium 20', 'monthly',
   '{"advanced_matching":true,"why_we_matched":true}'::jsonb, TRUE, 10),
  ('premium_plus_40', 'Premium 40', 'monthly',
   '{"advanced_matching":true,"why_we_matched":true,"enhanced_privacy":true}'::jsonb, TRUE, 20),
  ('premium_vip_60', 'Premium 60', 'monthly',
   '{"advanced_matching":true,"why_we_matched":true,"enhanced_privacy":true,"priority_support":true}'::jsonb, TRUE, 30)
ON CONFLICT (plan_key) DO UPDATE SET
  display_name = EXCLUDED.display_name,
  billing_period = EXCLUDED.billing_period,
  features = EXCLUDED.features,
  active = EXCLUDED.active,
  sort_order = EXCLUDED.sort_order,
  updated_at = now();


-- Firebase migration #3: additive Azure wallet ledger.
-- No existing tables or data are removed or replaced.
CREATE TABLE IF NOT EXISTS wallet_accounts (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  balance NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
  currency TEXT NOT NULL DEFAULT 'SAR' CHECK (currency IN ('SAR','USDT')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS wallet_ledger (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  entry_type TEXT NOT NULL CHECK (entry_type IN ('earning','withdrawal_hold','withdrawal_release','payout')),
  amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
  currency TEXT NOT NULL CHECK (currency IN ('SAR','USDT')),
  reference_id TEXT,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS wallet_withdrawals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  amount NUMERIC(18,2) NOT NULL CHECK (amount >= 10),
  currency TEXT NOT NULL CHECK (currency IN ('SAR','USDT')),
  country TEXT NOT NULL,
  destination TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','paid','rejected','cancelled')),
  reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
  reviewed_at TIMESTAMPTZ,
  paid_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_wallet_ledger_user_created
  ON wallet_ledger (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_withdrawals_user_created
  ON wallet_withdrawals (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_withdrawals_status
  ON wallet_withdrawals (status, created_at);


-- Wallet verification checkpoint: additive only; no existing objects are removed or replaced.


-- Firebase migration: real Azure terms acceptance state.
ALTER TABLE users ADD COLUMN IF NOT EXISTS terms_accepted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS terms_version TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;
