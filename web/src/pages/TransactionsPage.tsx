import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowRightLeft, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { useAppShellContext } from '../components/layout/AppShell';
import { PageHeader } from '../components/layout/PageHeader';
import { TransactionTable } from '../components/features/TransactionTable';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { buildLedgerEntries, filterLedgerEntries } from '../lib/analytics';
import { formatAmount, parseAmountInput } from '../lib/format';
import { useFinanceStore } from '../store/useFinanceStore';
import type { FinanceFilters, LedgerEntry } from '../types';

const editTransactionSchema = z.object({
  amount: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Enter a valid amount'),
  accountId: z.string().min(1, 'Choose an account'),
  categoryId: z.string().min(1, 'Choose a category'),
  date: z.string().min(1, 'Choose a date'),
  note: z.string().max(200, 'Keep notes under 200 characters'),
});

type EditTransactionValues = z.infer<typeof editTransactionSchema>;

function TransactionDetailPanel({
  entry,
  onDelete,
}: {
  entry: LedgerEntry | null;
  onDelete: (id: string) => Promise<void>;
}) {
  const accounts = useFinanceStore((state) => state.accounts);
  const categories = useFinanceStore((state) => state.categories);
  const updateTransaction = useFinanceStore((state) => state.updateTransaction);
  const isSaving = useFinanceStore((state) => state.isSaving);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditTransactionValues>({
    resolver: zodResolver(editTransactionSchema),
    defaultValues: {
      amount: '',
      accountId: '',
      categoryId: '',
      date: '',
      note: '',
    },
  });

  useEffect(() => {
    if (entry?.kind === 'transaction') {
      reset({
        amount: (entry.raw.amountCents / 100).toFixed(2),
        accountId: entry.raw.accountId,
        categoryId: entry.raw.categoryId ?? '',
        date: entry.raw.date,
        note: entry.raw.note ?? '',
      });
    }
  }, [entry, reset]);

  if (!entry) {
    return (
      <Card className="flex h-full items-center justify-center">
        <p className="text-sm text-[#5A5A5A]">Select a ledger row to inspect or edit it.</p>
      </Card>
    );
  }

  if (entry.kind === 'transfer') {
    return (
      <Card className="space-y-5">
        <div className="flex items-center gap-2">
          <ArrowRightLeft className="h-4 w-4 text-purple-300" />
          <div>
            <p className="section-label">Transfer detail</p>
            <h3 className="mt-1 text-lg font-semibold text-[#F0F0F0]">{entry.title}</h3>
          </div>
        </div>
        <div className="space-y-3 text-sm text-[#A0A0A0]">
          <p>Sent {formatAmount(entry.raw.fromAmountCents, entry.raw.fromCurrency)} from {entry.accountName}.</p>
          <p>Received {formatAmount(entry.raw.toAmountCents, entry.raw.toCurrency)} in {entry.counterpartyName}.</p>
          <p>Exchange rate: {entry.raw.exchangeRate.toFixed(2)}</p>
          <p>Note: {entry.note ?? 'No note'}</p>
        </div>
      </Card>
    );
  }

  const categoryOptions = categories
    .filter((category) => category.deletedAt === null && category.type === entry.raw.type)
    .map((category) => ({ value: category.id, label: category.name }));
  const accountOptions = accounts
    .filter((account) => account.deletedAt === null)
    .map((account) => ({ value: account.id, label: `${account.name} · ${account.currency}` }));

  const submit = async (values: EditTransactionValues) => {
    const amountCents = parseAmountInput(values.amount);
    const account = accounts.find((item) => item.id === values.accountId);

    if (!amountCents || !account) {
      return;
    }

    await updateTransaction({
      id: entry.id,
      type: entry.raw.type,
      amountCents,
      accountId: values.accountId,
      categoryId: values.categoryId,
      currency: account.currency,
      note: values.note || null,
      date: values.date,
    });
  };

  return (
    <Card className="glass-card border-white/5 space-y-6">
      <div>
        <p className="eyebrow">Transaction Detail</p>
        <h3 className="mt-2 text-xl font-bold tracking-tight text-white">{entry.title}</h3>
        <p className="mt-2 text-sm leading-relaxed text-[#A0A0A0]">Edit the underlying record directly from the ledger.</p>
      </div>

      <form className="space-y-4" onSubmit={handleSubmit(submit)}>
        <Input error={errors.amount?.message} label="Amount" {...register('amount')} />
        <Select error={errors.accountId?.message} label="Account" options={accountOptions} {...register('accountId')} />
        <Select error={errors.categoryId?.message} label="Category" options={categoryOptions} {...register('categoryId')} />
        <Input error={errors.date?.message} label="Date" type="date" {...register('date')} />
        <label className="flex flex-col gap-2">
          <span className="section-label">Note</span>
          <textarea className="textarea" {...register('note')} />
        </label>

        <div className="flex gap-3">
          <Button disabled={isSaving} type="submit">
            {isSaving ? 'Saving...' : 'Save changes'}
          </Button>
          <Button
            disabled={isSaving}
            onClick={() => {
              if (window.confirm('Delete this transaction?')) {
                void onDelete(entry.id);
              }
            }}
            type="button"
            variant="destructive"
          >
            <Trash2 className="mr-2 h-4 w-4" />
            Delete
          </Button>
        </div>
      </form>
    </Card>
  );
}

