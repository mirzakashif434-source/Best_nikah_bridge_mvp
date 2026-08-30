# Best Nikah Bridge — Production Monetization Policy

## Free access
- Core profile creation and genuine matching remain available without payment.
- A free user can earn **2 message credits per day by completing 2 rewarded ads**.
- No fake reward buttons: a message credit must only be granted after a genuine rewarded-ad reward is verified by the production ad flow.
- One free boost is available to every eligible user once every **8 days**.

## Paid options
Play Console one-time products are reserved for these real product IDs:

| Price | Product ID | Message credits |
|---|---|---:|
| SAR 20 | `bnb_plus_20` | 10 |
| SAR 40 | `bnb_plus_40` | 30 |
| SAR 60 | `bnb_plus_60` | 60 |

The app must never grant paid credits from a client-side price or button click. The purchase token must be verified against Google Play by the Firebase backend first.

## Production requirements before publishing
1. Create the three one-time products in Google Play Console with the exact IDs above and the intended Saudi Riyal prices.
2. Create the real AdMob app and rewarded-ad unit, then replace development/test ad IDs with the production IDs.
3. Configure the Firebase Functions secret `PLAY_SERVICE_ACCOUNT_JSON` with a Google Play Developer API service account that has access to this app.
4. Wire the Android purchase/reward UI to the backend functions before the final release AAB.
5. Do not publish while test AdMob IDs or unverified purchase grants are present.

## Principle
Best Nikah Bridge is affordable first: payment unlocks additional communication capacity and convenience; essential safe Nikah discovery is not sold as a fake guarantee of a match.
