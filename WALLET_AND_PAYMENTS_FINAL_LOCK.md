# BEST NIKAH BRIDGE — WALLET & PAYMENTS FINAL LOCK

**Status: FINAL / LOCKED — 2026-08-31**

## Nikah Wallet
- Real production wallet architecture; no demo balances or fake transactions.
- Separate available balance, pending balance and transaction ledger.
- Every balance-changing operation is server-authorized and auditable.
- Users cannot edit their balance from the app.
- Idempotency and duplicate-withdrawal protection are required.
- Fraud/risk controls and suspicious-transaction review are required.

## Upgrades / Payments
- Best Nikah Bridge must have a real in-app **Upgrade / Premium** payment flow.
- Premium purchases must use the payment mechanism required by the distribution platform and applicable local rules.
- Payment status must be verified server-side before granting premium entitlements.
- Never trust a client-only "payment successful" flag.
- Receipts/transaction identifiers must be stored securely for reconciliation.

## Withdrawals
- Users can request withdrawal from their **eligible earned balance** to a supported payout method.
- Supported payout methods are country/provider dependent; the app must show only methods actually available for the user's verified country.
- Bank-account payout and supported card/ATM-accessible payout options may be offered where the licensed payment provider supports them.
- Currency display and conversion must support multiple currencies (including USD, SAR and PKR), while settlement is made in the payout provider's supported local/settlement currency.
- Exchange rates, fees and the final received amount must be shown before confirmation.
- Withdrawal status: pending → processing → paid / failed / reversed.
- KYC/identity verification is required whenever the payout provider or applicable law requires it.
- Minimum withdrawal, limits, fees and supported countries are configurable server-side.

## Critical financial rule
Best Nikah Bridge itself must not pretend to be a bank, ATM network, money transmitter or licensed payment institution. Real-money custody, card charging and payouts must be handled through an appropriate licensed/payment-provider infrastructure available in each supported country.

## Security
- Server-side ledger is the source of truth.
- Webhooks/signatures must be verified before changing payment or payout status.
- No secret API keys in the Android app.
- No client-side balance manipulation.
- Full audit trail for credits, debits, refunds, chargebacks and withdrawals.
- Account ownership and payout destination must be verified.

## Final product decision
The owner explicitly requested a real wallet, real Upgrade/Payments, and real withdrawals rather than demo functionality. This specification is locked. The exact payment/payout provider must be selected and integrated according to supported countries, Google Play requirements, KYC/AML obligations, fees and legal availability before production money movement is enabled.
