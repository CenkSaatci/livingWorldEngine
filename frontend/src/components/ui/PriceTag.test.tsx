import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PriceTag } from './PriceTag';

describe('PriceTag', () => {
  it('renders base price without diff', () => {
    render(<PriceTag basePrice={50} finalPrice={50} />);
    expect(screen.getByText('50 G')).toBeInTheDocument();
  });

  it('shows premium with markup indicator', () => {
    render(<PriceTag basePrice={50} finalPrice={75} />);
    expect(screen.getByText('75 G')).toBeInTheDocument();
    expect(screen.getByText('(+25)')).toBeInTheDocument();
  });

  it('shows discount', () => {
    render(<PriceTag basePrice={50} finalPrice={40} />);
    expect(screen.getByText('40 G')).toBeInTheDocument();
    expect(screen.getByText('(-10)')).toBeInTheDocument();
  });
});
