# Currency Logic — Raqeem

---

## The Rule

**The entire app operates in USD.** There is one exception: the **Wallet** account,
which is a cash account denominated in EGP (Egyptian Pounds).

---

## Why EGP Exists

The Wallet represents physical cash in hand. In Egypt, daily micro-expenses
(transportation, street food, small purchases) are paid in EGP. Rather than
converting every small expense back to USD, these are tracked directly in EGP.

---

## The Exchange Rate

- **Fixed rate.** There is no live API for exchange rates.
- Default: **1 USD = 52 EGP** (user-configurable in Settings).
- This rate is stored in the `settings.usd_to_egp_rate` column.
- When the rate changes in real life, the user updates it in Settings. Past
  transactions are NOT retroactively recalculated — they keep the rate they
  were entered with.

---

## Wallet Funding Flow (USD → EGP)

When the user transfers from any USD account to the Wallet:

1. The user enters the USD amount to send (e.g., `$10`).
2. The app automatically calculates the EGP equivalent using the fixed rate:
   `$10 × 52 = EGP 520`.
3. A bottom sheet appears with:
   ```
   Transfer $10.00 → Wallet
   ─────────────────────────
   At rate: 1 USD = 52 EGP
   You'll receive: EGP 520.00

   [Edit rate]   [Confirm transfer]
   ```
4. The user can tap "Edit rate" to override the rate for this specific transfer
   (e.g., if the bureau rate was different that day).
5. The transfer is saved with `is_currency_conversion = true` and the actual
   `exchange_rate` used.

**The transfer record stores:**
- `from_account`: Binance Savings (USD)
- `from_amount_cents`: 1000 (= $10.00)
- `from_currency`: USD
- `to_account`: Wallet (EGP)
- `to_amount_cents`: 52000 (= EGP 520.00, in piastres)
- `to_currency`: EGP
- `exchange_rate`: 52.0000
- `is_currency_conversion`: true

---

## Expense Tracking in EGP

When logging an expense from the Wallet:
- Currency is **EGP**.
- Amount is entered in EGP (e.g., EGP 35).
- Stored as 3500 piastres.
- The Wallet balance decrements in EGP.

---

## Analytics: Normalizing EGP to USD

All analytics and net worth calculations normalize everything to USD using the
**current** rate from `settings.usd_to_egp_rate`.

```typescript
function toUSD(cents: number, currency: Currency, usdToEgpRate: number): number {
  if (currency === 'USD') return cents;
  return Math.round(cents / usdToEgpRate);  // piastres → cents USD
}
```

**Example:** Wallet balance = EGP 520 (52000 piastres), rate = 52.
USD equivalent = 52000 / 52 = 1000 cents = $10.00.

---

## Display Rules

| Context | Show |
|---------|------|
| Wallet account balance | EGP 520.00 |
| Wallet in net worth total | $10.00 (converted) |
| Wallet transaction list | EGP 35.00 |
| Analytics spending charts | All in USD (EGP normalized) |
| Transfer confirmation | Both currencies shown |
| Budget bars | USD only (EGP budget is a separate line if set) |

---

## No Multi-Currency Anywhere Else

- All other accounts (Binance Free, Binance Savings, Charity) are USD only.
- There is no USDT or crypto currency type in the app — Binance accounts are
  tracked as USD values (whatever USDT = at time of recording).
- No auto-conversion API. No live rates. Simple, manual, reliable.

---

## Settings: Currency Config

In the Settings screen, there is a "Currency" section:

```
USD / EGP Exchange Rate
─────────────────────────────
Current rate:   1 USD = [52.00] EGP
                             [Save]

This rate is used for all transfers from USD accounts to your Wallet,
and for normalizing your Wallet balance in Analytics.
```

---

## Edge Case: Expense accidentally logged in wrong currency

If the user logs an EGP expense on a USD account (shouldn't happen — the
currency selector defaults to the account's currency and non-EGP accounts
should not show EGP option), the validation layer must catch this:

```typescript
// Validation rule
if (selectedAccount.currency === 'USD' && selectedCurrency === 'EGP') {
  throw new Error('Cannot log EGP expense on a USD account.');
}
```
