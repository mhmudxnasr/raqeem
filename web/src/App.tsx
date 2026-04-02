import { zodResolver } from '@hookform/resolvers/zod';
import { LockKeyhole, Mail } from 'lucide-react';
import { Suspense, lazy, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, Route, Routes } from 'react-router-dom';
import { z } from 'zod';

import { AppShell } from './components/layout/AppShell';
import { Button } from './components/ui/Button';
import { Card } from './components/ui/Card';
import { Input } from './components/ui/Input';
import { useRealtime } from './hooks/useRealtime';
import { useAuthStore } from './store/useAuthStore';
import { useFinanceStore } from './store/useFinanceStore';

const DashboardPage = lazy(async () => ({ default: (await import('./pages/DashboardPage')).DashboardPage }));
const TransactionsPage = lazy(async () => ({ default: (await import('./pages/TransactionsPage')).TransactionsPage }));
const AccountsPage = lazy(async () => ({ default: (await import('./pages/AccountsPage')).AccountsPage }));
const AnalyticsPage = lazy(async () => ({ default: (await import('./pages/AnalyticsPage')).AnalyticsPage }));
const BudgetsPage = lazy(async () => ({ default: (await import('./pages/BudgetsPage')).BudgetsPage }));
const GoalsPage = lazy(async () => ({ default: (await import('./pages/GoalsPage')).GoalsPage }));
const SettingsPage = lazy(async () => ({ default: (await import('./pages/SettingsPage')).SettingsPage }));

const authSchema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(6, 'Use at least 6 characters'),
});

type AuthFormValues = z.infer<typeof authSchema>;

function AuthScreen() {
  const signIn = useAuthStore((state) => state.signIn);
  const authError = useAuthStore((state) => state.error);
  const clearError = useAuthStore((state) => state.clearError);
  const isDemoMode = useAuthStore((state) => state.isDemoMode);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AuthFormValues>({
    resolver: zodResolver(authSchema),
    defaultValues: {
      email: 'demo@raqeem.app',
      password: 'demo123',
    },
  });

  const onSubmit = async (values: AuthFormValues) => {
    clearError();
    await signIn(values.email, values.password);
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <Card className="w-full max-w-md space-y-6 border-white/10 bg-elevated">
        <div className="space-y-3">
          <p className="section-label">Personal finance tracker</p>
          <div className="flex items-center gap-3">
            <img alt="Raqeem logo" className="h-11 w-11 rounded-xl border border-white/5 bg-base p-2" src="/logo.svg" />
            <div>
              <h1 className="text-2xl font-semibold text-[#F0F0F0]">Raqeem</h1>
              <p className="text-sm text-[#A0A0A0]">Calm, precise money tracking for one person.</p>
            </div>
          </div>
          {isDemoMode ? (
            <div className="rounded-xl border border-amber-400/20 bg-amber-400/10 p-4 text-sm text-amber-100">
              Supabase env vars are missing, so the app is running in demo mode with seeded finance data.
            </div>
          ) : null}
          {authError ? <div className="rounded-xl border border-rose-400/20 bg-rose-400/10 p-4 text-sm text-negative">{authError}</div> : null}
        </div>

        <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
          <Input error={errors.email?.message} label="Email" placeholder="you@example.com" {...register('email')} />
          <Input error={errors.password?.message} label="Password" type="password" {...register('password')} />
          <Button className="w-full gap-2" disabled={isSubmitting} type="submit">
            {isDemoMode ? <Mail className="h-4 w-4" /> : <LockKeyhole className="h-4 w-4" />}
            {isSubmitting ? 'Signing in...' : isDemoMode ? 'Open demo workspace' : 'Sign in'}
          </Button>
        </form>
      </Card>
    </div>
  );
}

export function App() {
  const initialize = useAuthStore((state) => state.initialize);
  const status = useAuthStore((state) => state.status);
  const user = useAuthStore((state) => state.user);
  const bootstrap = useFinanceStore((state) => state.bootstrap);
  useRealtime();

  useEffect(() => {
    void initialize();
  }, [initialize]);

  useEffect(() => {
    if (status === 'ready' && user) {
      void bootstrap();
    }
  }, [bootstrap, status, user]);

  if (status === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-sm text-[#A0A0A0]">Loading workspace...</p>
      </div>
    );
  }

  if (!user) {
    return <AuthScreen />;
  }

  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center">
          <p className="text-sm text-[#A0A0A0]">Loading page...</p>
        </div>
      }
    >
      <Routes>
        <Route element={<AppShell />}>
          <Route element={<DashboardPage />} path="/" />
          <Route element={<TransactionsPage />} path="/transactions" />
          <Route element={<AccountsPage />} path="/accounts" />
          <Route element={<AnalyticsPage />} path="/analytics" />
          <Route element={<BudgetsPage />} path="/budgets" />
          <Route element={<GoalsPage />} path="/goals" />
          <Route element={<SettingsPage />} path="/settings" />
        </Route>
        <Route element={<Navigate replace to="/" />} path="*" />
      </Routes>
    </Suspense>
  );
}
