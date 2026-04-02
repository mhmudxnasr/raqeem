# Screens — Raqeem
## Every screen on both platforms

---

## Android Screens

### 1. Home Screen (Tab 1)

**Purpose:** Quick-add actions + recent activity. No clutter.

**Layout:**
```
┌─────────────────────────────────────┐
│  Good morning              [🔔]     │  ← greeting + notification bell
│  Wednesday, April 2                 │
├─────────────────────────────────────┤
│                                     │
│  ┌──────────────┐  ┌─────────────┐  │
│  │  + Expense   │  │  + Income   │  │  ← two large quick-add cards
│  │   (red tint) │  │ (green tint)│  │    tap → opens AddTransaction sheet
│  └──────────────┘  └─────────────┘  │
│                                     │
│  TODAY                              │  ← section header (caps, muted)
│  ─────────────────────────────────  │
│  [transaction item]                 │
│  [transaction item]                 │
│  [transaction item]                 │
│                                     │
│  YESTERDAY                          │
│  ─────────────────────────────────  │
│  [transaction item]                 │
│  [transaction item]                 │
│                                     │
│  [See all transactions →]           │
└─────────────────────────────────────┘
     [Home] [Accounts] [+] [Budgets] [Goals]
```

**Rules:**
- No total balance shown here.
- Quick-add cards are large (full half-width), easy to tap.
- Transaction list is infinite-scrolling, grouped by date.
- FAB (+) in the center of the bottom nav floats above it.

---

### 2. Add Transaction Bottom Sheet

**Triggered by:** FAB, or tapping either quick-add card.
**Type pre-selected** if opened from quick-add cards.

**Layout:**
```
─────── (drag handle)
Add Expense

AMOUNT
[  − $  _________ ]   ← large, JB Mono, cursor blinks

ACCOUNT           CATEGORY
[Binance Free ▾]  [Food & Dining ▾]

DATE              NOTE
[Today ▾]         [Optional note...]

RECEIPT
[Attach photo +]

                    [Add Expense]  ← purple CTA full width
```

**Rules:**
- Amount field is focused immediately on open.
- Account defaults to user's default account (from settings).
- If the selected account is Wallet (EGP), the amount field shows `EGP` prefix.
- Date picker: calendar sheet, defaults to today.
- Category picker: scrollable grid of icons.
- Keyboard appears instantly — no extra tap needed.

---

### 3. Add Transfer Bottom Sheet

**Triggered by:** Transfer button (floating in Accounts screen or via FAB → "Transfer").

**Layout:**
```
─────── (drag handle)
Transfer

FROM              TO
[Binance Free ▾]  [Wallet ▾]

AMOUNT
[$  __________]

── If TO is Wallet (EGP) ──────────────────
At rate: 1 USD = 52 EGP
You'll receive: EGP 0.00
              [Edit rate]
───────────────────────────────────────────

DATE              NOTE
[Today ▾]         [Optional note...]

FUND A GOAL?      [Select goal ▾]

             [Confirm Transfer]
```

---

### 4. Accounts Screen (Tab 2)

```
┌─────────────────────────────────────┐
│  Accounts                           │
│                                     │
│  NET WORTH                          │
│  ●●●●●●●  [eye icon to reveal]      │  ← hidden by default
│                                     │
│  ┌───────────────────────────────┐  │
│  │ Binance Savings      SAVING   │  │
│  │ ●●●●●●●●             [reveal] │  │
│  │ Last 30d:  ────────── +$240  │  │  ← sparkline
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ Binance Free       CHECKING   │  │
│  │ ●●●●●●●●             [reveal] │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ Wallet                  CASH  │  │
│  │ ●●●●●●●●  EGP        [reveal] │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ Charity            CHECKING   │  │
│  │ ●●●●●●●●             [reveal] │  │
│  └───────────────────────────────┘  │
│                                     │
│  [+ Add Account]                    │
└─────────────────────────────────────┘
```

**Rules:**
- Each balance is hidden behind a tap (eye icon per account).
- Net worth requires a long press or separate eye icon at the top.
- Tapping an account card opens the Account Detail screen.

---

### 5. Account Detail Screen

```
← Binance Savings           [⋮ Edit]

$●●●●●●●                [👁 Reveal]

INCOME THIS MONTH          EXPENSES
+$2,400.00                 −$180.00

[30d sparkline / mini chart]

TRANSACTIONS ─────────────────────

[Full transaction list for this account]
```

---

### 6. Analytics Screen (Tab 3)

```
┌─────────────────────────────────────┐
│  Analytics          [Month ▾] [Apr] │
│                                     │
│  SPENDING BY CATEGORY               │
│  ┌───────────────────────────────┐  │
│  │     [Donut chart]             │  │
│  │  Food 38%  Transport 22% ...  │  │
│  └───────────────────────────────┘  │
│                                     │
│  INCOME VS EXPENSES                 │
│  ┌───────────────────────────────┐  │
│  │     [Bar chart, 6 months]     │  │
│  └───────────────────────────────┘  │
│                                     │
│  NET WORTH OVER TIME                │
│  ┌───────────────────────────────┐  │
│  │     [Line chart]              │  │
│  └───────────────────────────────┘  │
│                                     │
│  AI INSIGHTS  ── ── ── ── ── ──     │  ← Groq-powered
│  ┌───────────────────────────────┐  │
│  │ 💡 "You spent 23% more on     │  │
│  │ food this month vs last. Your │  │
│  │ highest spend day was Apr 3." │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ [Chat with AI assistant]  [→] │  │  ← opens AI bot full screen
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

---

### 7. AI Bot Full Screen

```
← Financial Assistant

