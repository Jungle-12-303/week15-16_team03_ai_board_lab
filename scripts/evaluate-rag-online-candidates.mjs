import { spawn } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const BACKEND_DIR = resolve('backend');
const OUTPUT_PATH =
  process.env.PROJECT_ALPHA_ONLINE_CANDIDATE_OUTPUT ??
  'backend/build/rag-online-candidate-evaluation.json';
const SERVER_PORT = Number(process.env.PROJECT_ALPHA_ONLINE_CANDIDATE_PORT ?? 18080);
const K = Number(process.env.PROJECT_ALPHA_RAG_EVAL_K ?? 5);
const USERNAME = process.env.PROJECT_ALPHA_SEED_USERNAME ?? 'korean-seed';
const PASSWORD = process.env.PROJECT_ALPHA_SEED_PASSWORD ?? 'korean-seed-password';

const EVALUATION_CASES = [
  {
    name: 'RAG/LLM 서비스 설계',
    query: {
      category: 'All',
      title: '게시판에 RAG 기반 글쓰기 도우미를 붙이는 방법',
      content:
        '기존 게시글을 검색해서 비슷한 글을 추천하고, 그 내용을 근거로 초안을 생성하는 RAG 기능을 설계하고 싶다.',
      tags: [],
    },
    relevantPostIds: [395, 398, 402, 406, 416, 537, 582, 639, 670, 801, 1291],
  },
  {
    name: 'GitHub Actions 배포 자동화',
    query: {
      category: 'All',
      title: 'GitHub Actions로 PR 머지 후 자동 배포하기',
      content:
        'GitHub Actions workflow, secrets, pull request merge 이벤트를 이용해서 배포 파이프라인을 자동화하는 내용을 찾고 싶다.',
      tags: [],
    },
    relevantPostIds: [525, 526, 638, 646, 648, 1562],
  },
  {
    name: 'Kubernetes/EKS 운영',
    query: {
      category: 'All',
      title: 'Kubernetes 환경에서 서비스 배포와 운영을 개선하기',
      content:
        'EKS, Kubernetes, container, canary deployment, private registry, cluster migration 같은 운영 사례를 참고하고 싶다.',
      tags: [],
    },
    relevantPostIds: [399, 432, 583, 590, 598, 602, 610, 616, 630, 642, 660, 703, 903, 907, 1292, 1471],
  },
  {
    name: 'Redis 캐시와 세션',
    query: {
      category: 'All',
      title: 'Spring에서 Redis 캐시와 세션 저장소를 안정적으로 운영하기',
      content:
        'Spring Cache, Redis, 분산 락, Spring Session, connection 증가 문제와 캐시 삭제 이슈를 정리하고 싶다.',
      tags: [],
    },
    relevantPostIds: [552, 579, 580, 595, 674, 689, 695],
  },
  {
    name: 'React 프론트엔드 개발',
    query: {
      category: 'All',
      title: 'React 프론트엔드와 JavaScript 번들 최적화',
      content:
        'React 19 마이그레이션, useState 같은 기본 훅, WebView, React Native, JavaScript bundle size 최적화 사례를 찾고 싶다.',
      tags: [],
    },
    relevantPostIds: [397, 521, 522, 564, 566, 575, 604, 607, 664],
  },
  {
    name: 'Airflow 데이터 파이프라인',
    query: {
      category: 'All',
      title: 'Airflow와 데이터 파이프라인 운영 경험',
      content:
        'Airflow, ELT, 로그 파이프라인, 데이터 분석 플랫폼, 데이터 디스커버리와 데이터 엔지니어링 운영 사례를 보고 싶다.',
      tags: [],
    },
    relevantPostIds: [578, 796, 1158, 1232, 1285, 1347, 1458, 1463, 1577],
  },
];

