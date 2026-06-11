const API_BASE_URL = process.env.PROJECT_ALPHA_API_BASE_URL ?? 'http://localhost:8080';
const USERNAME = process.env.PROJECT_ALPHA_SEED_USERNAME ?? 'korean-seed';
const PASSWORD = process.env.PROJECT_ALPHA_SEED_PASSWORD ?? 'korean-seed-password';
const BATCH_LIMIT = Number(process.env.PROJECT_ALPHA_EMBEDDING_BATCH_LIMIT ?? 20);
const MAX_BATCHES = Number(process.env.PROJECT_ALPHA_EMBEDDING_MAX_BATCHES ?? 20);
const BATCH_DELAY_MS = Number(process.env.PROJECT_ALPHA_EMBEDDING_BATCH_DELAY_MS ?? 500);

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

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
  let totalProcessed = 0;
  let totalSucceeded = 0;
  let totalFailed = 0;

  for (let batchIndex = 0; batchIndex < MAX_BATCHES; batchIndex++) {
    const result = await postJson(
      `/api/ai/embedding-jobs/process?limit=${BATCH_LIMIT}`,
      undefined,
      auth.token,
    );

    totalProcessed += result.processedCount;
    totalSucceeded += result.succeededCount;
    totalFailed += result.failedCount;

    console.log(
      JSON.stringify({
        batch: batchIndex + 1,
        processedCount: result.processedCount,
        succeededCount: result.succeededCount,
        failedCount: result.failedCount,
        firstMessage: result.results?.[0]?.message ?? '',
      }),
    );

    if (result.failedCount > 0) {
      process.exitCode = 1;
      break;
    }

    if (result.processedCount === 0) {
      break;
    }

    await sleep(BATCH_DELAY_MS);
  }

  console.log(
    JSON.stringify({
      totalProcessed,
      totalSucceeded,
      totalFailed,
    }),
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
