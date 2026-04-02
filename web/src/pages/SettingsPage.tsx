import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { PageHeader } from '../components/layout/PageHeader';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { formatAmount } from '../lib/format';
import { useAuthStore } from '../store/useAuthStore';
import { useFinanceStore } from '../store/useFinanceStore';

const settingsSchema = z.object({
  usdToEgpRate: z.string().regex(/^\d+(\.\d{1,4})?$/, 'Enter a valid exchange rate'),
  defaultAccountId: z.string().min(1, 'Choose a default account'),
});

type SettingsFormValues = z.infer<typeof settingsSchema>;

export function SettingsPage() {
  const user = useAuthStore((state) => state.user);
  const signOut = useAuthStore((state) => state.signOut);
  const accounts = useFinanceStore((state) => state.accounts);
  const categories = useFinanceStore((state) => state.categories);
  const settings = useFinanceStore((state) => state.settings);
  const saveSettings = useFinanceStore((state) => state.saveSettings);
  const isSaving = useFinanceStore((state) => state.isSaving);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<SettingsFormValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: {
      usdToEgpRate: settings.usdToEgpRate.toFixed(2),
      defaultAccountId: settings.defaultAccountId ?? '',
    },
  });

  useEffect(() => {
    reset({
      usdToEgpRate: settings.usdToEgpRate.toFixed(2),
      defaultAccountId: settings.defaultAccountId ?? '',
    });
  }, [reset, settings.defaultAccountId, settings.usdToEgpRate]);

  const accountOptions = accounts
    .filter((account) => account.deletedAt === null)
    .map((account) => ({ value: account.id, label: `${account.name} · ${account.currency}` }));

  const onSubmit = async (values: SettingsFormValues) => {
    await saveSettings({
      usdToEgpRate: Number(values.usdToEgpRate),
      defaultAccountId: values.defaultAccountId,
    });
  };

  return (
    <>
      <PageHeader eyebrow="Settings" title="Core app preferences" description="Keep the exchange rate and quick-add defaults aligned with how you actually manage money." />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          <Card className="glass-card border-white/5 space-y-5">
            <div>
              <p className="eyebrow">Currency</p>
              <h3 className="mt-2 text-xl font-bold tracking-tight text-white">USD / EGP conversion</h3>
            </div>
            <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
              <Input error={errors.usdToEgpRate?.message} label="1 USD equals" {...register('usdToEgpRate')} className="bg-white/5 border-white/10" />
              <Select error={errors.defaultAccountId?.message} label="Default account" options={accountOptions} {...register('defaultAccountId')} className="bg-white/5 border-white/10" />
              <Button disabled={isSaving} type="submit">
                {isSaving ? 'Saving...' : 'Save settings'}
              </Button>
            </form>
          </Card>

          <Card className="glass-card border-white/5 space-y-4">
            <p className="eyebrow">Accounts</p>
            <div className="divide-y divide-white/10 pt-2">
              {accounts
                .filter((account) => account.deletedAt === null)
                .map((account) => (
                  <div key={account.id} className="flex items-center justify-between gap-4 py-4">
                    <div>
                      <p className="text-sm font-semibold text-white">{account.name}</p>
                      <p className="text-[10px] uppercase tracking-wider text-[#5A5A5A]">{account.type}</p>
                    </div>
                    <span className="font-serif text-base font-medium text-white/90">{formatAmount(account.balanceCents, account.currency)}</span>
                  </div>
                ))}
            </div>
          </Card>

          <Card className="glass-card border-white/5 space-y-4">
            <p className="eyebrow">Ledger Categories</p>
            <div className="grid gap-3 md:grid-cols-2 pt-2">
              {categories
                .filter((category) => category.deletedAt === null)
                .map((category) => (
                  <div key={category.id} className="rounded-xl border border-white/5 bg-white/[0.02] p-4 transition-colors hover:bg-white/[0.04]">
                    <div className="flex items-center justify-between gap-4">
                      <p className="text-sm font-semibold text-white">{category.name}</p>
                      <Badge tone={category.type === 'income' ? 'positive' : 'accent'}>{category.type}</Badge>
                    </div>
                    <p className="mt-2 text-[10px] uppercase tracking-wider text-[#5A5A5A]">
                      {category.budgetCents ? `Allocated ${formatAmount(category.budgetCents, 'USD')}` : 'No monthly budget'}
                    </p>
                  </div>
                ))}
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="glass-card border-white/5 space-y-4">
            <p className="eyebrow">Session</p>
            <div className="space-y-3 pt-2">
              <div>
                <p className="text-sm font-bold tracking-tight text-white">{user?.email}</p>
                <div className="mt-1">
                  <Badge tone="accent">Supabase Authenticated</Badge>
                </div>
              </div>
            </div>
            <Button className="w-full" onClick={() => void signOut()} variant="secondary">
              Sign out
            </Button>
          </Card>
        </div>
      </div>
    </>
  );
}
