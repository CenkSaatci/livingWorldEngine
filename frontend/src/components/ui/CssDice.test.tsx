import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { CssDice } from './CssDice';

describe('CssDice', () => {
  it('zeigt Einzelwürfe, Bonus und Summe als Aufstellung', () => {
    render(
      <CssDice
        results={[{ sides: 6, value: 4 }, { sides: 6, value: 2 }]}
        modifier={3}
        total={9}
        label="2d6+3"
      />,
    );

    expect(screen.getByText('4 + 2 + 3 = 9')).toBeInTheDocument();
  });
});
