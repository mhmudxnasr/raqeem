import { create } from 'zustand';

import { isSupabaseConfigured, supabase } from '../lib/supabase';
import type { UserSummary } from '../types';

interface AuthState {
  user: UserSummary | null;
  status: 'loading' | 'ready';
  error: string | null;
  isDemoMode: boolean;
  initialize: () => Promise<void>;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  clearError: () => void;
}

function buildUserSummary(id: string, email: string | undefined): UserSummary {
  return {
    id,
    email: email ?? 'demo@raqeem.app',
    displayName: 'Mahmud',
  };
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  status: 'loading',
  error: null,
  isDemoMode: !isSupabaseConfigured,
  initialize: async () => {
    if (!isSupabaseConfigured || !supabase) {
      set({
        user: buildUserSummary('demo-user', 'demo@raqeem.app'),
        status: 'ready',
        error: null,
        isDemoMode: true,
      });
      return;
    }

    const {
      data: { session },
      error,
    } = await supabase.auth.getSession();

    if (error) {
      set({ user: null, status: 'ready', error: 'Unable to restore your session.', isDemoMode: false });
      return;
    }

    set({
      user: session?.user ? buildUserSummary(session.user.id, session.user.email) : null,
      status: 'ready',
      error: null,
      isDemoMode: false,
    });
  },
  signIn: async (email, password) => {
    if (!isSupabaseConfigured || !supabase) {
      set({ user: buildUserSummary('demo-user', email), status: 'ready', error: null, isDemoMode: true });
      return;
    }

    const { data, error } = await supabase.auth.signInWithPassword({ email, password });
    if (error || !data.user) {
      set({ error: error?.message ?? 'Sign in failed.' });
      throw new Error(error?.message ?? 'Sign in failed.');
    }

    set({ user: buildUserSummary(data.user.id, data.user.email), status: 'ready', error: null, isDemoMode: false });
  },
  signOut: async () => {
    if (!isSupabaseConfigured || !supabase) {
      set({ user: buildUserSummary('demo-user', 'demo@raqeem.app'), status: 'ready', error: null, isDemoMode: true });
      return;
    }

    const { error } = await supabase.auth.signOut();
    if (error) {
      set({ error: 'Unable to sign out right now.' });
      throw new Error(error.message);
    }

    set({ user: null, error: null, status: 'ready', isDemoMode: false });
  },
  clearError: () => {
    set({ error: null });
  },
}));
