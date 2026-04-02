import { zodResolver } from '@hookform/resolvers/zod';
import { LockKeyhole } from 'lucide-react';
import { Suspense, lazy, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, Route, Routes } from 'react-router-dom';
import { z } from 'zod';

import { AppShell } from './components/layout/AppShell';
import { Button } from './components/ui/Button';
import { Card } from './components/ui/Card';
import { Input } from './components/ui/Input';
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
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-void px-4 py-10 selection:bg-purple-500/30">
      {/* Background Atmosphere */}
      <div className="absolute left-1/2 top-1/2 h-[500px] w-[500px] -translate-x-1/2 -translate-y-1/2 animate-pulse-soft rounded-full bg-purple-600/10 blur-[120px]"></div>
      
      <div className="z-10 w-full max-w-md animate-fade-slide">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 shadow-xl shadow-purple-500/20">
            <LockKeyhole className="h-8 w-8 text-white" />
          </div>
          <p className="eyebrow mx-auto mb-2 w-fit">Authentication Required</p>
          <h1 className="font-serif text-4xl font-bold tracking-tight text-white md:text-5xl">Raqeem</h1>
          <p className="mt-3 text-sm text-[#A0A0A0]">Quiet authority for your money.</p>
        </div>

        <Card className="glass-card space-y-6 border-white/5 p-8 shadow-2xl">
          {!isConfigured ? (
            <div className="rounded-xl border border-amber-400/20 bg-amber-400/10 p-4 text-xs leading-relaxed text-amber-200/80">
              <p className="font-semibold text-amber-200 uppercase tracking-wider mb-1">Configuration Required</p>
              The web app needs its Supabase environment variables before login can work.
            </div>
          ) : null}
          
          {authError ? (
            <div className="rounded-xl border border-rose-400/20 bg-rose-400/10 p-4 text-sm text-negative animate-shake">
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
                className="bg-white/5 border-white/10 focus:border-purple-500/50"
              />
              <Input 
                error={errors.password?.message} 
                label="Password" 
                type="password" 
                placeholder="••••••••"
                {...register('password')} 
                className="bg-white/5 border-white/10 focus:border-purple-500/50"
              />
            </div>

            <Button 
              className="w-full h-12 gap-2 text-base font-semibold shadow-lg shadow-purple-500/20 transition-all hover:scale-[1.02] active:scale-[0.98]" 
              disabled={isSubmitting || !isConfigured} 
              type="submit"
            >
              {isSubmitting ? (
                <span className="flex items-center gap-2">
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white"></div>
                  Authenticating...
                </span>
              ) : (
                <>
                  <span>Sign in to Workspace</span>
                </>
              )}
            </Button>
          </form>
        </Card>

        <div className="mt-8 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-white/5 bg-white/5 px-3 py-1 text-[10px] font-medium uppercase tracking-[0.2em] text-[#5A5A5A]">
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
