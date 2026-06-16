import {
  apiBaseUrl,
  authFetch,
  clearCsrfToken,
  refreshAuthCookies,
  withCsrf,
} from './config';

export async function login(username, password) {
  const response = await fetch(`${apiBaseUrl}/api/auth/login`, await withCsrf({
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username,
      password,
    }),
  }));

  if (!response.ok) {
    throw new Error('Failed to login.');
  }

  return normalizeUser(await response.json());
}

export async function signUp(username, password) {
  const response = await fetch(`${apiBaseUrl}/api/auth/signup`, await withCsrf({
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username,
      password,
    }),
  }));

  if (!response.ok) {
    throw new Error('Failed to sign up.');
  }

  return normalizeUser(await response.json());
}

export async function getCurrentUser() {
  const response = await authFetch(`${apiBaseUrl}/api/auth/me`);

  if (!response.ok) {
    throw new Error('Failed to load current user.');
  }

  return normalizeUser(await response.json());
}

export async function refreshSession() {
  const response = await refreshAuthCookies();

  if (!response.ok) {
    throw new Error('Failed to refresh session.');
  }

  return normalizeUser(await response.json());
}

export async function logout() {
  const response = await fetch(`${apiBaseUrl}/api/auth/logout`, await withCsrf({
    method: 'POST',
  }));

  if (!response.ok) {
    throw new Error('Failed to logout.');
  }

  clearCsrfToken();
}

function normalizeUser(user) {
  return {
    name: String(user.name ?? ''),
  };
}