export function TransactionsPage() {
  const { openQuickAdd } = useAppShellContext();
  const accounts = useFinanceStore((state) => state.accounts);
  const categories = useFinanceStore((state) => state.categories);
  const transactions = useFinanceStore((state) => state.transactions);
  const transfers = useFinanceStore((state) => state.transfers);
  const goals = useFinanceStore((state) => state.goals);
  const subscriptions = useFinanceStore((state) => state.subscriptions);
  const settings = useFinanceStore((state) => state.settings);
  const deleteTransaction = useFinanceStore((state) => state.deleteTransaction);
  const snapshot = { accounts, categories, transactions, transfers, goals, subscriptions, settings };
  const [filters, setFilters] = useState<FinanceFilters>({
    accountId: '',
    categoryId: '',
    entryType: 'all',
    search: '',
    dateFrom: '',
    dateTo: '',
  });
  const entries = filterLedgerEntries(buildLedgerEntries(snapshot), filters);
  const [selectedId, setSelectedId] = useState<string | null>(entries[0]?.id ?? null);

  useEffect(() => {
    if (!selectedId || !entries.some((entry) => entry.id === selectedId)) {
      setSelectedId(entries[0]?.id ?? null);
    }
  }, [entries, selectedId]);

  const selectedEntry = entries.find((entry) => entry.id === selectedId) ?? null;

  const accountOptions = [{ value: '', label: 'All accounts' }, ...accounts.filter((account) => account.deletedAt === null).map((account) => ({ value: account.id, label: account.name }))];
  const categoryOptions = [{ value: '', label: 'All categories' }, ...categories.filter((category) => category.deletedAt === null && category.type === 'expense').map((category) => ({ value: category.id, label: category.name }))];

  return (
    <>
      <PageHeader
        eyebrow="Transactions"
        title="Ledger and filters"
        description="Search, filter, and edit every income, expense, and transfer in one place."
        actions={<Button onClick={() => openQuickAdd('expense')}>Add entry</Button>}
      />

      <Card className="glass-card grid gap-4 border-white/5 md:grid-cols-2 xl:grid-cols-6 mb-6">
        <Input
          label="Search"
          onChange={(event) => setFilters((current) => ({ ...current, search: event.target.value }))}
          placeholder="Category, note, amount..."
          value={filters.search}
          className="bg-white/5 border-white/10"
        />
        <Select label="Account" onChange={(event) => setFilters((current) => ({ ...current, accountId: event.target.value }))} options={accountOptions} value={filters.accountId} className="bg-white/5 border-white/10" />
        <Select label="Category" onChange={(event) => setFilters((current) => ({ ...current, categoryId: event.target.value }))} options={categoryOptions} value={filters.categoryId} className="bg-white/5 border-white/10" />
        <Select
          label="Type"
          onChange={(event) =>
            setFilters((current) => ({ ...current, entryType: event.target.value as FinanceFilters['entryType'] }))
          }
          options={[
            { value: 'all', label: 'All entries' },
            { value: 'income', label: 'Income' },
            { value: 'expense', label: 'Expense' },
            { value: 'transfer', label: 'Transfer' },
          ]}
          value={filters.entryType}
          className="bg-white/5 border-white/10"
        />
        <Input label="From" onChange={(event) => setFilters((current) => ({ ...current, dateFrom: event.target.value }))} type="date" value={filters.dateFrom} className="bg-white/5 border-white/10" />
        <Input label="To" onChange={(event) => setFilters((current) => ({ ...current, dateTo: event.target.value }))} type="date" value={filters.dateTo} className="bg-white/5 border-white/10" />
      </Card>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.4fr)_360px]">
        <TransactionTable entries={entries} onSelect={setSelectedId} selectedId={selectedId} />
        <TransactionDetailPanel entry={selectedEntry} onDelete={deleteTransaction} />
      </div>
    </>
  );
}
