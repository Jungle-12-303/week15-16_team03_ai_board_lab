const API_BASE_URL = process.env.PROJECT_ALPHA_API_BASE_URL ?? 'http://localhost:8080';
const USERNAME = process.env.PROJECT_ALPHA_SEED_USERNAME ?? 'korean-seed';
const PASSWORD = process.env.PROJECT_ALPHA_SEED_PASSWORD ?? 'korean-seed-password';
const LIMIT = Number(process.env.PROJECT_ALPHA_CHUNK_JOB_ENQUEUE_LIMIT ?? 20);

async function postJson(path, body, token) {
  const headers = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const responseText = await response.text();

  if (!response.ok) {
    throw new Error(`${path} failed: ${response.status} ${responseText}`);
  }

  return responseText ? JSON.parse(responseText) : null;
}

async function login() {
  return postJson('/api/auth/login', {
    username: USERNAME,
    password: PASSWORD,
  });
}

async function main() {
  const auth = await login();
  const result = await postJson(
    `/api/ai/embedding-jobs/enqueue-missing-chunks?limit=${LIMIT}`,
    undefined,
    auth.token,
  );

  console.log(JSON.stringify(result, null, 2));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
