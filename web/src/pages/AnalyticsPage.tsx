import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

import { InsightPanel } from '../components/features/InsightPanel';
import { BudgetProgress } from '../components/features/BudgetProgress';
import { PageHeader } from '../components/layout/PageHeader';
import { Card } from '../components/ui/Card';
import { Select } from '../components/ui/Select';
import { getAvailableMonths, getBudgetSummaries, getIncomeExpenseSeries, getSpendingByCategory } from '../lib/analytics';
import { formatMonthLabel } from '../lib/format';
import { useFinanceStore } from '../store/useFinanceStore';

const tooltipStyle = {
  backgroundColor: 'var(--bg-overlay)',
  border: '1px solid rgba(255,255,255,0.09)',
  borderRadius: '12px',
  color: 'var(--text-primary)',
};

export function AnalyticsPage() {
  const accounts = useFinanceStore((state) => state.accounts);
  const categories = useFinanceStore((state) => state.categories);
  const transactions = useFinanceStore((state) => state.transactions);
  const transfers = useFinanceStore((state) => state.transfers);
  const goals = useFinanceStore((state) => state.goals);
  const subscriptions = useFinanceStore((state) => state.subscriptions);
  const settings = useFinanceStore((state) => state.settings);
  const selectedMonth = useFinanceStore((state) => state.selectedMonth);
  const setSelectedMonth = useFinanceStore((state) => state.setSelectedMonth);

  const snapshot = { accounts, categories, transactions, transfers, goals, subscriptions, settings };
  const months = getAvailableMonths(snapshot).map((month) => ({ value: month, label: formatMonthLabel(month) }));
  const spendingByCategory = getSpendingByCategory(snapshot, selectedMonth);
  const budgets = getBudgetSummaries(snapshot, selectedMonth).slice(0, 4);
  const series = getIncomeExpenseSeries(snapshot, 6);

  return (
    <>
      <PageHeader
        eyebrow="Analytics"
        title="Spending patterns and AI insight"
        description="Charts stay grounded in your ledger, with Wallet activity normalized to USD."
        actions={<Select label="" onChange={(event) => setSelectedMonth(event.target.value)} options={months} value={selectedMonth} />}
      />

      <div className="grid gap-6 2xl:grid-cols-[minmax(0,1.1fr)_minmax(0,1.1fr)_340px]">
        <Card className="space-y-4">
          <div>
            <p className="section-label">Spending by category</p>
            <h3 className="mt-2 text-lg font-semibold text-[#F0F0F0]">Current month mix</h3>
          </div>
          <div className="h-[280px]">
            <ResponsiveContainer height="100%" width="100%">
              <PieChart>
                <Pie data={spendingByCategory} dataKey="amountCents" innerRadius={72} outerRadius={104} paddingAngle={2}>
                  {spendingByCategory.map((entry) => (
                    <Cell fill={entry.color} key={entry.categoryId} />
                  ))}
                </Pie>
                <Tooltip contentStyle={tooltipStyle} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="space-y-3">
            {spendingByCategory.map((entry) => (
              <div key={entry.categoryId} className="flex items-center justify-between gap-4 text-sm text-[#A0A0A0]">
                <span className="flex items-center gap-3">
                  <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: entry.color }} />
                  {entry.name}
                </span>
                <span className="font-mono text-[#F0F0F0]">${(entry.amountCents / 100).toFixed(2)}</span>
              </div>
            ))}
          </div>
        </Card>

        <div className="space-y-6">
          <Card className="space-y-4">
            <div>
              <p className="section-label">Income vs expenses</p>
              <h3 className="mt-2 text-lg font-semibold text-[#F0F0F0]">Last six months</h3>
            </div>
            <div className="h-[240px]">
              <ResponsiveContainer height="100%" width="100%">
                <BarChart data={series}>
                  <CartesianGrid stroke="rgba(255,255,255,0.05)" strokeDasharray="3 3" />
                  <XAxis dataKey="label" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                  <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                  <Tooltip contentStyle={tooltipStyle} />
                  <Bar dataKey="incomeCents" fill="var(--positive)" radius={[6, 6, 0, 0]} />
                  <Bar dataKey="expenseCents" fill="var(--negative)" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>

          <Card className="space-y-4">
            <div>
              <p className="section-label">Net worth over time</p>
              <h3 className="mt-2 text-lg font-semibold text-[#F0F0F0]">Six month trajectory</h3>
            </div>
            <div className="h-[220px]">
              <ResponsiveContainer height="100%" width="100%">
                <LineChart data={series}>
                  <CartesianGrid stroke="rgba(255,255,255,0.05)" strokeDasharray="3 3" />
                  <XAxis dataKey="label" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                  <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                  <Tooltip contentStyle={tooltipStyle} />
                  <Line dataKey="netWorthCents" dot={false} stroke="var(--purple-400)" strokeWidth={2.5} type="monotone" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </Card>

          <Card className="space-y-4">
            <p className="section-label">Budget hotspots</p>
            <div className="space-y-3">
              {budgets.map((summary) => (
                <BudgetProgress key={summary.categoryId} summary={summary} />
              ))}
            </div>
          </Card>
        </div>

        <InsightPanel month={selectedMonth} snapshot={snapshot} />
      </div>
    </>
  );
}
