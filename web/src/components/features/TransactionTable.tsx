import { ReceiptText } from 'lucide-react';

import { formatAmount, formatDateLabel } from '../../lib/format';
import { EmptyState } from './EmptyState';
import type { LedgerEntry } from '../../types';

interface TransactionTableProps {
  entries: LedgerEntry[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}

export function TransactionTable({ entries, selectedId, onSelect }: TransactionTableProps) {
  if (entries.length === 0) {
    return (
      <EmptyState
        description="Create an expense, income, or transfer to start filling the ledger."
        icon={ReceiptText}
        title="No ledger entries yet"
      />
    );
  }

  return (
    <div className="overflow-hidden rounded-xl border border-white/5 bg-surface">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-white/5">
          <thead className="bg-elevated/60">
            <tr className="text-left text-[11px] uppercase tracking-[0.08em] text-[#5A5A5A]">
              <th className="px-4 py-3">Date</th>
              <th className="px-4 py-3">Type</th>
              <th className="px-4 py-3">Detail</th>
              <th className="px-4 py-3">Account</th>
              <th className="px-4 py-3 text-right">Amount</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {entries.map((entry) => (
              <tr
                key={entry.id}
                className={selectedId === entry.id ? 'cursor-pointer bg-subtle/80' : 'cursor-pointer transition-colors hover:bg-subtle/50'}
                onClick={() => onSelect(entry.id)}
              >
                <td className="px-4 py-3 text-sm text-[#A0A0A0]">{formatDateLabel(entry.date)}</td>
                <td className="px-4 py-3">
                  <span className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-[11px] uppercase tracking-[0.08em] text-[#A0A0A0]">
                    {entry.type}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="space-y-1">
                    <p className="text-sm font-medium text-[#F0F0F0]">{entry.title}</p>
                    <p className="text-xs text-[#5A5A5A]">{entry.note ?? entry.subtitle}</p>
                  </div>
                </td>
                <td className="px-4 py-3 text-sm text-[#A0A0A0]">{entry.accountName}</td>
                <td className="px-4 py-3 text-right font-mono text-sm">
                  <span className={entry.amountCents >= 0 ? 'text-positive' : 'text-negative'}>
                    {formatAmount(entry.amountCents, entry.currency)}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
