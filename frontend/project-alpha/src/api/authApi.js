import { apiBaseUrl, withCredentials } from './config';

export async function login(username, password) {
  const response = await fetch(`${apiBaseUrl}/api/auth/login`, withCredentials({
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
  const response = await fetch(`${apiBaseUrl}/api/auth/signup`, withCredentials({
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
  const response = await fetch(`${apiBaseUrl}/api/auth/me`, withCredentials());

  if (!response.ok) {
    throw new Error('Failed to load current user.');
  }

  return normalizeUser(await response.json());
}

export async function logout() {
  const response = await fetch(`${apiBaseUrl}/api/auth/logout`, withCredentials({
    method: 'POST',
  }));

  if (!response.ok) {
    throw new Error('Failed to logout.');
  }
}

function normalizeUser(user) {
  return {
    name: String(user.name ?? ''),
  };
}
