const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function login(username, password) {
  const response = await fetch(`${apiBaseUrl}/api/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username,
      password,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to login.');
  }

  return normalizeUser(await response.json());
}

export async function signUp(username, password) {
  const response = await fetch(`${apiBaseUrl}/api/auth/signup`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username,
      password,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to sign up.');
  }

  return normalizeUser(await response.json());
}

function normalizeUser(user) {
  return {
    name: String(user.name ?? ''),
  };
}
