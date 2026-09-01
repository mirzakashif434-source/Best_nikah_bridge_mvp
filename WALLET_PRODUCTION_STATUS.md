# Best Nikah Bridge — Production Wallet

The wallet screen and backend ledger are production-oriented and use real Firebase data only.

- Wallet balance: Firebase wallet ledger
- Earnings: server-side ledger entries
- Withdrawal request: authenticated callable backend
- KYC gate: required before withdrawal
- Withdrawal review: admin-controlled
- Payout state: pending review → approved pending payout → paid/rejected
- SAR and USDT request currencies are supported as ledger/request types
- No fake balances or simulated payouts are seeded

Actual money movement still requires a real payout provider/account and its required credentials/KYC/legal setup.
