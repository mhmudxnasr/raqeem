import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowRightLeft, BanknoteArrowDown, Landmark, Plus, X } from 'lucide-react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { parseAmountInput } from '../../lib/format';
import { useFinanceStore } from '../../store/useFinanceStore';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import type { Account, QuickAddMode } from '../../types';

const quickAddSchema = z
  .object({
    mode: z.enum(['expense', 'income', 'transfer']),
    amount: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Enter a valid amount'),
    accountId: z.string(),
    categoryId: z.string(),
    fromAccountId: z.string(),
    toAccountId: z.string(),
    goalId: z.string(),
    date: z.string().min(1, 'Choose a date'),
    note: z.string().max(200, 'Keep notes under 200 characters'),
  })
  .superRefine((values, context) => {
    if (values.mode === 'transfer') {
      if (!values.fromAccountId) {
        context.addIssue({ code: z.ZodIssueCode.custom, path: ['fromAccountId'], message: 'Choose a source account' });
      }

      if (!values.toAccountId) {
        context.addIssue({ code: z.ZodIssueCode.custom, path: ['toAccountId'], message: 'Choose a destination account' });
      }

      if (values.fromAccountId && values.toAccountId && values.fromAccountId === values.toAccountId) {
        context.addIssue({ code: z.ZodIssueCode.custom, path: ['toAccountId'], message: 'Accounts must be different' });
      }
    } else {
      if (!values.accountId) {
        context.addIssue({ code: z.ZodIssueCode.custom, path: ['accountId'], message: 'Choose an account' });
      }

      if (!values.categoryId) {
        context.addIssue({ code: z.ZodIssueCode.custom, path: ['categoryId'], message: 'Choose a category' });
      }
    }
  });

type QuickAddFormValues = z.infer<typeof quickAddSchema>;

interface QuickAddDialogProps {
  isOpen: boolean;
  initialMode: QuickAddMode;
  defaultGoalId?: string | null;
  onClose: () => void;
}

function getDefaultValues(accounts: Account[], defaultAccountId: string | null, initialMode: QuickAddMode, goalId: string | null) {
  const defaultAccount = accounts.find((account) => account.id === defaultAccountId) ?? accounts[0];
  const wallet = accounts.find((account) => account.name === 'Wallet') ?? accounts[0];
  const today = new Date().toISOString().slice(0, 10);

  return {
    mode: initialMode,
    amount: '',
    accountId: defaultAccount?.id ?? '',
    categoryId: '',
    fromAccountId: defaultAccount?.id ?? '',
    toAccountId: wallet?.id ?? '',
    goalId: goalId ?? '',
    date: today,
    note: '',
  } satisfies QuickAddFormValues;
}

