import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi } from '../api/endpoints';
import { tokenStore } from '../api/client';
import type { LoginRequest, MeResponse, RegisterRequest } from '../api/types';

interface AuthContextValue {
  user: MeResponse | null;
  initializing: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [initializing, setInitializing] = useState(true);

  // On first load, if a token is present, resolve the current user.
  useEffect(() => {
    let active = true;
    const token = tokenStore.get();
    if (!token) {
      setInitializing(false);
      return;
    }
    authApi
      .me()
      .then((me) => {
        if (active) setUser(me);
      })
      .catch(() => {
        tokenStore.clear();
      })
      .finally(() => {
        if (active) setInitializing(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      initializing,
      login: async (credentials) => {
        const tokens = await authApi.login(credentials);
        tokenStore.set(tokens.accessToken);
        setUser(await authApi.me());
      },
      register: async (data) => {
        await authApi.register(data);
      },
      logout: async () => {
        try {
          await authApi.logout();
        } finally {
          tokenStore.clear();
          setUser(null);
        }
      },
    }),
    [user, initializing],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
