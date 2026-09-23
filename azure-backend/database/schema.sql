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

-- Step 2: real Azure Global Community Chat migration (additive; Firebase retained for rollback).
CREATE TABLE IF NOT EXISTS community_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  author_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  author_name TEXT NOT NULL,
  country TEXT,
  body TEXT NOT NULL CHECK (char_length(body) BETWEEN 1 AND 500),
  moderation_status TEXT NOT NULL DEFAULT 'visible' CHECK (moderation_status IN ('visible','removed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_community_messages_created ON community_messages(created_at DESC);

CREATE TABLE IF NOT EXISTS community_mutes (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  muted_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, muted_user_id),
  CHECK (user_id <> muted_user_id)
);

CREATE TABLE IF NOT EXISTS community_reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reported_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  message_id UUID NOT NULL REFERENCES community_messages(id) ON DELETE CASCADE,
  reason TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open','reviewing','resolved','dismissed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_community_reports_status ON community_reports(status,created_at);
CREATE TABLE IF NOT EXISTS community_rate_limits (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  window_started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  message_count INTEGER NOT NULL DEFAULT 0 CHECK (message_count >= 0)
);


-- Step 7 / Firebase migration #1: Azure Help Line tickets.
CREATE TABLE IF NOT EXISTS help_line_tickets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  question TEXT NOT NULL,
  ai_answer TEXT,
  ai_answered BOOLEAN NOT NULL DEFAULT FALSE,
  human_required BOOLEAN NOT NULL DEFAULT FALSE,
  status TEXT NOT NULL DEFAULT 'awaiting_human' CHECK (status IN ('ai_answered','awaiting_human','human_replied','closed')),
  human_reply TEXT,
  human_replied_at TIMESTAMPTZ,
  replied_by UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  human_reply_target_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_help_line_tickets_user_created ON help_line_tickets(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_help_line_tickets_status_created ON help_line_tickets(status, created_at DESC);


-- Step 2: real Azure owner earnings / settlement ledger.
CREATE TABLE IF NOT EXISTS owner_earnings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  purchase_token_hash TEXT UNIQUE,
  purchase_id TEXT,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  product_id TEXT NOT NULL,
  order_id TEXT,
  status TEXT NOT NULL DEFAULT 'verified_sale' CHECK (status IN ('verified_sale','refunded','revoked')),
  plan_value_sar_minor INTEGER NOT NULL CHECK (plan_value_sar_minor > 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  verified_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_owner_earnings_created ON owner_earnings(created_at DESC);

CREATE TABLE IF NOT EXISTS owner_earnings_summary (
  id BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (id),
  verified_sales_count INTEGER NOT NULL DEFAULT 0,
  verified_plan_value_sar_minor BIGINT NOT NULL DEFAULT 0,
  available_sar_minor BIGINT NOT NULL DEFAULT 0,
  available_pkr_minor BIGINT NOT NULL DEFAULT 0,
  available_usdt_minor BIGINT NOT NULL DEFAULT 0,
  pending_sar_minor BIGINT NOT NULL DEFAULT 0,
  pending_pkr_minor BIGINT NOT NULL DEFAULT 0,
  pending_usdt_minor BIGINT NOT NULL DEFAULT 0,
  settled_sar_minor BIGINT NOT NULL DEFAULT 0,
  settled_pkr_minor BIGINT NOT NULL DEFAULT 0,
  settled_usdt_minor BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO owner_earnings_summary(id) VALUES(TRUE) ON CONFLICT(id) DO NOTHING;

CREATE TABLE IF NOT EXISTS owner_settlement_profile (
  id BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (id),
  country TEXT NOT NULL DEFAULT '',
  currency TEXT NOT NULL DEFAULT '',
  destination TEXT NOT NULL DEFAULT '',
  label TEXT NOT NULL DEFAULT '',
  updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS owner_provider_settlements (
  provider_reference TEXT PRIMARY KEY,
  currency TEXT NOT NULL CHECK (currency IN ('SAR','PKR','USDT')),
  amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
  provider TEXT NOT NULL,
  recorded_by UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS owner_settlements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  currency TEXT NOT NULL CHECK (currency IN ('SAR','PKR','USDT')),
  amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
  country TEXT NOT NULL,
  destination TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending_provider' CHECK (status IN ('pending_provider','paid','cancelled')),
  requested_by UUID REFERENCES users(id) ON DELETE SET NULL,
  provider_reference TEXT,
  paid_by UUID REFERENCES users(id) ON DELETE SET NULL,
  paid_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_owner_settlements_status_created ON owner_settlements(status,created_at DESC);

-- Owner Wallet: Saudi Google Play merchant payouts arrive by USD wire transfer.
-- Additive only: existing SAR/PKR/USDT data remains valid and untouched.
ALTER TABLE owner_earnings_summary ADD COLUMN IF NOT EXISTS available_usd_minor BIGINT NOT NULL DEFAULT 0;
ALTER TABLE owner_earnings_summary ADD COLUMN IF NOT EXISTS pending_usd_minor BIGINT NOT NULL DEFAULT 0;
ALTER TABLE owner_earnings_summary ADD COLUMN IF NOT EXISTS settled_usd_minor BIGINT NOT NULL DEFAULT 0;

ALTER TABLE owner_provider_settlements DROP CONSTRAINT IF EXISTS owner_provider_settlements_currency_check;
ALTER TABLE owner_provider_settlements
  ADD CONSTRAINT owner_provider_settlements_currency_check
  CHECK (currency IN ('SAR','USD','PKR','USDT'));

ALTER TABLE owner_settlements DROP CONSTRAINT IF EXISTS owner_settlements_currency_check;
ALTER TABLE owner_settlements
  ADD CONSTRAINT owner_settlements_currency_check
  CHECK (currency IN ('SAR','USD','PKR','USDT'));


-- Step 4: Firestore -> Azure PostgreSQL parity layer (additive only).
-- These tables mirror remaining production Firestore collections before cutover.
CREATE TABLE IF NOT EXISTS user_settings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  setting_key TEXT NOT NULL,
  setting_value JSONB NOT NULL DEFAULT '{}'::jsonb,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, setting_key)
);

CREATE TABLE IF NOT EXISTS wali_connections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  wali_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  wali_name TEXT,
  wali_email TEXT,
  wali_phone_e164 TEXT,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','active','revoked')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_wali_connections_user_status ON wali_connections(user_id,status);

CREATE TABLE IF NOT EXISTS family_bridges (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active','closed','revoked')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS family_bridge_participants (
  bridge_id UUID NOT NULL REFERENCES family_bridges(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role TEXT NOT NULL CHECK (role IN ('member','wali','chaperone')),
  joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (bridge_id,user_id)
);

CREATE TABLE IF NOT EXISTS family_bridge_questions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  bridge_id UUID NOT NULL REFERENCES family_bridges(id) ON DELETE CASCADE,
  author_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  question TEXT NOT NULL,
  answer TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  answered_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_family_bridge_questions_bridge_created ON family_bridge_questions(bridge_id,created_at);

CREATE TABLE IF NOT EXISTS deletion_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','approved','completed','rejected')),
  reason TEXT,
  requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_deletion_requests_user_status ON deletion_requests(user_id,status);

CREATE TABLE IF NOT EXISTS moderation_queue (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_type TEXT NOT NULL,
  entity_id UUID,
  reported_by UUID REFERENCES users(id) ON DELETE SET NULL,
  reason TEXT,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','reviewing','resolved','dismissed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_moderation_queue_status_created ON moderation_queue(status,created_at);

CREATE TABLE IF NOT EXISTS risk_signals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  signal_type TEXT NOT NULL,
  score NUMERIC(8,3),
  details JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_risk_signals_user_created ON risk_signals(user_id,created_at DESC);

CREATE TABLE IF NOT EXISTS play_purchases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  product_id TEXT NOT NULL,
  purchase_token_hash TEXT NOT NULL UNIQUE,
  order_id TEXT,
  state TEXT NOT NULL,
  purchased_at TIMESTAMPTZ,
  verified_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public_config (
  config_key TEXT PRIMARY KEY,
  config_value JSONB NOT NULL DEFAULT '{}'::jsonb,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS admin_records (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  record_key TEXT NOT NULL UNIQUE,
  record_value JSONB NOT NULL DEFAULT '{}'::jsonb,
  updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- Step 8: Azure PostgreSQL data-migration integrity ledger.
-- This is additive only. It records controlled Firestore -> Azure migration state
-- without deleting or overwriting the original Firebase data.
CREATE TABLE IF NOT EXISTS firestore_migration_audit (
  collection_name TEXT PRIMARY KEY,
  target_table TEXT NOT NULL,
  migration_status TEXT NOT NULL DEFAULT 'pending'
    CHECK (migration_status IN ('pending','validated','migrated','verified','blocked')),
  source_record_count BIGINT,
  target_record_count BIGINT,
  source_last_exported_at TIMESTAMPTZ,
  target_last_verified_at TIMESTAMPTZ,
  last_error TEXT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO firestore_migration_audit(collection_name,target_table)
VALUES
  ('users','users'),
  ('profiles','profiles'),
  ('partner_preferences','partner_preferences'),
  ('interests','interests'),
  ('conversations','conversations'),
  ('messages','messages'),
  ('family_links','family_links'),
  ('verifications','verifications'),
  ('photos','photos'),
  ('safety_reports','safety_reports'),
  ('blocked_users','blocked_users'),
  ('privacy_settings','privacy_settings'),
  ('community_messages','community_messages'),
  ('community_reports','community_reports'),
  ('help_line_tickets','help_line_tickets'),
  ('owner_earnings','owner_earnings')
ON CONFLICT (collection_name) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_firestore_migration_audit_status
  ON firestore_migration_audit(migration_status,updated_at DESC);

-- Azure identity/data consistency constraints.
CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX IF NOT EXISTS idx_users_azure_subject ON users(azure_subject);


-- Step 9: Firebase Function parity tables for remaining application flows.
CREATE TABLE IF NOT EXISTS likes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  from_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  to_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  day DATE NOT NULL,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (from_user_id,to_user_id)
);
CREATE INDEX IF NOT EXISTS idx_likes_from_day ON likes(from_user_id,day);

CREATE TABLE IF NOT EXISTS daily_likes (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  day DATE NOT NULL,
  count INTEGER NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id,day)
);

CREATE TABLE IF NOT EXISTS nikah_promise_paths (
  connection_id UUID PRIMARY KEY,
  stage TEXT NOT NULL,
  target_date TEXT,
  notes TEXT,
  updated_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS living_compatibility (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  country TEXT, city TEXT, marriage_timeline TEXT, family_involvement TEXT,
  children_expectation TEXT, career_plan TEXT, living_plan TEXT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS living_change_alerts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  from_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  to_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  field TEXT NOT NULL, value TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'unread',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS connection_health_checks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  connection_id UUID NOT NULL,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  communication TEXT, family_progress TEXT, unresolved_differences TEXT,
  timeline_aligned TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(connection_id,user_id)
);

CREATE TABLE IF NOT EXISTS free_boost_claims (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  claimed_at TIMESTAMPTZ NOT NULL
);


-- Family Nikah Circle: additive viral family-network feature.
CREATE TABLE IF NOT EXISTS family_circles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL DEFAULT 'My Nikah Circle',
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active','closed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_family_circles_owner_active
  ON family_circles(owner_user_id) WHERE status='active';

CREATE TABLE IF NOT EXISTS family_circle_invites (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  circle_id UUID NOT NULL REFERENCES family_circles(id) ON DELETE CASCADE,
  invited_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL UNIQUE,
  role TEXT NOT NULL DEFAULT 'family' CHECK (role IN ('wali','parent','sibling','family','trusted')),
  max_uses SMALLINT NOT NULL DEFAULT 5 CHECK (max_uses BETWEEN 1 AND 20),
  use_count SMALLINT NOT NULL DEFAULT 0 CHECK (use_count >= 0),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_family_circle_invites_circle ON family_circle_invites(circle_id,created_at DESC);

CREATE TABLE IF NOT EXISTS family_circle_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  circle_id UUID NOT NULL REFERENCES family_circles(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role TEXT NOT NULL CHECK (role IN ('owner','wali','parent','sibling','family','trusted')),
  can_suggest_matches BOOLEAN NOT NULL DEFAULT TRUE,
  can_view_progress BOOLEAN NOT NULL DEFAULT TRUE,
  joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  removed_at TIMESTAMPTZ,
  UNIQUE(circle_id,user_id)
);
CREATE INDEX IF NOT EXISTS idx_family_circle_members_user ON family_circle_members(user_id,joined_at DESC);

CREATE TABLE IF NOT EXISTS family_match_suggestions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  circle_id UUID NOT NULL REFERENCES family_circles(id) ON DELETE CASCADE,
  suggested_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  suggested_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  note TEXT,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','viewed','accepted','declined')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  responded_at TIMESTAMPTZ,
  CHECK (suggested_by_user_id <> suggested_user_id)
);
CREATE INDEX IF NOT EXISTS idx_family_match_suggestions_circle_status
  ON family_match_suggestions(circle_id,status,created_at DESC);
