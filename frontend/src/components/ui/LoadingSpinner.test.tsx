import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { LoadingSpinner } from './LoadingSpinner';

describe('LoadingSpinner', () => {
  it('renders without text', () => {
    const { container } = render(<LoadingSpinner />);
    const spinner = container.querySelector('.animate-spin');
    expect(spinner).toBeInTheDocument();
  });

  it('renders with text', () => {
    render(<LoadingSpinner size="lg" text="Loading worlds…" />);
    expect(screen.getByText('Loading worlds…')).toBeInTheDocument();
  });

  it('applies size classes', () => {
    const { container } = render(<LoadingSpinner size="sm" />);
    const spinner = container.querySelector('.animate-spin');
    expect(spinner).toHaveClass('h-4', 'w-4');
  });
});