const CANDIDATES = [
  {
    id: 'baseline-current',
    label: 'Current baseline',
    args: {},
  },
  {
    id: 'query-title-content',
    label: 'Query title + content',
    args: {
      'app.rag-search.query-mode': 'TITLE_CONTENT',
    },
  },
  {
    id: 'chunk-3-percent',
    label: 'Chunk evidence 3%',
    args: {
      'app.rag-search.chunk-evidence-weight': '0.03',
    },
  },
  {
    id: 'bm25-k1-2-b-025',
    label: 'BM25 k1=2 b=0.25',
    args: {
      'app.rag-search.bm25-k1': '2.0',
      'app.rag-search.bm25-b': '0.25',
    },
  },
  {
    id: 'query-terms-12-guard-6',
    label: 'Query terms 12 / guard 6',
    args: {
      'app.rag-search.max-query-terms': '12',
      'app.rag-search.max-guard-terms': '6',
    },
  },
  {
    id: 'weighted-fusion',
    label: 'Weighted fusion',
    args: {
      'app.rag-search.fusion-mode': 'WEIGHTED',
    },
  },
  {
    id: 'combined-tuned',
    label: 'Combined tuned',
    args: {
      'app.rag-search.query-mode': 'TITLE_CONTENT',
      'app.rag-search.chunk-evidence-weight': '0.03',
      'app.rag-search.bm25-k1': '2.0',
      'app.rag-search.bm25-b': '0.25',
      'app.rag-search.max-query-terms': '12',
      'app.rag-search.max-guard-terms': '6',
    },
  },
  {
    id: 'combined-weighted-tuned',
    label: 'Combined tuned + weighted fusion',
    args: {
      'app.rag-search.query-mode': 'TITLE_CONTENT',
      'app.rag-search.chunk-evidence-weight': '0.03',
      'app.rag-search.bm25-k1': '2.0',
      'app.rag-search.bm25-b': '0.25',
      'app.rag-search.max-query-terms': '12',
      'app.rag-search.max-guard-terms': '6',
      'app.rag-search.fusion-mode': 'WEIGHTED',
    },
  },
  {
    id: 'metadata-off-diagnostic',
    label: 'Metadata off diagnostic',
    args: {
      'app.rag-search.metadata-enabled': 'false',
    },
  },
];

async function main() {
  const results = [];

  for (const candidate of CANDIDATES) {
    console.log(`\n=== ${candidate.label} ===`);
    const server = await startServer(candidate);

    try {
      const result = await evaluateCandidate(candidate);
      console.table([toSummaryRow(result)]);
      results.push(result);
    } finally {
      await stopServer(server);
      await sleep(3000);
    }
  }

  const report = {
    generatedAt: new Date().toISOString(),
    note: 'Online candidate evaluation. Each candidate starts a real Spring Boot server on a temporary port and calls /api/ai/similar-posts with Qdrant/MySQL/OpenAI query embeddings.',
    k: K,
    port: SERVER_PORT,
    candidates: results,
  };
  const outputPath = resolve(OUTPUT_PATH);
  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8');

  console.log('\nFinal summary');
  console.table(results.map(toSummaryRow));
  console.log(`Wrote ${outputPath}`);
}

async function startServer(candidate) {
  const args = [
    `--server.port=${SERVER_PORT}`,
    '--spring.output.ansi.enabled=never',
    '--app.embedding-worker.enabled=false',
    ...Object.entries(candidate.args).map(([key, value]) => `--${key}=${value}`),
  ];
  const bootRunArg = `--args=${args.join(' ')}`;
  const child = spawn(
    'cmd.exe',
    ['/c', '.\\gradlew.bat', 'bootRun', bootRunArg],
    {
      cwd: BACKEND_DIR,
      stdio: ['ignore', 'pipe', 'pipe'],
      windowsHide: true,
    },
  );
  let stdout = '';
  let stderr = '';

  child.stdout.on('data', (data) => {
    stdout += data.toString();
    trimLogs();
  });
  child.stderr.on('data', (data) => {
    stderr += data.toString();
    trimLogs();
  });

  function trimLogs() {
    stdout = stdout.slice(-20000);
    stderr = stderr.slice(-20000);
  }

  await waitForServer(child, () => stdout + stderr);

  return { child, getLogs: () => stdout + stderr };
}

async function waitForServer(child, getLogs) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 180000) {
    if (child.exitCode != null) {
      throw new Error(`bootRun exited early with code ${child.exitCode}\n${getLogs()}`);
    }

    try {
      const response = await fetch(`http://localhost:${SERVER_PORT}/actuator/health`);
      if (response.ok) {
        return;
      }
    } catch {
      // keep waiting
    }

    await sleep(2000);
  }

  throw new Error(`Timed out waiting for Spring Boot server.\n${getLogs()}`);
}

async function stopServer(server) {
  const pid = server.child.pid;
  if (!pid || server.child.exitCode != null) {
    return;
  }

  await new Promise((resolve) => {
    const killer = spawn('cmd.exe', ['/c', 'taskkill', '/PID', String(pid), '/T', '/F'], {
      stdio: 'ignore',
      windowsHide: true,
    });
    killer.on('exit', resolve);
    killer.on('error', resolve);
  });
}

