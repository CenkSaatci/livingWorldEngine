import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { ErrorBoundary } from './ErrorBoundary';

describe('ErrorBoundary', () => {
  it('renders children when no error', () => {
    render(
      <ErrorBoundary>
        <div>Hello World</div>
      </ErrorBoundary>,
    );
    expect(screen.getByText('Hello World')).toBeInTheDocument();
  });

  it('renders error UI when child throws', () => {
    const Bomb = () => {
      throw new Error('💥');
    };

    render(
      <ErrorBoundary>
        <Bomb />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    expect(screen.getByText('💥')).toBeInTheDocument();
  });
});
