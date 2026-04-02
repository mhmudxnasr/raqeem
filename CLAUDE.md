# Raqeem — Personal Finance Tracker
## Master Guide for Claude Code

---

## What This App Is

**Raqeem** (رقيم — Arabic for "record / written ledger") is a personal finance tracker
built for a single user. It replaces a Notion-based system and must feel like a premium,
native-quality app on both Android and web. The experience benchmark is **Revolut meets
Obsidian** — dark, minimal, precise, never generic.

There is **one user only**. No multi-tenant, no teams, no sharing. Build accordingly
— don't over-engineer auth or permissions.

---

## How to Navigate This Project

Before writing any code, read the relevant skill file for what you're working on.
This is not optional — the skills encode decisions that took real deliberation.

| What you're doing | Read this first |
|---|---|
| Any UI, colors, spacing, components | `skills/SKILL_DESIGN.md` |
| Android / Kotlin / Compose | `skills/SKILL_ANDROID.md` |
| Web / React / TypeScript | `skills/SKILL_WEB.md` |
| Supabase, schema, queries, RLS | `skills/SKILL_BACKEND.md` |
| Real-time sync between platforms | `skills/SKILL_SYNC.md` |
| Groq AI, analytics bot, insights | `skills/SKILL_AI.md` |
| Tests, QA, error handling | `skills/SKILL_QA.md` |

For full feature list: `docs/FEATURES.md`
For all screens described: `docs/SCREENS.md`
For database schema: `docs/DATA_MODEL.md`
For currency logic: `docs/CURRENCY_LOGIC.md`
For design tokens + component specs: `docs/DESIGN.md`

---

## Tech Stack

### Android
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material3 as base, heavily customized)
- **Architecture**: MVVM + Clean Architecture (UseCases + Repositories)
- **DI**: Hilt
- **Database (local)**: Room (offline-first)
- **Networking**: Ktor client
- **State**: StateFlow + ViewModel
- **Navigation**: Compose Navigation
- **Charts**: Vico (Compose-native)
- **Auth (local gate)**: BiometricPrompt
- **Build**: Gradle (KTS), minSdk 26, targetSdk 35

### Web
- **Framework**: React 18 + TypeScript (strict)
- **Build**: Vite
- **Styling**: Tailwind CSS (custom config — see SKILL_WEB.md)
- **State**: Zustand
- **Charts**: Recharts
- **HTTP client**: supabase-js
- **Routing**: React Router v6
- **Forms**: React Hook Form + Zod
- **Icons**: Lucide React (used sparingly)

### Backend
- **Platform**: Supabase
- **Database**: PostgreSQL (via Supabase)
- **Auth**: Supabase Auth (email + password, single account)
- **Realtime**: Supabase Realtime (postgres changes)
- **Storage**: Supabase Storage (receipt images)
- **Edge Functions**: Deno (for Groq proxy only)

### AI
- **Provider**: Groq API (llama3-70b-8192 or mixtral-8x7b)
- **Access**: Via Supabase Edge Function (never expose key client-side)
- **Features**: Monthly insights, anomaly detection, conversational bot

---

## Core Principles

1. **Offline-first on Android.** Every action writes to Room first. Supabase sync
   is background. If offline, the app works fully — sync when back online.

2. **Web is real-time.** Web fetches from Supabase directly and subscribes to
   realtime changes. No local DB on web.

3. **Single user.** No user_id foreign keys needed beyond Supabase's default
   auth.uid(). RLS policy: `auth.uid() = user_id` on every table.

4. **USD is the base currency.** All financial calculations and analytics are
   in USD. EGP is only used for the "Wallet" cash account. See
   `docs/CURRENCY_LOGIC.md` for the full conversion logic.

5. **No total net worth on the home screen.** The home screen is for quick-add
   actions and recent transactions. Net worth lives in the Accounts tab,
   hidden behind a tap.

6. **Design is law.** Do not invent new colors, spacings, or components.
   Everything is in `docs/DESIGN.md` and `skills/SKILL_DESIGN.md`.

---

## Project Structure

### Android App (`/android`)
```
android/
├── app/
│   └── src/main/
│       ├── data/
│       │   ├── local/          # Room DB, DAOs, entities
│       │   ├── remote/         # Supabase client, DTOs
│       │   └── repository/     # Repository implementations
│       ├── domain/
│       │   ├── model/          # Pure Kotlin data classes
│       │   ├── repository/     # Repository interfaces
│       │   └── usecase/        # One class per use case
│       └── presentation/
│           ├── ui/
│           │   ├── theme/      # Colors, Typography, Theme.kt
│           │   ├── components/ # Shared composables
│           │   └── screens/    # One folder per screen
│           └── viewmodel/
```

### Web App (`/web`)
```
web/
├── src/
│   ├── components/
│   │   ├── ui/           # Base components (Button, Input, Card...)
│   │   └── features/     # Feature-specific components
│   ├── pages/            # Route-level components
│   ├── store/            # Zustand stores
│   ├── lib/
│   │   ├── supabase.ts   # Supabase client
│   │   └── groq.ts       # Groq API calls (via edge function)
│   ├── types/            # TypeScript types
│   └── hooks/            # Custom hooks
```

### Backend (`/backend`)
```
backend/
├── supabase/
│   ├── migrations/       # SQL migration files
│   ├── functions/        # Edge functions (Deno)
│   └── seed.sql          # Initial seed data
```

---

## Non-Negotiable Rules

- **Never hardcode the Groq API key.** It goes only in Supabase Edge Function
  environment variables.
- **Never use `SELECT *` in production queries.** Always name the columns.
- **Never skip loading states.** Every async operation has a loading indicator.
- **Never swallow errors silently.** Log + show user-facing snackbar/toast.
- **Never use colors not in the design token system.**
- **All amounts displayed with 2 decimal places**, monospaced font, currency symbol.
- **Positive/negative amounts use `+` / `−` prefix AND color.** Never color alone.
- **All Compose previews must compile.** No `@Preview` that crashes.
- **All TypeScript must compile with zero errors.** No `any` types.
