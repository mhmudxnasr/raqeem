import { BarChart3, Flag, LayoutDashboard, ReceiptText, Settings, Target, WalletCards } from 'lucide-react';
import { NavLink, useLocation } from 'react-router-dom';

import { cx } from '../../lib/cx';
import { Logo } from '../ui/Logo';

const navItems = [
  { to: '/', label: 'Home', icon: LayoutDashboard, end: true },
  { to: '/transactions', label: 'Transactions', icon: ReceiptText },
  { to: '/accounts', label: 'Accounts', icon: WalletCards },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 },
  { to: '/budgets', label: 'Budgets', icon: Target },
  { to: '/goals', label: 'Goals', icon: Flag },
];

export function Sidebar() {
  const location = useLocation();

  return (
    <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col border-r border-white/5 bg-surface lg:flex">
      <div className="flex h-full flex-col px-4 py-6">
        <div className="mb-10 flex items-center gap-3 px-3">
          <Logo size={36} />
          <div>
            <span className="text-base font-semibold tracking-tight text-white">Raqeem</span>
            <div className="flex items-center gap-1.5 leading-none">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
              <span className="text-[10px] font-medium uppercase tracking-widest text-[#5A5A5A]">v0.0.1</span>
            </div>
          </div>
        </div>

        <nav className="flex-1 space-y-1">
          {navItems.map((item) => {
            const isActive = location.pathname === item.to;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={Boolean(item.end)}
                className={cx(
                  'group relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors duration-150',
                  isActive
                    ? "bg-elevated text-white"
                    : "text-[#A0A0A0] hover:bg-elevated/50 hover:text-white"
                )}
              >
                {isActive && (
                  <span className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r-full bg-purple-500" />
                )}
                <item.icon className={cx('h-[18px] w-[18px]', isActive ? "text-purple-400" : "")} />
                {item.label}
              </NavLink>
            );
          })}
        </nav>

        <div className="mt-auto border-t border-white/5 pt-4">
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              cx(
                'relative flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors duration-150',
                isActive
                  ? "bg-elevated text-white"
                  : "text-[#A0A0A0] hover:bg-elevated/50 hover:text-white"
              )
            }
          >
            {({ isActive }) => (
              <>
                {isActive && (
                  <span className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r-full bg-purple-500" />
                )}
                <Settings className={cx('h-[18px] w-[18px]', isActive ? 'text-purple-400' : '')} />
                Settings
              </>
            )}
          </NavLink>
        </div>
      </div>
    </aside>
  );
}
