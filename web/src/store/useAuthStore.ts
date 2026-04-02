import { create } from 'zustand';

import { isSupabaseConfigured, supabase } from '../lib/supabase';
import type { UserSummary } from '../types';

interface AuthState {
  user: UserSummary | null;
  status: 'loading' | 'ready';
  error: string | null;
  isConfigured: boolean;
  initialize: () => Promise<void>;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  syncSessionUser: (user: { id: string; email?: string } | null) => void;
  clearError: () => void;
}

const missingConfigMessage = 'Supabase is not configured for the web app. Add the Vite Supabase env vars and reload.';
const sessionRestoreErrorMessage = 'We could not restore your session. Please sign in again.';
const sessionInitializationTimeoutMs = 5000;

function buildUserSummary(id: string, email: string | undefined): UserSummary {
  return {
    id,
    email: email ?? '',
    displayName: email?.split('@')[0] ?? 'Raqeem',
  };
}

async function getSessionWithTimeout() {
  if (!supabase) {
    throw new Error('Supabase client is unavailable.');
  }

  const client = supabase;

  return await new Promise<Awaited<ReturnType<typeof client.auth.getSession>>>((resolve, reject) => {
    const timeoutId = setTimeout(() => {
      reject(new Error('Timed out while restoring the auth session.'));
    }, sessionInitializationTimeoutMs);

    void client.auth.getSession().then(
      (result) => {
        clearTimeout(timeoutId);
        resolve(result);
      },
      (error) => {
        clearTimeout(timeoutId);
        reject(error);
      },
    );
  });
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  status: 'loading',
  error: null,
  isConfigured: isSupabaseConfigured,
  initialize: async () => {
    if (!isSupabaseConfigured || !supabase) {
      set({
        user: null,
        status: 'ready',
        error: missingConfigMessage,
        isConfigured: false,
      });
      return;
    }

    try {
      const {
        data: { session },
        error,
      } = await getSessionWithTimeout();

      if (error) {
        set({ user: null, status: 'ready', error: sessionRestoreErrorMessage, isConfigured: true });
        return;
      }

      set({
        user: session?.user ? buildUserSummary(session.user.id, session.user.email) : null,
        status: 'ready',
        error: null,
        isConfigured: true,
      });
    } catch (error) {
      console.error('Failed to initialize auth session.', error);
      set({ user: null, status: 'ready', error: sessionRestoreErrorMessage, isConfigured: true });
    }
  },
  signIn: async (email, password) => {
    if (!isSupabaseConfigured || !supabase) {
      set({ user: null, status: 'ready', error: missingConfigMessage, isConfigured: false });
      throw new Error(missingConfigMessage);
    }

    const { data, error } = await supabase.auth.signInWithPassword({ email, password });
    if (error || !data.user) {
      set({ error: error?.message ?? 'Sign in failed.' });
      throw new Error(error?.message ?? 'Sign in failed.');
    }

    set({ user: buildUserSummary(data.user.id, data.user.email), status: 'ready', error: null, isConfigured: true });
  },
  signOut: async () => {
    if (!isSupabaseConfigured || !supabase) {
      set({ user: null, status: 'ready', error: missingConfigMessage, isConfigured: false });
      throw new Error(missingConfigMessage);
    }

    const { error } = await supabase.auth.signOut();
    if (error) {
      set({ error: 'Unable to sign out right now.' });
      throw new Error(error.message);
    }

    set({ user: null, error: null, status: 'ready', isConfigured: true });
  },
  syncSessionUser: (user) => {
    set({
      user: user ? buildUserSummary(user.id, user.email) : null,
      status: 'ready',
      error: null,
      isConfigured: isSupabaseConfigured,
    });
  },
  clearError: () => {
    set({ error: null });
  },
}));
