export const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ?? `${window.location.protocol}//${window.location.hostname}:8080`;

let csrfToken = null;
let csrfHeaderName = 'X-XSRF-TOKEN';
let csrfTokenRequest = null;

export function withCredentials(options = {}) {
  return {
    ...options,
    credentials: 'include',
  };
}

export async function withCsrf(options = {}) {
  const { headerName, token } = await loadCsrfToken();

  return withCredentials({
    ...options,
    headers: {
      ...(options.headers ?? {}),
      [headerName]: token,
    },
  });
}

export function clearCsrfToken() {
  csrfToken = null;
  csrfTokenRequest = null;
}

async function loadCsrfToken() {
  if (csrfToken !== null) {
    return {
      headerName: csrfHeaderName,
      token: csrfToken,
    };
  }

  if (csrfTokenRequest === null) {
    csrfTokenRequest = fetch(`${apiBaseUrl}/api/auth/csrf`, withCredentials())
      .then(async (response) => {
        if (!response.ok) {
          throw new Error('Failed to load CSRF token.');
        }

        const csrfResponse = await response.json();
        csrfHeaderName = String(csrfResponse.headerName ?? 'X-XSRF-TOKEN');
        csrfToken = String(csrfResponse.token ?? '');

        return {
          headerName: csrfHeaderName,
          token: csrfToken,
        };
      })
      .finally(() => {
        csrfTokenRequest = null;
      });
  }

  return csrfTokenRequest;
}
