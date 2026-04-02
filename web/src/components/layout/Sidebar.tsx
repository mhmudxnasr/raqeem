import { BarChart3, Flag, LayoutDashboard, ReceiptText, Settings, Target, WalletCards } from 'lucide-react';
import { NavLink } from 'react-router-dom';

import { Badge } from '../ui/Badge';
import { cx } from '../../lib/cx';

const navItems = [
  { to: '/', label: 'Home', icon: LayoutDashboard, end: true },
  { to: '/transactions', label: 'Transactions', icon: ReceiptText },
  { to: '/accounts', label: 'Accounts', icon: WalletCards },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 },
  { to: '/budgets', label: 'Budgets', icon: Target },
  { to: '/goals', label: 'Goals', icon: Flag },
];

interface SidebarProps {
  isDemoMode: boolean;
}

export function Sidebar({ isDemoMode }: SidebarProps) {
  return (
    <aside className="hidden h-screen w-[220px] shrink-0 flex-col border-r border-white/5 bg-surface px-4 py-6 lg:flex">
      <div className="space-y-3 px-2">
        <div className="flex items-center gap-3">
          <img alt="Raqeem logo" className="h-10 w-10 rounded-xl border border-white/5 bg-base p-2" src="/logo.svg" />
          <div>
            <p className="text-base font-semibold text-[#F0F0F0]">Raqeem</p>
            <p className="text-sm text-[#5A5A5A]">رقيم</p>
          </div>
        </div>
        <Badge tone={isDemoMode ? 'warning' : 'accent'}>{isDemoMode ? 'Demo mode' : 'Live sync'}</Badge>
      </div>

      <nav className="mt-8 flex-1 space-y-1">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            className={({ isActive }) =>
              cx(
                'group relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-[#A0A0A0] transition-colors hover:bg-subtle hover:text-[#F0F0F0]',
                isActive && 'bg-subtle text-[#F0F0F0]',
              )
            }
            end={Boolean(item.end)}
            to={item.to}
          >
            {({ isActive }) => (
              <>
                <span className={cx('absolute inset-y-2 left-0 w-1 rounded-r-full', isActive ? 'bg-purple-500' : 'bg-transparent')} />
                <item.icon className={cx('h-4 w-4', isActive ? 'text-purple-300' : 'text-[#5A5A5A]')} />
                <span>{item.label}</span>
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <NavLink
        className={({ isActive }) =>
          cx(
            'mt-3 flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-[#A0A0A0] transition-colors hover:bg-subtle hover:text-[#F0F0F0]',
            isActive && 'bg-subtle text-[#F0F0F0]',
          )
        }
        to="/settings"
      >
        <Settings className="h-4 w-4 text-[#5A5A5A]" />
        <span>Settings</span>
      </NavLink>
    </aside>
  );
}
