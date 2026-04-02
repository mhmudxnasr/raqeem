import { afterEach, describe, expect, it, vi } from 'vitest';

const sessionRestoreErrorMessage = 'We could not restore your session. Please sign in again.';

async function loadStore(getSessionImpl: () => Promise<unknown>) {
  vi.resetModules();
  vi.doMock('../lib/supabase', () => ({
    isSupabaseConfigured: true,
    supabase: {
      auth: {
        getSession: getSessionImpl,
        signInWithPassword: vi.fn(),
        signOut: vi.fn(),
      },
    },
  }));

  const module = await import('./useAuthStore');
  return module.useAuthStore;
}

afterEach(() => {
  vi.useRealTimers();
  vi.clearAllMocks();
  vi.resetModules();
});

describe('useAuthStore.initialize', () => {
  it('sets ready state when session restore rejects', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    const useAuthStore = await loadStore(async () => {
      throw new Error('network failure');
    });

    await useAuthStore.getState().initialize();

    expect(useAuthStore.getState()).toMatchObject({
      user: null,
      status: 'ready',
      error: sessionRestoreErrorMessage,
      isConfigured: true,
    });
    expect(consoleError).toHaveBeenCalled();
  });

  it('sets ready state when session restore times out', async () => {
    vi.useFakeTimers();
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    const useAuthStore = await loadStore(() => new Promise(() => {}));

    const initializePromise = useAuthStore.getState().initialize();
    await vi.advanceTimersByTimeAsync(5000);
    await initializePromise;

    expect(useAuthStore.getState()).toMatchObject({
      user: null,
      status: 'ready',
      error: sessionRestoreErrorMessage,
      isConfigured: true,
    });
    expect(consoleError).toHaveBeenCalled();
  });
});
