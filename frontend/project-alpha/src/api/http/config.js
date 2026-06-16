export const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ?? `${window.location.protocol}//${window.location.hostname}:8080`;

export const authExpiredEventName = 'project-alpha-auth-expired';

let csrfToken = null;
let csrfHeaderName = 'X-XSRF-TOKEN';
let csrfTokenRequest = null;
let refreshTokenRequest = null;

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

export async function authFetch(input, options = {}) {
  let requestOptions = withCredentials(options);
  let response = await fetch(input, requestOptions);

  if (response.status === 403 && hasCsrfHeader(requestOptions)) {
    requestOptions = await withFreshCsrf(requestOptions);
    response = await fetch(input, requestOptions);
  }

  if (response.status !== 401) {
    return response;
  }

  const refreshResponse = await refreshAuthCookies();

  if (!refreshResponse.ok) {
    notifyAuthExpired();
    return response;
  }

  requestOptions = hasCsrfHeader(requestOptions)
    ? await withFreshCsrf(requestOptions)
    : requestOptions;

  let retryResponse = await fetch(input, requestOptions);

  if (retryResponse.status === 403 && hasCsrfHeader(requestOptions)) {
    requestOptions = await withFreshCsrf(requestOptions);
    retryResponse = await fetch(input, requestOptions);
  }

  if (retryResponse.status === 401) {
    notifyAuthExpired();
  }

  return retryResponse;
}

export async function refreshAuthCookies() {
  if (refreshTokenRequest === null) {
    refreshTokenRequest = withCsrf({
      method: 'POST',
    })
      .then((options) => fetch(`${apiBaseUrl}/api/auth/refresh`, options))
      .finally(() => {
        refreshTokenRequest = null;
      });
  }

  return refreshTokenRequest;
}

export function clearCsrfToken() {
  csrfToken = null;
  csrfTokenRequest = null;
  refreshTokenRequest = null;
}

async function withFreshCsrf(options = {}) {
  clearCsrfToken();

  return withCsrf({
    ...options,
    headers: removeCsrfHeaders(options.headers),
  });
}

function hasCsrfHeader(options = {}) {
  const headers = normalizeHeaders(options.headers);
  const expectedHeaderName = csrfHeaderName.toLowerCase();

  return Object.keys(headers).some((headerName) => {
    const normalizedHeaderName = headerName.toLowerCase();

    return normalizedHeaderName === expectedHeaderName || normalizedHeaderName === 'x-xsrf-token';
  });
}

function removeCsrfHeaders(headers = {}) {
  const nextHeaders = normalizeHeaders(headers);
  const expectedHeaderName = csrfHeaderName.toLowerCase();

  for (const headerName of Object.keys(nextHeaders)) {
    const normalizedHeaderName = headerName.toLowerCase();

    if (normalizedHeaderName === expectedHeaderName || normalizedHeaderName === 'x-xsrf-token') {
      delete nextHeaders[headerName];
    }
  }

  return nextHeaders;
}

function normalizeHeaders(headers = {}) {
  if (headers instanceof Headers) {
    return Object.fromEntries(headers.entries());
  }

  if (Array.isArray(headers)) {
    return Object.fromEntries(headers);
  }

  return {
    ...headers,
  };
}

function notifyAuthExpired() {
  clearCsrfToken();

  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(authExpiredEventName));
  }
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
