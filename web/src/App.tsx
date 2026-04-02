import { zodResolver } from '@hookform/resolvers/zod';
import { Suspense, lazy, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, Route, Routes } from 'react-router-dom';
import { z } from 'zod';

import { AppShell } from './components/layout/AppShell';
import { Button } from './components/ui/Button';
import { Card } from './components/ui/Card';
import { Input } from './components/ui/Input';
import { Logo } from './components/ui/Logo';
import { useRealtime } from './hooks/useRealtime';
import { supabase } from './lib/supabase';
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
  const isConfigured = useAuthStore((state) => state.isConfigured);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AuthFormValues>({
    resolver: zodResolver(authSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = async (values: AuthFormValues) => {
    clearError();
    await signIn(values.email, values.password);
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-void px-4 py-10">
      <div className="w-full max-w-md animate-fadeSlide">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-5 flex justify-center">
            <Logo size={64} />
          </div>
          <p className="section-label mb-2">Authentication Required</p>
          <h1 className="text-3xl font-bold tracking-tight text-white">Raqeem</h1>
          <p className="mt-2 text-sm text-[#A0A0A0]">Quiet authority for your money.</p>
        </div>

        <Card className="space-y-6 p-8">
          {!isConfigured ? (
            <div className="rounded-lg border border-amber-400/20 bg-amber-400/5 p-4 text-xs leading-relaxed text-amber-200/80">
              <p className="section-label mb-1 text-amber-200">Configuration Required</p>
              The web app needs its Supabase environment variables before login can work.
            </div>
          ) : null}
          
          {authError ? (
            <div className="rounded-lg border border-red-500/20 bg-red-500/5 p-4 text-sm text-negative">
              {authError}
            </div>
          ) : null}

          <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
            <div className="space-y-4">
              <Input 
                error={errors.email?.message} 
                label="Email address" 
                placeholder="name@example.com" 
                {...register('email')} 
              />
              <Input 
                error={errors.password?.message} 
                label="Password" 
                type="password" 
                placeholder="••••••••"
                {...register('password')} 
              />
            </div>

            <Button 
              className="w-full h-12 text-sm font-semibold" 
              disabled={isSubmitting || !isConfigured} 
              type="submit"
            >
              {isSubmitting ? (
                <span className="flex items-center gap-2">
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white"></div>
                  Authenticating...
                </span>
              ) : (
                'Sign in to Workspace'
              )}
            </Button>
          </form>
        </Card>

        <div className="mt-6 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-white/5 bg-elevated px-3 py-1 text-[10px] font-medium uppercase tracking-widest text-[#5A5A5A]">
            Production Release v0.0.1
          </span>
        </div>
      </div>
    </div>
  );
}

export function App() {
  const initialize = useAuthStore((state) => state.initialize);
  const syncSessionUser = useAuthStore((state) => state.syncSessionUser);
  const status = useAuthStore((state) => state.status);
  const user = useAuthStore((state) => state.user);
  const bootstrap = useFinanceStore((state) => state.bootstrap);
  const resetFinance = useFinanceStore((state) => state.reset);
  useRealtime();

  useEffect(() => {
    void initialize();
  }, [initialize]);

  useEffect(() => {
    if (!supabase) {
      return undefined;
    }

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, session) => {
      syncSessionUser(session?.user ?? null);
    });

    return () => {
      subscription.unsubscribe();
    };
  }, [syncSessionUser]);

  useEffect(() => {
    if (status !== 'ready') {
      return;
    }

    if (user) {
      void bootstrap();
      return;
    }

    resetFinance();
  }, [bootstrap, resetFinance, status, user]);

  if (status === 'loading') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-void">
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
        <div className="flex min-h-screen items-center justify-center bg-void">
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
