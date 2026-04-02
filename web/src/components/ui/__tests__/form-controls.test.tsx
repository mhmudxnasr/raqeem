import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useForm } from 'react-hook-form';
import { describe, expect, it, vi } from 'vitest';

import { Input } from '../Input';
import { Select } from '../Select';

interface FormValues {
  email: string;
  accountId: string;
}

function FormFixture({ onSubmit }: { onSubmit: (values: FormValues) => void }) {
  const { register, handleSubmit } = useForm<FormValues>({
    defaultValues: {
      email: '',
      accountId: 'cash',
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input label="Email" {...register('email')} />
      <Select
        label="Account"
        options={[
          { label: 'Cash', value: 'cash' },
          { label: 'Bank', value: 'bank' },
        ]}
        {...register('accountId')}
      />
      <button type="submit">Save</button>
    </form>
  );
}

describe('form controls', () => {
  it('submit registered values through react-hook-form', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn<(values: FormValues) => void>();

    render(<FormFixture onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText(/email/i), 'mahmudelmorsy0@gmail.com');
    await user.selectOptions(screen.getByLabelText(/account/i), 'bank');
    await user.click(screen.getByRole('button', { name: /save/i }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    const submittedValues = onSubmit.mock.calls.at(0)?.[0];
    expect(submittedValues).toEqual({
      email: 'mahmudelmorsy0@gmail.com',
      accountId: 'bank',
    });
  });
});
