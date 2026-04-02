import { Plus } from 'lucide-react';
import { useState } from 'react';
import { Outlet, useOutletContext } from 'react-router-dom';

import { QuickAddDialog } from '../features/QuickAddDialog';
import { Sidebar } from './Sidebar';
import { Button } from '../ui/Button';
import { useFinanceStore } from '../../store/useFinanceStore';
import type { QuickAddMode } from '../../types';

export interface AppShellContextValue {
  openQuickAdd: (mode?: QuickAddMode, goalId?: string | null) => void;
}

export function useAppShellContext() {
  return useOutletContext<AppShellContextValue>();
}

export function AppShell() {
  const error = useFinanceStore((state) => state.error);
  const clearError = useFinanceStore((state) => state.clearError);
  const [dialogState, setDialogState] = useState<{ isOpen: boolean; mode: QuickAddMode; goalId: string | null }>({
    isOpen: false,
    mode: 'expense',
    goalId: null,
  });

  const openQuickAdd = (mode: QuickAddMode = 'expense', goalId: string | null = null) => {
    setDialogState({
      isOpen: true,
      mode,
      goalId,
    });
  };

  return (
    <div className="flex min-h-screen bg-void">
      <Sidebar />
      <div className="flex min-h-screen flex-1 flex-col">
        <header className="sticky top-0 z-40 border-b border-white/5 bg-base px-4 py-4 md:px-8">
          <div className="mx-auto flex w-full max-w-[1240px] items-center justify-between gap-4">
            <div>
              <p className="section-label">Raqeem</p>
              <h1 className="text-lg font-semibold tracking-tight text-white">Workspace</h1>
            </div>
            <Button className="h-10 gap-2 px-5" onClick={() => openQuickAdd('expense')}>
              <Plus className="h-4 w-4 stroke-[3]" />
              <span className="font-semibold">Add</span>
            </Button>
          </div>
        </header>

        {error ? (
          <div className="border-b border-red-500/20 bg-red-500/5 px-4 py-3 text-sm text-negative md:px-8">
            <div className="mx-auto flex max-w-[1200px] items-center justify-between gap-4">
              <span>{error}</span>
              <button className="text-xs font-medium uppercase tracking-widest text-[#A0A0A0] hover:text-white" onClick={clearError} type="button">
                Dismiss
              </button>
            </div>
          </div>
        ) : null}

        <main className="flex-1 overflow-y-auto px-4 py-8 md:px-8">
          <div className="mx-auto flex w-full max-w-[1200px] flex-col gap-8 animate-fadeSlide">
            <Outlet context={{ openQuickAdd }} />
          </div>
        </main>
      </div>

      <QuickAddDialog
        defaultGoalId={dialogState.goalId}
        initialMode={dialogState.mode}
        isOpen={dialogState.isOpen}
        onClose={() => setDialogState((current) => ({ ...current, isOpen: false }))}
      />
    </div>
  );
}
