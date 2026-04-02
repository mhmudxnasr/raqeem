import type { ReactNode } from 'react';

interface PageHeaderProps {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}

export function PageHeader({ eyebrow, title, description, actions }: PageHeaderProps) {
  return (
    <div className="mb-10 flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
      <div className="space-y-4">
        {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
        <div className="space-y-2">
          <h1 className="font-serif text-3xl font-bold tracking-tight text-white md:text-4xl">{title}</h1>
          {description ? <p className="max-w-2xl text-base text-[#A0A0A0]">{description}</p> : null}
        </div>
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-4">{actions}</div> : null}
    </div>
  );
}
