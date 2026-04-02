# Design System — Raqeem
## Obsidian Aesthetic · Minimal · Precise · Dark

---

## Philosophy

The app handles money. It should feel like a well-made instrument — not a
dashboard, not a chart dump, not a fintech startup template. Think Obsidian's
focused calm meets a Bloomberg Terminal's precision.

**Keywords:** Void dark · Surgical precision · Quiet authority · No decoration for
decoration's sake · Every pixel earns its place

**The test:** Remove all color from any screen. Does the layout still communicate
hierarchy, importance, and flow? If not, the design has failed.

---

## Color System

### Philosophy
Deep charcoal voids. Single purple accent. Nothing else competes.
Income is emerald. Expense is red. Budget warnings are amber.
That's the entire palette.

### CSS Variables (Web)

```css
:root {
  /* ── Voids (backgrounds) ─────────────────── */
  --void:           #080808;   /* true background, behind app shell */
  --bg-base:        #0F0F0F;   /* main app background */
  --bg-surface:     #161616;   /* cards, panels, sidebar */
  --bg-elevated:    #1E1E1E;   /* modals, popovers */
  --bg-subtle:      #242424;   /* input fields, hover states */
  --bg-overlay:     #2C2C2C;   /* context menus, tooltips bg */

  /* ── Purple Scale (primary accent) ──────── */
  --purple-950:     #0D0520;
  --purple-900:     #1A0D33;
  --purple-800:     #2D1B52;
  --purple-700:     #4C2A8A;
  --purple-600:     #6D28D9;
  --purple-500:     #7C3AED;   /* PRIMARY — main CTAs, active states */
  --purple-400:     #8B5CF6;   /* hover on primary */
  --purple-300:     #A78BFA;   /* links, secondary interactive */
  --purple-200:     #C4B5FD;   /* light accent text */
  --purple-100:     #EDE9FE;   /* near-white on purple */

  /* ── Text ────────────────────────────────── */
  --text-primary:   #F0F0F0;   /* headings, main content */
  --text-secondary: #A0A0A0;   /* labels, secondary info */
  --text-muted:     #5A5A5A;   /* placeholders, disabled */
  --text-accent:    #8B5CF6;   /* links, active labels */
  --text-inverse:   #0F0F0F;   /* text on purple/light backgrounds */

  /* ── Semantic ─────────────────────────────── */
  --positive:       #10B981;   /* income, gains — emerald */
  --positive-bg:    rgba(16, 185, 129, 0.10);
  --negative:       #F87171;   /* expenses, losses — soft red */
  --negative-bg:    rgba(248, 113, 113, 0.10);
  --warning:        #FBBF24;   /* budget warnings — amber */
  --warning-bg:     rgba(251, 191, 36, 0.10);

  /* ── Borders ─────────────────────────────── */
  --border-subtle:  rgba(255, 255, 255, 0.05);
  --border-default: rgba(255, 255, 255, 0.09);
  --border-strong:  rgba(255, 255, 255, 0.16);
  --border-accent:  rgba(139, 92, 246, 0.45);
  --border-positive:rgba(16, 185, 129, 0.35);
  --border-negative:rgba(248, 113, 113, 0.35);
}
```

### Dart / Flutter (`lib/core/theme/app_colors.dart`)

```dart
import 'package:flutter/material.dart';

class AppColors {
  // Voids
  static const void_    = Color(0xFF080808);
  static const bgBase   = Color(0xFF0F0F0F);
  static const bgSurface= Color(0xFF161616);
  static const bgElevated= Color(0xFF1E1E1E);
  static const bgSubtle = Color(0xFF242424);
  static const bgOverlay= Color(0xFF2C2C2C);

  // Purple
  static const purple500 = Color(0xFF7C3AED);
  static const purple400 = Color(0xFF8B5CF6);
  static const purple300 = Color(0xFFA78BFA);
  static const purple200 = Color(0xFFC4B5FD);

  // Text
  static const textPrimary   = Color(0xFFF0F0F0);
  static const textSecondary = Color(0xFFA0A0A0);
  static const textMuted     = Color(0xFF5A5A5A);
  static const textAccent    = Color(0xFF8B5CF6);

  // Semantic
  static const positive    = Color(0xFF10B981);
  static const positiveBg  = Color(0x1A10B981);
  static const negative    = Color(0xFFF87171);
  static const negativeBg  = Color(0x1AF87171);
  static const warning     = Color(0xFFFBBF24);
  static const warningBg   = Color(0x1AFBBF24);

  // Borders (as full colors — use .withOpacity() when needed)
  static const borderSubtle  = Color(0x0DFFFFFF);
  static const borderDefault = Color(0x17FFFFFF);
  static const borderStrong  = Color(0x29FFFFFF);
  static const borderAccent  = Color(0x738B5CF6);
}
```

