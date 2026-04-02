---
name: SKILL_WEB
description: >
  Read this for ALL web work: React, TypeScript, Vite, Tailwind, Zustand,
  Recharts, and desktop-first layout. Read SKILL_DESIGN.md first.
---

# Web Skill — Raqeem

## Stack

- React 18 + TypeScript (strict mode, no `any`)
- Vite (build)
- Tailwind CSS (custom config — see `docs/DESIGN.md`)
- Zustand (state management)
- React Router v6
- React Hook Form + Zod (forms + validation)
- Recharts (charts)
- supabase-js v2 (data + realtime)
- Lucide React (icons — used sparingly)
- date-fns (date formatting)

---

## Project Structure

```
web/src/
├── components/
│   ├── ui/               # Primitive components: Button, Input, Card, Badge...
│   ├── layout/           # Sidebar, TopBar, Layout wrapper
│   └── features/         # Domain-specific: TransactionItem, AccountCard, BudgetBar...
├── pages/                # Route-level: HomePage, AccountsPage, AnalyticsPage...
├── store/                # Zustand stores: useTransactionStore, useAccountStore...
├── hooks/                # useTransactions, useAccounts, useRealtime...
├── lib/
│   ├── supabase.ts       # createClient, typed client
│   ├── groq.ts           # calls edge function
│   └── format.ts         # formatAmount, formatDate, etc.
├── types/                # All TypeScript types (import from docs/DATA_MODEL.md)
└── styles/
    └── globals.css       # CSS variables, @import fonts, base styles
```

---

## TypeScript Config

```json
// tsconfig.json
{
  "compilerOptions": {
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true,
    "noImplicitReturns": true
  }
}
```

Zero `any` types. Zero `@ts-ignore`. If TypeScript complains, fix the type.

---

## Supabase Client

```typescript
// lib/supabase.ts
import { createClient } from '@supabase/supabase-js';
import type { Database } from '../types/supabase'; // generated types

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

export const supabase = createClient<Database>(supabaseUrl, supabaseAnonKey, {
  realtime: { params: { eventsPerSecond: 10 } },
});
```

**Always use the typed client.** Generate types with:
```bash
npx supabase gen types typescript --project-id YOUR_PROJECT_ID > src/types/supabase.ts
```

---

## Zustand Store Pattern

```typescript
// store/useTransactionStore.ts
import { create } from 'zustand';
import { Transaction } from '../types';
import { supabase } from '../lib/supabase';

interface TransactionStore {
  transactions: Transaction[];
  isLoading: boolean;
  error: string | null;
  fetch: (filters?: TransactionFilters) => Promise<void>;
  add: (tx: NewTransaction) => Promise<void>;
  update: (id: string, changes: Partial<Transaction>) => Promise<void>;
  remove: (id: string) => Promise<void>;
}

export const useTransactionStore = create<TransactionStore>((set, get) => ({
  transactions: [],
  isLoading: false,
  error: null,

  fetch: async (filters) => {
    set({ isLoading: true, error: null });
    try {
      const { data, error } = await supabase
        .from('transactions')
        .select('id, account_id, category_id, type, amount_cents, currency, note, date, receipt_url, created_at')
        .order('date', { ascending: false })
        .limit(100);

      if (error) throw error;
      set({ transactions: data ?? [], isLoading: false });
    } catch (err) {
      set({ error: 'Failed to load transactions', isLoading: false });
    }
  },
  
  // ... other methods
}));
```

**Rule:** Never `SELECT *` — always name columns explicitly.

---

## Realtime Subscriptions

```typescript
// hooks/useRealtime.ts
export function useRealtime() {
  const fetchTransactions = useTransactionStore(s => s.fetch);
  const fetchAccounts = useAccountStore(s => s.fetch);

  useEffect(() => {
    const channel = supabase
      .channel('db-changes')
      .on('postgres_changes',
        { event: '*', schema: 'public', table: 'transactions' },
        () => fetchTransactions()
      )
      .on('postgres_changes',
        { event: '*', schema: 'public', table: 'accounts' },
        () => fetchAccounts()
      )
      .subscribe();

    return () => { supabase.removeChannel(channel); };
  }, []);
}
```

Initialize `useRealtime()` once at the top level (in `App.tsx` or Layout).

---

## Layout System (Desktop-First)

