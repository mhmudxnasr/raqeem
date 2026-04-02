# Raqeem

Raqeem is a personal finance app with a native Android client, a React web client, and a Supabase backend for auth, sync, storage, and AI-backed insights.

## Live Links

- Web app: [https://mhmudxnasr.github.io/raqeem/#/](https://mhmudxnasr.github.io/raqeem/#/)
- Android release: [GitHub Releases](https://github.com/mhmudxnasr/raqeem/releases/latest)

## What Is In This Repo

- `android/`: Native Android app built with Kotlin and Jetpack Compose
- `web/`: React + Vite + Tailwind web client
- `backend/supabase/`: Supabase SQL migrations and Edge Functions
- `backend/scripts/`: One-off migration utilities, including the Notion import script
- `docs/`: Product, design, data model, and feature documentation
- `skills/`: Project-specific agent guidance files

## Current Product Scope

Raqeem is focused on day-to-day personal finance tracking with:

- Email/password auth through Supabase
- Accounts, balances, and transaction history
- Budgets and savings goals
- Analytics dashboards and charts
- AI insights and finance chat through a Supabase Edge Function proxy
- Realtime sync on web and sync infrastructure for Android
- Android biometric/app-lock support

The web app currently exposes dashboard, transactions, accounts, analytics, budgets, goals, and settings flows. The backend also includes subscriptions and settings tables that support the broader product roadmap.

## Architecture

- Android: Kotlin, Jetpack Compose, Room, WorkManager, Hilt
- Web: React 18, Vite, Tailwind CSS, Zustand, Recharts, Supabase JS
- Backend: Supabase Postgres, Realtime, Auth, Storage, Edge Functions
- AI: `backend/supabase/functions/ai-insights` proxies grounded finance prompts to Groq without exposing the API key to the client

## Local Development

### Web

1. Create `web/.env.local` with:
   - `VITE_SUPABASE_URL`
   - `VITE_SUPABASE_ANON_KEY`
2. Install dependencies:

   ```bash
   cd web
   npm ci
   ```

3. Start the dev server:

   ```bash
   npm run dev
   ```

### Android

1. Open `android/local.properties`
2. Add:
   - `SUPABASE_URL=...`
   - `SUPABASE_ANON_KEY=...`
3. Release signing values are also read from `local.properties`
4. Build from Android Studio or with Gradle from `android/`

### Supabase Backend

- SQL schema changes live in `backend/supabase/migrations/`
- The AI function lives in `backend/supabase/functions/ai-insights/index.ts`
- The optional Notion migration utility is in `backend/scripts/migrate-notion.mjs`

## Deployment

- The web app is deployed to GitHub Pages
- GitHub Pages is configured to deploy from GitHub Actions on `main`
- The production build uses the repository subpath base `/raqeem/`

## Notes

- Generated artifacts such as `web/dist`, Android build logs, and Supabase temp files are intentionally not tracked
- Some root-level XML files are preserved as design/export artifacts from the app exploration work
