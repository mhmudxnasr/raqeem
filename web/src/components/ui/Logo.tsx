import { cx } from '../../lib/cx';

interface LogoProps {
  className?: string;
  size?: number | string;
}

export function Logo({ className, size = 40 }: LogoProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 200 200"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={cx('shrink-0', className)}
    >
      <path
        d="M 30.72,60 L 100,20 L 100,110 Z"
        fill="#AD6BFF"
      />
      <path
        d="M 100,20 L 169.28,60 L 100,110 Z"
        fill="#8C3FFF"
      />
      <path
        d="M 169.28,60 L 169.28,140 L 100,110 Z"
        fill="#5B16BA"
      />
      <path
        d="M 169.28,140 L 100,180 L 100,110 Z"
        fill="#2E0666"
      />
      <path
        d="M 100,180 L 30.72,140 L 100,110 Z"
        fill="#470C96"
      />
      <path
        d="M 30.72,140 L 30.72,60 L 100,110 Z"
        fill="#7122D9"
      />
    </svg>
  );
}
