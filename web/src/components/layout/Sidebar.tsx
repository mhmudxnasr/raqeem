import { BarChart3, Flag, LayoutDashboard, ReceiptText, Settings, Target, WalletCards } from 'lucide-react';
import { NavLink, useLocation } from 'react-router-dom';

import { cx } from '../../lib/cx';

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
    <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col border-r border-white/5 bg-void/50 backdrop-blur-md transition-all duration-300 lg:flex">
      <div className="flex h-full flex-col p-6">
        <div className="mb-10 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 shadow-lg shadow-purple-500/20">
            <LayoutDashboard className="h-6 w-6 text-white" />
          </div>
          <div>
            <span className="text-lg font-bold tracking-tight text-white">Raqeem</span>
            <div className="flex items-center gap-1.5 leading-none">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]"></span>
              <span className="text-[10px] font-medium uppercase tracking-widest text-[#A0A0A0]">v0.0.1</span>
            </div>
          </div>
        </div>

        <nav className="flex-1 space-y-2">
          {navItems.map((item) => {
            const isActive = location.pathname === item.to;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={Boolean(item.end)}
                className={cx(
                  'group flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all duration-200',
                  isActive 
                    ? "bg-white/10 text-white shadow-[0_0_20px_rgba(255,255,255,0.05)]" 
                    : "text-[#A0A0A0] hover:bg-white/5 hover:text-white"
                )}
              >
                <item.icon className={cx('h-5 w-5 transition-transform duration-200 group-hover:scale-110', isActive ? "text-purple-400" : "")} />
                {item.label}
              </NavLink>
            );
          })}
        </nav>

        <div className="mt-auto border-t border-white/5 pt-6">
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              cx(
                'flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all',
                isActive 
                  ? "bg-white/10 text-white" 
                  : "text-[#A0A0A0] hover:bg-white/5 hover:text-white"
              )
            }
          >
            <Settings className="h-5 w-5" />
            Settings
          </NavLink>
        </div>
      </div>
    </aside>
  );
}