export function QuickAddDialog({ isOpen, initialMode, defaultGoalId = null, onClose }: QuickAddDialogProps) {
  const accounts = useFinanceStore((state) => state.accounts);
  const categories = useFinanceStore((state) => state.categories);
  const goals = useFinanceStore((state) => state.goals);
  const settings = useFinanceStore((state) => state.settings);
  const isSaving = useFinanceStore((state) => state.isSaving);
  const addTransaction = useFinanceStore((state) => state.addTransaction);
  const addTransfer = useFinanceStore((state) => state.addTransfer);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    setValue,
    formState: { errors },
  } = useForm<QuickAddFormValues>({
    resolver: zodResolver(quickAddSchema),
    defaultValues: getDefaultValues(accounts, settings.defaultAccountId, initialMode, defaultGoalId),
  });

  useEffect(() => {
    if (isOpen) {
      reset(getDefaultValues(accounts, settings.defaultAccountId, initialMode, defaultGoalId));
    }
  }, [accounts, defaultGoalId, initialMode, isOpen, reset, settings.defaultAccountId]);

  const mode = watch('mode');
  const selectedAccount = accounts.find((account) => account.id === watch('accountId')) ?? null;
  const fromAccount = accounts.find((account) => account.id === watch('fromAccountId')) ?? null;
  const toAccount = accounts.find((account) => account.id === watch('toAccountId')) ?? null;
  const exchangeRate = settings.usdToEgpRate;
  const parsedAmount = parseAmountInput(watch('amount') ?? '');

  const categoryOptions = categories
    .filter((category) => category.type === (mode === 'income' ? 'income' : 'expense') && category.deletedAt === null)
    .map((category) => ({
      value: category.id,
      label: category.name,
    }));

  const accountOptions = accounts
    .filter((account) => account.deletedAt === null)
    .map((account) => ({
      value: account.id,
      label: `${account.name} · ${account.currency}`,
    }));

  const goalOptions = [
    { value: '', label: 'No goal selected' },
    ...goals
      .filter((goal) => goal.deletedAt === null)
      .map((goal) => ({
        value: goal.id,
        label: goal.name,
      })),
  ];

  const convertedAmount =
    parsedAmount && fromAccount && toAccount
      ? fromAccount.currency === toAccount.currency
        ? parsedAmount
        : fromAccount.currency === 'USD'
          ? Math.round(parsedAmount * exchangeRate)
          : Math.round(parsedAmount / exchangeRate)
      : 0;

  const onSubmit = async (values: QuickAddFormValues) => {
    const amountCents = parseAmountInput(values.amount);
    if (!amountCents) {
      return;
    }

    if (values.mode === 'transfer') {
      if (!fromAccount || !toAccount) {
        return;
      }

      await addTransfer({
        fromAccountId: values.fromAccountId,
        toAccountId: values.toAccountId,
        fromAmountCents: amountCents,
        toAmountCents: convertedAmount,
        fromCurrency: fromAccount.currency,
        toCurrency: toAccount.currency,
        exchangeRate: fromAccount.currency === toAccount.currency ? 1 : exchangeRate,
        goalId: values.goalId || null,
        note: values.note || null,
        date: values.date,
      });
    } else if (selectedAccount) {
      await addTransaction({
        type: values.mode,
        amountCents,
        accountId: values.accountId,
        categoryId: values.categoryId || null,
        currency: selectedAccount.currency,
        note: values.note || null,
        date: values.date,
      });
    }

    onClose();
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 px-4 py-8">
      <div className="w-full max-w-2xl rounded-2xl border border-white/10 bg-elevated">
        <div className="flex items-center justify-between border-b border-white/5 px-6 py-5">
          <div>
            <p className="section-label">Quick add</p>
            <h2 className="mt-1 text-xl font-semibold text-[#F0F0F0]">Log money movement fast</h2>
          </div>
          <button className="rounded-lg border border-white/10 p-2 text-[#A0A0A0] transition-colors hover:bg-subtle hover:text-white" onClick={onClose} type="button">
            <X className="h-4 w-4" />
          </button>
        </div>

        <form className="space-y-6 px-6 py-6" onSubmit={handleSubmit(onSubmit)}>
          <div className="grid grid-cols-3 gap-2">
            {[
              { modeValue: 'expense' as const, label: 'Expense', icon: BanknoteArrowDown },
              { modeValue: 'income' as const, label: 'Income', icon: Plus },
              { modeValue: 'transfer' as const, label: 'Transfer', icon: ArrowRightLeft },
            ].map((item) => (
              <button
                key={item.modeValue}
                className={mode === item.modeValue ? 'btn-primary gap-2' : 'btn-secondary gap-2'}
                onClick={() => setValue('mode', item.modeValue)}
                type="button"
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </button>
            ))}
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <Input
              error={errors.amount?.message}
              label={mode === 'transfer' ? `Amount · ${fromAccount?.currency ?? 'USD'}` : `Amount · ${selectedAccount?.currency ?? 'USD'}`}
              placeholder="0.00"
              {...register('amount')}
            />
            <Input error={errors.date?.message} label="Date" type="date" {...register('date')} />
          </div>

          {mode === 'transfer' ? (
            <div className="grid gap-4 md:grid-cols-2">
              <Select error={errors.fromAccountId?.message} label="From" options={accountOptions} {...register('fromAccountId')} />
              <Select error={errors.toAccountId?.message} label="To" options={accountOptions} {...register('toAccountId')} />
            </div>
          ) : (
            <div className="grid gap-4 md:grid-cols-2">
              <Select error={errors.accountId?.message} label="Account" options={accountOptions} {...register('accountId')} />
              <Select
                error={errors.categoryId?.message}
                label="Category"
                options={[{ value: '', label: 'Select a category' }, ...categoryOptions]}
                {...register('categoryId')}
              />
            </div>
          )}

          {mode === 'transfer' && fromAccount && toAccount && fromAccount.currency !== toAccount.currency && parsedAmount ? (
            <div className="rounded-xl border border-purple-400/15 bg-purple-500/5 p-4">
              <p className="section-label">Conversion</p>
              <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-[#A0A0A0]">
                <span>1 USD = {exchangeRate.toFixed(2)} EGP</span>
                <span>•</span>
                <span>You'll receive {toAccount.currency} {convertedAmount ? (convertedAmount / 100).toFixed(2) : '0.00'}</span>
              </div>
            </div>
          ) : null}

          {mode === 'transfer' ? (
            <Select error={errors.goalId?.message} label="Fund a goal" options={goalOptions} {...register('goalId')} />
          ) : null}

          <label className="flex flex-col gap-2">
            <span className="section-label">Note</span>
            <textarea className="textarea" placeholder="Optional note..." {...register('note')} />
            {errors.note?.message ? <span className="text-xs text-negative">{errors.note.message}</span> : null}
          </label>

          <div className="flex items-center justify-between gap-3 border-t border-white/5 pt-5">
            <div className="flex items-center gap-2 text-sm text-[#A0A0A0]">
              <Landmark className="h-4 w-4 text-[#5A5A5A]" />
              Uses your current settings and exchange rate.
            </div>
            <div className="flex gap-3">
              <Button onClick={onClose} type="button" variant="secondary">
                Cancel
              </Button>
              <Button disabled={isSaving} type="submit">
                {isSaving ? 'Saving...' : mode === 'transfer' ? 'Confirm transfer' : `Add ${mode}`}
              </Button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
