import { create } from 'zustand';

import { getAvailableMonths, getCurrentMonthKey } from '../lib/analytics';
import {
  createTransaction,
  createTransfer,
  fetchFinanceSnapshot,
  softDeleteTransaction,
  updateSettings,
  updateTransaction,
} from '../lib/repository';
import type { FinanceSnapshot, NewTransactionInput, NewTransferInput, Settings } from '../types';

interface FinanceState extends FinanceSnapshot {
  selectedMonth: string;
  isLoading: boolean;
  isSaving: boolean;
  error: string | null;
  bootstrap: () => Promise<void>;
  refresh: () => Promise<void>;
  setSelectedMonth: (month: string) => void;
  addTransaction: (input: NewTransactionInput) => Promise<void>;
  updateTransaction: (input: NewTransactionInput & { id: string }) => Promise<void>;
  deleteTransaction: (id: string) => Promise<void>;
  addTransfer: (input: NewTransferInput) => Promise<void>;
  saveSettings: (changes: Partial<Settings>) => Promise<void>;
  clearError: () => void;
}

const initialState: FinanceSnapshot = {
  accounts: [],
  categories: [],
  transactions: [],
  transfers: [],
  goals: [],
  subscriptions: [],
  settings: {
    usdToEgpRate: 52,
    defaultAccountId: null,
    analyticsCurrency: 'USD',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
};

export const useFinanceStore = create<FinanceState>((set, get) => ({
  ...initialState,
  selectedMonth: getCurrentMonthKey(),
  isLoading: true,
  isSaving: false,
  error: null,
  bootstrap: async () => {
    set({ isLoading: true, error: null });

    try {
      const snapshot = await fetchFinanceSnapshot();
      const availableMonths = getAvailableMonths(snapshot);
      const fallbackMonth = availableMonths[0] ?? getCurrentMonthKey();

      set({
        ...snapshot,
        selectedMonth: availableMonths.includes(get().selectedMonth) ? get().selectedMonth : fallbackMonth,
        isLoading: false,
        error: null,
      });
    } catch (error) {
      set({
        isLoading: false,
        error: error instanceof Error ? error.message : 'Failed to load the app data.',
      });
    }
  },
  refresh: async () => {
    await get().bootstrap();
  },
  setSelectedMonth: (month) => {
    set({ selectedMonth: month });
  },
  addTransaction: async (input) => {
    set({ isSaving: true, error: null });
    try {
      await createTransaction(input);
      await get().bootstrap();
      set({ isSaving: false });
    } catch (error) {
      set({ isSaving: false, error: error instanceof Error ? error.message : 'Failed to save the transaction.' });
      throw error;
    }
  },
  updateTransaction: async (input) => {
    set({ isSaving: true, error: null });
    try {
      await updateTransaction(input);
      await get().bootstrap();
      set({ isSaving: false });
    } catch (error) {
      set({ isSaving: false, error: error instanceof Error ? error.message : 'Failed to update the transaction.' });
      throw error;
    }
  },
  deleteTransaction: async (id) => {
    set({ isSaving: true, error: null });
    try {
      await softDeleteTransaction(id);
      await get().bootstrap();
      set({ isSaving: false });
    } catch (error) {
      set({ isSaving: false, error: error instanceof Error ? error.message : 'Failed to delete the transaction.' });
      throw error;
    }
  },
  addTransfer: async (input) => {
    set({ isSaving: true, error: null });
    try {
      await createTransfer(input);
      await get().bootstrap();
      set({ isSaving: false });
    } catch (error) {
      set({ isSaving: false, error: error instanceof Error ? error.message : 'Failed to save the transfer.' });
      throw error;
    }
  },
  saveSettings: async (changes) => {
    set({ isSaving: true, error: null });
    try {
      await updateSettings(changes);
      await get().bootstrap();
      set({ isSaving: false });
    } catch (error) {
      set({ isSaving: false, error: error instanceof Error ? error.message : 'Failed to update settings.' });
      throw error;
    }
  },
  clearError: () => {
    set({ error: null });
  },
}));