async function evaluateCandidate(candidate) {
  const auth = await postJson('/api/auth/login', {
    username: USERNAME,
    password: PASSWORD,
  });
  const cases = [];

  for (const evaluationCase of EVALUATION_CASES) {
    const response = await postJson(
      '/api/ai/similar-posts',
      {
        ...evaluationCase.query,
        limit: K,
      },
      auth.token,
    );
    const retrievedPosts = Array.isArray(response.posts) ? response.posts : [];
    const metrics = evaluateRetrievedPosts(retrievedPosts, evaluationCase.relevantPostIds);

    cases.push({
      case: evaluationCase.name,
      relevantTotal: evaluationCase.relevantPostIds.length,
      [`hit@${K}`]: metrics.hit,
      [`recall@${K}`]: roundMetric(metrics.recall),
      [`precision@${K}`]: roundMetric(metrics.precision),
      [`mrr@${K}`]: roundMetric(metrics.mrr),
      [`ndcg@${K}`]: roundMetric(metrics.ndcg),
      hits: `${metrics.hitCount}/${evaluationCase.relevantPostIds.length}`,
      retrievedPosts: retrievedPosts.slice(0, K).map((post) => ({
        postId: post.postId,
        title: post.title,
        score: post.score,
      })),
    });
  }

  return {
    id: candidate.id,
    label: candidate.label,
    args: candidate.args,
    average: averageCaseResults(cases),
    cases,
  };
}

async function postJson(path, body, token) {
  const headers = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`http://localhost:${SERVER_PORT}${path}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  });
  const responseText = await response.text();

  if (!response.ok) {
    throw new Error(`${path} failed: ${response.status} ${responseText}`);
  }

  return responseText ? JSON.parse(responseText) : null;
}

function evaluateRetrievedPosts(retrievedPosts, relevantPostIds) {
  const relevantSet = new Set(relevantPostIds);
  const retrievedTopK = retrievedPosts.slice(0, K).map((post) => Number(post.postId));
  const hitCount = retrievedTopK.filter((postId) => relevantSet.has(postId)).length;
  const firstRelevantIndex = retrievedTopK.findIndex((postId) => relevantSet.has(postId));

  return {
    hit: hitCount > 0 ? 1 : 0,
    recall: relevantSet.size === 0 ? 0 : hitCount / relevantSet.size,
    precision: K === 0 ? 0 : hitCount / K,
    mrr: firstRelevantIndex === -1 ? 0 : 1 / (firstRelevantIndex + 1),
    ndcg: normalizedDiscountedCumulativeGain(retrievedTopK, relevantSet),
    hitCount,
  };
}

function normalizedDiscountedCumulativeGain(retrievedTopK, relevantSet) {
  const dcg = retrievedTopK.reduce((sum, postId, index) => {
    if (!relevantSet.has(postId)) {
      return sum;
    }

    return sum + 1 / Math.log2(index + 2);
  }, 0);
  const idealHitCount = Math.min(retrievedTopK.length, relevantSet.size);
  const idcg = Array.from({ length: idealHitCount }).reduce(
    (sum, _, index) => sum + 1 / Math.log2(index + 2),
    0,
  );

  return idcg === 0 ? 0 : dcg / idcg;
}

function averageCaseResults(cases) {
  return {
    cases: cases.length,
    [`hit@${K}`]: roundMetric(average(cases, `hit@${K}`)),
    [`recall@${K}`]: roundMetric(average(cases, `recall@${K}`)),
    [`precision@${K}`]: roundMetric(average(cases, `precision@${K}`)),
    [`mrr@${K}`]: roundMetric(average(cases, `mrr@${K}`)),
    [`ndcg@${K}`]: roundMetric(average(cases, `ndcg@${K}`)),
  };
}

function average(rows, field) {
  if (rows.length === 0) {
    return 0;
  }

  return rows.reduce((sum, row) => sum + row[field], 0) / rows.length;
}

function toSummaryRow(result) {
  return {
    id: result.id,
    label: result.label,
    hit: result.average[`hit@${K}`],
    recall: result.average[`recall@${K}`],
    precision: result.average[`precision@${K}`],
    mrr: result.average[`mrr@${K}`],
    ndcg: result.average[`ndcg@${K}`],
  };
}

function roundMetric(value) {
  return Number(value.toFixed(4));
}

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