### Tailwind Config (`tailwind.config.js`)

```js
module.exports = {
  theme: {
    extend: {
      colors: {
        void:   '#080808',
        base:   '#0F0F0F',
        surface:'#161616',
        elevated:'#1E1E1E',
        subtle: '#242424',
        overlay:'#2C2C2C',
        purple: {
          950: '#0D0520',
          900: '#1A0D33',
          800: '#2D1B52',
          700: '#4C2A8A',
          600: '#6D28D9',
          500: '#7C3AED',
          400: '#8B5CF6',
          300: '#A78BFA',
          200: '#C4B5FD',
          100: '#EDE9FE',
        },
        positive: '#10B981',
        negative: '#F87171',
        warning:  '#FBBF24',
      },
      fontFamily: {
        sans:  ['Inter', 'sans-serif'],
        mono:  ['JetBrains Mono', 'monospace'],
        serif: ['DM Serif Display', 'serif'],
      },
      borderRadius: {
        DEFAULT: '8px',
        lg: '12px',
        xl: '16px',
        '2xl': '20px',
      },
    },
  },
};
```

---

## Typography

### Web fonts
```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600&family=DM+Serif+Display&display=swap');
```

- **UI text / body**: Inter — system-quality sans, Obsidian uses it
- **Financial amounts**: JetBrains Mono — tabular nums, scannable
- **Hero numbers / section headers (sparingly)**: DM Serif Display

### Type Scale

| Role               | Web px | Mobile sp | Weight     | Font      |
|--------------------|--------|-----------|------------|-----------|
| Page hero amount   | 40–56  | 36–48     | 500        | JB Mono   |
| Page title         | 22–26  | 20–22     | 600        | Inter     |
| Section header     | 13–14  | 13        | 600 · CAPS | Inter     |
| Body / labels      | 14–15  | 14        | 400        | Inter     |
| Caption / meta     | 11–12  | 11–12     | 400        | Inter     |
| Amount (large)     | 24–32  | 20–28     | 500        | JB Mono   |
| Amount (inline)    | 14–15  | 13–14     | 400–500    | JB Mono   |

**Rule:** Section headers are ALWAYS uppercase with `letter-spacing: 0.08em`.
They use `--text-muted` color — they label, they don't shout.

---

## Spacing

**Base unit: 4px.** All spacing is a multiple of 4.

| Token   | Value | Use |
|---------|-------|-----|
| space-1 | 4px   | Icon gaps, tight labels |
| space-2 | 8px   | Internal component padding |
| space-3 | 12px  | Small gaps |
| space-4 | 16px  | Default padding, list gaps |
| space-5 | 20px  | Card internal padding |
| space-6 | 24px  | Section gaps |
| space-8 | 32px  | Major section separation |
| space-10| 40px  | Page top/bottom padding |

---

## Component Specs

### Cards
- Background: `--bg-surface`
- Border: 1px `--border-subtle`
- Border radius: 12px (web) / 16sp (Flutter)
- Internal padding: 20px / 16–20sp
- **No box shadows.** Depth is created by border and background color alone.
- On hover (web): border transitions to `--border-default`

```jsx
// Web base card
<div className="bg-surface border border-white/5 rounded-xl p-5 transition-colors hover:border-white/10">
```

```dart
// Flutter base card
Container(
  padding: const EdgeInsets.all(20),
  decoration: BoxDecoration(
    color: AppColors.bgSurface,
    borderRadius: BorderRadius.circular(16),
    border: Border.all(color: AppColors.borderSubtle),
  ),
)
```

### Buttons

**Primary (purple):**
- bg: `--purple-500` → hover: `--purple-400`
- text: white
- border: none
- height: 44px (both platforms)
- border-radius: 8px
- font: Inter 500, 14px

**Secondary (ghost with border):**
- bg: transparent → hover: `--bg-subtle`
- border: 1px `--border-default` → hover: `--border-strong`
- text: `--text-secondary` → hover: `--text-primary`

**Destructive:**
- bg: transparent → hover: `--negative-bg`
- border: 1px `--border-negative`
- text: `--negative`

**Minimum touch target: 44×44px on all interactive elements. No exceptions.**

### Input Fields

