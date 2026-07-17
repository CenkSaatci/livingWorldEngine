import { render } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { SkeletonCard } from './SkeletonCard';

describe('SkeletonCard', () => {
  it('renders with default 3 lines', () => {
    const { container } = render(<SkeletonCard />);
    const bars = container.querySelectorAll('.rounded.bg-bg-elevated\\/60');
    expect(bars.length).toBe(3);
  });

  it('renders with custom line count', () => {
    const { container } = render(<SkeletonCard lines={5} />);
    const bars = container.querySelectorAll('.rounded.bg-bg-elevated\\/60');
    expect(bars.length).toBe(5);
  });

  it('has animate-pulse class', () => {
    const { container } = render(<SkeletonCard />);
    expect(container.firstChild).toHaveClass('animate-pulse');
  });
});
