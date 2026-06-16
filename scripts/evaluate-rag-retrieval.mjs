import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const API_BASE_URL = process.env.PROJECT_ALPHA_API_BASE_URL ?? 'http://localhost:8080';
const USERNAME = process.env.PROJECT_ALPHA_SEED_USERNAME ?? 'korean-seed';
const PASSWORD = process.env.PROJECT_ALPHA_SEED_PASSWORD ?? 'korean-seed-password';
const K = Number(process.env.PROJECT_ALPHA_RAG_EVAL_K ?? 5);
const RETRY_COUNT = Number(process.env.PROJECT_ALPHA_RAG_EVAL_RETRY_COUNT ?? 1);
const RETRY_DELAY_MS = Number(process.env.PROJECT_ALPHA_RAG_EVAL_RETRY_DELAY_MS ?? 1000);
const OUTPUT_PATH =
  process.env.PROJECT_ALPHA_RAG_EVAL_OUTPUT ??
  'backend/build/rag-current-retrieval-evaluation.json';

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
    body: JSON.stringify(body),
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

async function findSimilarPosts(evaluationCase, token) {
  return postJson(
    '/api/ai/similar-posts',
    {
      ...evaluationCase.query,
      limit: K,
    },
    token,
  );
}

async function findSimilarPostsWithRetry(evaluationCase, token) {
  for (let attempt = 0; attempt <= RETRY_COUNT; attempt++) {
    try {
      return await findSimilarPosts(evaluationCase, token);
    } catch (error) {
      if (attempt === RETRY_COUNT) {
        throw error;
      }

      await sleep(RETRY_DELAY_MS);
    }
  }

  throw new Error('Similar posts request retry loop ended unexpectedly.');
}

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
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
    retrievedTopK,
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

function average(rows, field) {
  if (rows.length === 0) {
    return 0;
  }

  return rows.reduce((sum, row) => sum + row[field], 0) / rows.length;
}

function roundMetric(value) {
  return Number(value.toFixed(4));
}

async function main() {
  const auth = await login();
  const rows = [];

  for (const evaluationCase of EVALUATION_CASES) {
    const result = await findSimilarPostsWithRetry(evaluationCase, auth.token);
    const retrievedPosts = Array.isArray(result.posts) ? result.posts : [];
    const metrics = evaluateRetrievedPosts(retrievedPosts, evaluationCase.relevantPostIds);

    rows.push({
      case: evaluationCase.name,
      [`hit@${K}`]: metrics.hit,
      [`recall@${K}`]: roundMetric(metrics.recall),
      [`precision@${K}`]: roundMetric(metrics.precision),
      [`mrr@${K}`]: roundMetric(metrics.mrr),
      [`ndcg@${K}`]: roundMetric(metrics.ndcg),
      hits: `${metrics.hitCount}/${evaluationCase.relevantPostIds.length}`,
      retrievedIds: metrics.retrievedTopK.join(', '),
    });
  }

  console.table(rows);
  const averageRow = {
    cases: rows.length,
    [`hit@${K}`]: roundMetric(average(rows, `hit@${K}`)),
    [`recall@${K}`]: roundMetric(average(rows, `recall@${K}`)),
    [`precision@${K}`]: roundMetric(average(rows, `precision@${K}`)),
    [`mrr@${K}`]: roundMetric(average(rows, `mrr@${K}`)),
    [`ndcg@${K}`]: roundMetric(average(rows, `ndcg@${K}`)),
  };
  console.table([averageRow]);

  const outputPath = resolve(OUTPUT_PATH);
  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, `${JSON.stringify({
    generatedAt: new Date().toISOString(),
    apiBaseUrl: API_BASE_URL,
    k: K,
    cases: rows,
    average: averageRow,
  }, null, 2)}\n`, 'utf8');
  console.log(`Wrote ${outputPath}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
