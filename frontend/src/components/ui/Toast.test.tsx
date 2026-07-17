import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { ToastContainer } from './Toast';
import { useToastStore } from '../../store/toastStore';

beforeEach(() => {
  useToastStore.setState({ toasts: [] });
});

describe('ToastContainer', () => {
  it('renders nothing when empty', () => {
    const { container } = render(<ToastContainer />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders toast messages', () => {
    useToastStore.setState({
      toasts: [{ id: '1', message: 'Hello', type: 'info' as const, timeout: 3000 }],
    });
    render(<ToastContainer />);
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });

  it('renders multiple toasts', () => {
    useToastStore.setState({
      toasts: [
        { id: '1', message: 'First', type: 'info' as const, timeout: 3000 },
        { id: '2', message: 'Second', type: 'error' as const, timeout: 5000 },
      ],
    });
    render(<ToastContainer />);
    expect(screen.getByText('First')).toBeInTheDocument();
    expect(screen.getByText('Second')).toBeInTheDocument();
  });
});
