import { useEffect } from 'react';

import { supabase } from '../lib/supabase';
import { useAuthStore } from '../store/useAuthStore';
import { useFinanceStore } from '../store/useFinanceStore';

export function useRealtime(): void {
  const user = useAuthStore((state) => state.user);
  const isDemoMode = useAuthStore((state) => state.isDemoMode);
  const refresh = useFinanceStore((state) => state.refresh);

  useEffect(() => {
    if (isDemoMode || !supabase || !user) {
      return undefined;
    }

    const channel = supabase
      .channel('raqeem-db-changes')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'transactions' }, () => {
        void refresh();
      })
      .on('postgres_changes', { event: '*', schema: 'public', table: 'transfers' }, () => {
        void refresh();
      })
      .on('postgres_changes', { event: '*', schema: 'public', table: 'accounts' }, () => {
        void refresh();
      })
      .on('postgres_changes', { event: '*', schema: 'public', table: 'goals' }, () => {
        void refresh();
      })
      .on('postgres_changes', { event: '*', schema: 'public', table: 'subscriptions' }, () => {
        void refresh();
      })
      .on('postgres_changes', { event: '*', schema: 'public', table: 'settings' }, () => {
        void refresh();
      })
      .subscribe();

    return () => {
      void supabase.removeChannel(channel);
    };
  }, [isDemoMode, refresh, user]);
}
