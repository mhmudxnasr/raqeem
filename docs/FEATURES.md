# Features — Raqeem
## Prioritized Feature List

Priority: P0 = must ship, P1 = ship soon, P2 = later

---

## P0 — Core (Must Ship)

### Transaction Management
- [ ] Add expense (account, category, amount, date, note)
- [ ] Add income (account, category, amount, date, note)
- [ ] Add transfer between accounts (with USD→EGP conversion for Wallet)
- [ ] Edit any transaction
- [ ] Delete any transaction (soft delete)
- [ ] View full transaction history, grouped by date
- [ ] Search transactions by note/category/amount

### Accounts
- [ ] View all accounts
- [ ] Balance hidden by default, tap/click to reveal per account
- [ ] Real-time balance auto-calculated (income − expense + transfers)
- [ ] Add / edit / delete accounts
- [ ] 30-day mini sparkline per account

### Budgets
- [ ] Set monthly budget per category
- [ ] View budget utilization progress bar per category
- [ ] Total monthly budget overview
- [ ] Budget overspend warning (in-app, color change to red at 100%)
- [ ] Month selector to view historical budget usage

### Subscriptions
- [ ] Add / edit / delete subscriptions
- [ ] View upcoming billing dates
- [ ] Monthly subscription total summary

### Goals
- [ ] Create savings goals with name, target amount, optional deadline
- [ ] Fund goal via transfer (from any account)
- [ ] Goal progress bar
- [ ] Mark goal complete

### Sync
- [ ] Android: offline-first with Room, sync to Supabase when online
- [ ] Web: real-time sync via Supabase Realtime
- [ ] Conflict resolution: last-write-wins with `updated_at` timestamp
- [ ] Sync status indicator (synced / syncing / offline)

### Auth
- [ ] Email + password login (Supabase Auth)
- [ ] Android: biometric lock (PIN fallback) — gate the app locally
- [ ] Auto lock after 5 minutes background
- [ ] Session persistence (stay logged in)

### Settings
- [ ] Edit USD/EGP exchange rate
- [ ] Set default account for quick-add
- [ ] Manage categories (add, edit, budget, delete)
- [ ] Manage accounts (reorder, edit, delete)

---

## P1 — Important

### Analytics
- [ ] Spending by category — donut chart (current month)
- [ ] Income vs Expenses — bar chart (last 6 months)
- [ ] Net worth over time — line chart (monthly snapshots)
- [ ] Day-of-week spending heatmap
- [ ] Average daily spend (this month)
- [ ] Month-over-month comparison card

### AI — Groq Integration
- [ ] Monthly insight card on Analytics screen (auto-generated, refreshed monthly)
  - Biggest spending category
  - Unusual spend detected
  - Budget on-track status
  - Savings rate
- [ ] AI chat bot: ask anything about your finances
  - Context: current month transactions injected in system prompt
  - Responses grounded in real data — no hallucinated numbers
- [ ] "You may have a subscription you forgot about" detection

### Notifications (Android)
- [ ] Budget warning: push notification at 80% and 100% of monthly budget
- [ ] Subscription reminder: 3 days before next billing date
- [ ] Weekly summary notification (Sunday): week's spending recap

### Export
- [ ] Export all transactions to CSV (date range selectable)

---

## P2 — Future

### Import
- [ ] Import from Notion CSV export (map columns wizard)

### Android Widget
- [ ] Home screen 2×2 widget: quick-add expense button + today's spending
- [ ] Home screen 4×2 widget: today's spending + 3 recent transactions

### Advanced Analytics
- [ ] Custom date range for all charts
- [ ] Per-account analytics breakdown
- [ ] Annual summary view
- [ ] Savings rate trend

### Recurring Transactions
- [ ] Mark a transaction as recurring (weekly/monthly)
- [ ] Auto-log recurring transactions on due date
- [ ] "Did you forget to log this?" reminder if a recurring pattern breaks

### Multi-device
- [ ] Realtime sync to web while Android is open simultaneously
- [ ] Conflict log: show last sync time, any conflicts resolved

### Appearance
- [ ] Accent color picker (purple variants)
- [ ] Compact / Comfortable layout density toggle
