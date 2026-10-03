-- Final premium tier display-name alignment with Google Play Console.
-- Additive only: preserves plan keys, product IDs, entitlements, purchases and features.

UPDATE premium_plans
SET display_name = CASE plan_key
  WHEN 'premium_basic_20' THEN '20 SAR Basic'
  WHEN 'premium_plus_40' THEN '40 SAR Premium'
  WHEN 'premium_vip_60' THEN '60 SAR VIP'
  ELSE display_name
END,
updated_at = now()
WHERE plan_key IN ('premium_basic_20','premium_plus_40','premium_vip_60');

DO $$
DECLARE
  mismatches integer;
BEGIN
  SELECT count(*) INTO mismatches
  FROM premium_plans
  WHERE (plan_key='premium_basic_20' AND display_name<>'20 SAR Basic')
     OR (plan_key='premium_plus_40' AND display_name<>'40 SAR Premium')
     OR (plan_key='premium_vip_60' AND display_name<>'60 SAR VIP');

  IF mismatches <> 0 THEN
    RAISE EXCEPTION 'Premium tier display-name lock failed: % mismatch(es)', mismatches;
  END IF;
END $$;