─────────────────────────────────────
                   You
      How much did I spend on food
      this month?

 Raqeem AI                           
 You've spent $93.50 on Food &
 Dining in April — that's 78% of
 your $120 monthly budget with
 12 days left in the month.
 [See transactions →]
─────────────────────────────────────

[Ask anything about your finances...]  [→]
```

**Rules:**
- Groq has full read access to this month's transactions via context injection.
- The AI should NEVER make up numbers — data is always fetched first and
  injected into the prompt. See `docs/AI_INTEGRATION.md`.
- Suggestions shown at top of empty chat: "What did I spend most on?",
  "How's my savings rate?", "Am I on track for my goals?"

---

### 8. Budgets Screen (Tab 4)

```
┌─────────────────────────────────────┐
│  Budgets                   April    │
│                                     │
│  TOTAL SPENT                        │
│  $312 of $600 budget (52%)          │
│  [████████████░░░░░░░░░░░░]         │
│                                     │
│  BY CATEGORY ───────────────────── │
│                                     │
│  Food & Dining                      │
│  $93 / $120                         │
│  [█████████████████░░░] 78%  ⚠️     │
│                                     │
│  Transportation                     │
│  $18 / $30                          │
│  [████████░░░░░░░░░░░░] 60%         │
│  ...                                │
│                                     │
│  SUBSCRIPTIONS ─────────────────── │
│  ┌───────────────────────────────┐  │
│  │ ChatGPT Plus   $20  Monthly   │  │
│  │ Next: Apr 15                  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

---

### 9. Goals Screen (Tab 5)

```
┌─────────────────────────────────────┐
│  Goals                   [+ New]    │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ 🏆 BIGGG DREAAM               │  │
│  │ $1,240 / $5,000               │  │
│  │ [████████░░░░░░░░░░░░░] 25%   │  │
│  │ No deadline                   │  │
│  │               [+ Add funds]   │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ 🥇 Gold                       │  │
│  │ $400 / $1,000                 │  │
│  │ [████████████░░░░░░░] 40%     │  │
│  │               [+ Add funds]   │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

**"Add funds" → opens Transfer sheet with goal pre-selected.**

---

### 10. Settings Screen

Accessible via: icon in top right of any screen (or side nav on web).

**Sections:**
- **Account** — email, change password
- **Currency** — USD/EGP rate editor
- **Accounts** — manage accounts (reorder, edit, delete)
- **Categories** — manage categories and budgets
- **Default Account** — which account pre-selects in Add Transaction
- **AI Assistant** — toggle insights on/off
- **Data** — export to CSV, import from Notion CSV
- **Appearance** — (future: accent color picker)
- **About / Feedback**

---

## Web Screens

The web is desktop-first. Left sidebar navigation, main content area.

### Web Sidebar (always visible)

```
┌────────────────────┐
│  Raqeem  رقيم      │
├────────────────────┤
│▌ Home              │  ← active = left purple bar
│  Transactions      │
│  Accounts          │
│  Analytics         │
│  Budgets           │
│  Goals             │
├────────────────────┤
│  Settings          │
└────────────────────┘
```

---

### Web: Home / Dashboard

```
┌──────────────────────────────────────────────────────────────────┐
│ Good morning · Wednesday, April 2                   [+ Add]      │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌───────────────────┐  ┌───────────────────┐                   │
│  │ + Add Expense     │  │ + Add Income      │                   │
│  └───────────────────┘  └───────────────────┘                   │
│                                                                   │
│  RECENT TRANSACTIONS ─────────────────────────────────────────  │
│  [full width transaction list, paginated, filterable]            │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

### Web: Transactions Page

Full transaction history with filters.

**Filter bar:** Date range | Account | Category | Type | Search

**Table view:**
```
Date        Type      Category          Account           Amount
────────────────────────────────────────────────────────────────
Apr 2       Expense   Food & Dining     Binance Free     −$12.50
Apr 1       Income    Outlier           Binance Free    +$240.00
Apr 1       Transfer  —                 → Wallet        −$10.00
```

**Right panel (appears on row click):** Full transaction detail + edit form.

---

### Web: Analytics Page

Three-column layout on wide screens:
- Left: Spending by category (donut) + budget bars
- Center: Income vs Expenses (bar, 12 months) + Net Worth line chart
- Right: AI Insights panel + quick-ask input

---

### Web: Accounts Page

Grid of account cards. Each card:
- Account name + type badge
- Balance (click eye to reveal)
- Mini 30d sparkline
- This month: income / expenses

Click any card → Account Detail drawer slides in from right.

---

### Web: Goals Page

Same as Android but larger cards, shown in a 2-column grid.

---

### Web: Budgets Page

Left column: total budget overview.
Right column: per-category progress bars, full width.
Below: Subscriptions table.
