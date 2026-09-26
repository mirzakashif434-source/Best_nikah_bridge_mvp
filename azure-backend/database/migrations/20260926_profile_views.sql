-- Additive Step 3: real profile view tracking for Viewed Me / I Viewed.
CREATE TABLE IF NOT EXISTS profile_views (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  viewer_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  viewed_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  first_viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  view_count INTEGER NOT NULL DEFAULT 1 CHECK (view_count > 0),
  UNIQUE (viewer_user_id, viewed_user_id),
  CHECK (viewer_user_id <> viewed_user_id)
);
CREATE INDEX IF NOT EXISTS idx_profile_views_viewed_last
  ON profile_views(viewed_user_id,last_viewed_at DESC);
CREATE INDEX IF NOT EXISTS idx_profile_views_viewer_last
  ON profile_views(viewer_user_id,last_viewed_at DESC);