```css
/* Web */
.input {
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 10px 14px;
  color: var(--text-primary);
  font-family: Inter;
  font-size: 14px;
  transition: border-color 150ms;
}
.input:focus {
  outline: none;
  border-color: var(--border-accent);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.12);
}
.input::placeholder { color: var(--text-muted); }
```

- Label: always ABOVE the field, `--text-secondary`, 12px caps
- Error: `--negative` border + 11px error message below field
- Amount inputs: `JetBrains Mono`, align right if possible

### Bottom Sheet / Modal (Android + Web)

- Background: `--bg-elevated`
- Top border: 1px `--border-subtle`
- Handle bar (Android): 4×36px, `--border-default`, centered, 8px from top
- Rounded top corners: 20px
- Drag to dismiss: yes (Android)

### Amounts Display

```
Rule: ALWAYS monospace. ALWAYS currency symbol. ALWAYS +/− prefix.
Never use color alone to indicate positive/negative.
```

```jsx
// Web
<span className="font-mono text-positive">+$124.00</span>
<span className="font-mono text-negative">−$32.50</span>
```

```dart
// Flutter
Text(
  '−\$32.50',
  style: GoogleFonts.jetBrainsMono(
    color: AppColors.negative,
    fontFeatures: [FontFeature.tabularFigures()],
  ),
)
```

### Charts

- Library: Recharts (web) / Vico (Android)
- Background: transparent — they sit inside `--bg-surface` cards
- Grid lines: `--border-subtle`, extremely faint
- Axis labels: 11px, `--text-muted`
- Tooltip: `--bg-overlay` bg, 1px `--border-default` border, 8px radius
- Color sequence: `--purple-400` → `--positive` → `--warning`
- Always include axis labels. Never naked charts.
- Show loading skeleton while fetching, not a spinner over the chart area.

### Transaction List Item

```
[Category icon in subtle circle] [Title bold 14px] [Date muted 11px]
                                 [Account label 11px secondary]    [Amount mono right-aligned]
```

- Full-width, tap to expand detail
- Swipe left → delete (Android) / hover row actions (web)
- Amount: positive = `--positive`, expense = `--negative`

### Budget Bar

```
FOOD & DINING                                          $312 / $400
[████████████████████████░░░░░░] 78%
```
- Track bg: `--bg-subtle`, height: 4px, radius: 2px
- Fill: `--positive` 0–79%, `--warning` 80–99%, `--negative` 100%+
- No animated pulse — static is calmer

### Account Card

```
┌──────────────────────────────────────────┐
│  Binance Savings              SAVINGS    │
│                                          │
│  ●●●●  (hidden by default)               │
│  tap to reveal                           │
│                                          │
│  ████████████████████           +$240    │
│  this month                              │
└──────────────────────────────────────────┘
```
- Balance hidden until user taps the amount
- Mini sparkline showing last 30d activity

---

## Navigation

### Android — Bottom Navigation Bar
5 tabs, always visible:
1. **Home** (house icon) — quick add + recent transactions
2. **Accounts** (wallet icon) — account list + net worth
3. **Analytics** (chart icon) — charts + AI bot
4. **Budgets** (target icon) — category budgets + subscriptions
5. **Goals** (flag icon) — savings goals

- Active tab: `--purple-400` icon + label
- Inactive: `--text-muted`
- Background: `--bg-surface`, top border 1px `--border-subtle`
- FAB: purple circle, `+` icon, sits above center of nav bar

### Web — Left Sidebar (Desktop)
- Width: 220px
- Background: `--bg-surface`
- Right border: 1px `--border-subtle`
- Logo / app name at top
- Nav items: icon + label, active has left accent bar in `--purple-500`
- Bottom of sidebar: Settings link

---

## Animation Principles

- **Duration**: 120–200ms for micro-interactions, 250–350ms for transitions
- **Easing**: `cubic-bezier(0.16, 1, 0.3, 1)` for entrances (fast out)
- **No bouncing.** No spring physics on financial data. It's serious.
- **Skeleton loaders** — not spinners — for content loading
- **Page transitions**: subtle fade (150ms), not slides that feel like a website

---

## What Bad Design Looks Like Here — Avoid These

- Purple gradient on anything. One color, flat.
- Cards with 3+ levels of elevation stacked
- Charts dumped without context or labels
- Rounded everything at 24px+ (feels playful, not precise)
- Color-only distinction of positive/negative
- Section titles that are the same size as body text
- Padding under 12px anywhere the user reads content
- White backgrounds. Ever.
