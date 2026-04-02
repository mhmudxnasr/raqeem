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
  backgroundColor: '#2C2C2C',
  border: '1px solid rgba(255, 255, 255, 0.09)',
  borderRadius: '8px',
  color: '#F0F0F0',
  padding: '8px 12px',
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
        <Card className="flex flex-col h-fit sticky top-24">
          <div className="mb-6">
            <p className="section-label">Composition</p>
            <h3 className="mt-1 text-base font-semibold tracking-tight text-white">Spending by Category</h3>
          </div>
          <div className="h-[280px] w-full">
            <ResponsiveContainer height="100%" width="100%">
              <PieChart>
                <Pie 
                  data={spendingByCategory} 
                  dataKey="amountCents" 
                  innerRadius={72} 
                  outerRadius={104} 
                  paddingAngle={4}
                  stroke="none"
                >
                  {spendingByCategory.map((entry) => (
                    <Cell fill={entry.color} key={entry.categoryId} className="transition-opacity hover:opacity-80 cursor-pointer" />
                  ))}
                </Pie>
                <Tooltip contentStyle={tooltipStyle} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-6 space-y-1">
            {spendingByCategory.map((entry) => (
              <div key={entry.categoryId} className="flex items-center justify-between gap-4 rounded-lg px-2 py-2 text-sm text-[#A0A0A0] transition-colors hover:bg-elevated">
                <span className="flex items-center gap-3 font-medium">
                  <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: entry.color }} />
                  {entry.name}
                </span>
                <span className="font-mono text-white/90 font-medium">${(entry.amountCents / 100).toFixed(2)}</span>
              </div>
            ))}
          </div>
        </Card>

        <div className="space-y-6">
          <Card>
            <div className="mb-6">
              <p className="section-label">Comparison</p>
              <h3 className="mt-1 text-base font-semibold tracking-tight text-white">Income vs Expenses</h3>
            </div>
            <div className="h-[240px]">
              <ResponsiveContainer height="100%" width="100%">
                <BarChart data={series}>
                  <CartesianGrid stroke="rgba(255,255,255,0.03)" strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: '#5A5A5A', fontSize: 10, fontWeight: 600 }} />
                  <YAxis axisLine={false} tickLine={false} tick={{ fill: '#5A5A5A', fontSize: 10, fontWeight: 600 }} />
                  <Tooltip cursor={{ fill: 'rgba(255,255,255,0.03)' }} contentStyle={tooltipStyle} />
                  <Bar dataKey="incomeCents" fill="#10B981" radius={[4, 4, 0, 0]} barSize={24} />
                  <Bar dataKey="expenseCents" fill="#F87171" radius={[4, 4, 0, 0]} barSize={24} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>

          <Card>
            <div className="mb-6">
              <p className="section-label">Growth</p>
              <h3 className="mt-1 text-base font-semibold tracking-tight text-white">Net Worth Trend</h3>
            </div>
            <div className="h-[220px]">
              <ResponsiveContainer height="100%" width="100%">
                <LineChart data={series}>
                  <CartesianGrid stroke="rgba(255,255,255,0.03)" strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: '#5A5A5A', fontSize: 10, fontWeight: 600 }} />
                  <YAxis axisLine={false} tickLine={false} tick={{ fill: '#5A5A5A', fontSize: 10, fontWeight: 600 }} />
                  <Tooltip contentStyle={tooltipStyle} />
                  <Line 
                    dataKey="netWorthCents" 
                    dot={{ r: 3, fill: '#8B5CF6', strokeWidth: 0 }} 
                    activeDot={{ r: 5, fill: '#8B5CF6', strokeWidth: 0 }}
                    stroke="#8B5CF6" 
                    strokeWidth={2} 
                    type="monotone" 
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </Card>

          <Card>
            <p className="section-label mb-4">Budget Criticality</p>
            <div className="space-y-4">
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