```tsx
// components/layout/Layout.tsx
export function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-screen bg-base overflow-hidden">
      <Sidebar />
      <main className="flex-1 overflow-y-auto p-8">
        {children}
      </main>
    </div>
  );
}
```

**Sidebar:** 220px fixed width, `bg-surface`, right border `border-white/5`.
**Main content:** max-width 1200px centered, 32px padding.

---

## Component Conventions

```tsx
// Every reusable component:
// 1. Named export (not default)
// 2. Props interface defined above the component
// 3. No inline styles — Tailwind classes only
// 4. Loading/error/empty states handled

interface TransactionItemProps {
  transaction: Transaction;
  onEdit?: (id: string) => void;
  onDelete?: (id: string) => void;
}

export function TransactionItem({ transaction, onEdit, onDelete }: TransactionItemProps) {
  // ...
}
```

---

## Form Pattern (React Hook Form + Zod)

```tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const addTransactionSchema = z.object({
  type: z.enum(['income', 'expense']),
  amountCents: z.number().int().positive('Amount must be greater than 0'),
  accountId: z.string().uuid(),
  categoryId: z.string().uuid().optional(),
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  note: z.string().max(200).optional(),
});

type AddTransactionFormValues = z.infer<typeof addTransactionSchema>;

function AddTransactionForm({ onSuccess }: { onSuccess: () => void }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<AddTransactionFormValues>({
    resolver: zodResolver(addTransactionSchema),
    defaultValues: { type: 'expense', date: new Date().toISOString().split('T')[0] },
  });

  const onSubmit = async (values: AddTransactionFormValues) => {
    await useTransactionStore.getState().add(values);
    onSuccess();
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {/* form fields */}
      {errors.amountCents && <p className="text-negative text-xs">{errors.amountCents.message}</p>}
      <button type="submit" disabled={isSubmitting} className="btn-primary w-full">
        {isSubmitting ? 'Saving...' : 'Add Transaction'}
      </button>
    </form>
  );
}
```

---

## Chart Conventions (Recharts)

```tsx
// Always wrap in bg-surface card with title
// Always include axis labels and tooltip
// Use token colors

const CHART_COLORS = {
  income: '#10B981',   // --positive
  expense: '#F87171',  // --negative  
  accent: '#8B5CF6',   // --purple-400
  warning: '#FBBF24',  // --warning
};

<BarChart data={data} margin={{ top: 8, right: 8, bottom: 8, left: 8 }}>
  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
  <XAxis dataKey="month" tick={{ fill: '#5A5A5A', fontSize: 11 }} />
  <YAxis tick={{ fill: '#5A5A5A', fontSize: 11 }} />
  <Tooltip
    contentStyle={{ background: '#2C2C2C', border: '1px solid rgba(255,255,255,0.09)', borderRadius: '8px' }}
    labelStyle={{ color: '#F0F0F0' }}
    itemStyle={{ color: '#A0A0A0' }}
  />
  <Bar dataKey="income" fill={CHART_COLORS.income} radius={[4,4,0,0]} />
  <Bar dataKey="expense" fill={CHART_COLORS.expense} radius={[4,4,0,0]} />
</BarChart>
```

---

## Key CSS Classes to Define in globals.css

```css
/* globals.css */
.btn-primary {
  @apply bg-purple-500 hover:bg-purple-400 text-white font-medium text-sm
         h-11 px-4 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed;
}
.btn-secondary {
  @apply bg-transparent hover:bg-subtle border border-white/10 hover:border-white/20
         text-[#A0A0A0] hover:text-[#F0F0F0] font-medium text-sm
         h-11 px-4 rounded-lg transition-colors;
}
.card {
  @apply bg-surface border border-white/5 rounded-xl p-5;
}
.section-label {
  @apply text-[11px] font-semibold tracking-widest uppercase text-[#5A5A5A];
}
.amount-positive { @apply font-mono text-[#10B981]; }
.amount-negative { @apply font-mono text-[#F87171]; }
```

---

## What NOT to Do

- No `useState` for server data — use Zustand stores + Supabase queries
- No `useEffect` for data fetching — use the store's `fetch()` in the page's `useEffect` on mount
- No default exports for components
- No barrel files (`index.ts`) that re-export everything — import directly
- No `any` in TypeScript
- No inline color values (`style={{ color: '#7C3AED' }}`) — use Tailwind classes
- No `console.log` left in production code
