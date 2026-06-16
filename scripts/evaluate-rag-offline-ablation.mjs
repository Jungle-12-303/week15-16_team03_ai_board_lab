import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const MYSQL_CONTAINER = process.env.PROJECT_ALPHA_MYSQL_CONTAINER ?? 'project-alpha-mysql';
const MYSQL_USER = process.env.PROJECT_ALPHA_MYSQL_USER ?? 'alpha';
const MYSQL_PASSWORD = process.env.PROJECT_ALPHA_MYSQL_PASSWORD ?? 'alpha_password';
const MYSQL_DATABASE = process.env.PROJECT_ALPHA_MYSQL_DATABASE ?? 'project_alpha';
const DOTENV_PATH = process.env.PROJECT_ALPHA_BACKEND_DOTENV ?? 'backend/.env';
const OUTPUT_PATH =
  process.env.PROJECT_ALPHA_OFFLINE_ABLATION_OUTPUT ??
  'backend/build/rag-offline-ablation.json';
const EMBEDDING_CACHE_PATH =
  process.env.PROJECT_ALPHA_QUERY_EMBEDDING_CACHE ??
  'backend/build/rag-query-embedding-cache.json';
const K = Number(process.env.PROJECT_ALPHA_RAG_EVAL_K ?? 5);

const DEFAULT_CONFIG = {
  queryVariant: 'current',
  useVector: true,
  useBm25: true,
  useRrf: true,
  useMetadata: true,
  useChunks: true,
  vectorWeight: 0.35,
  bm25Weight: 0.65,
  keywordWeight: 0.25,
  chunkWeight: 0.01,
  vectorThreshold: 0.45,
  hybridThreshold: 0.38,
  rankedThreshold: 0.6,
  bm25K1: 1.2,
  bm25B: 0.75,
  rrfK: 60,
  maxQueryTerms: 16,
  maxGuardTerms: 8,
};

const QUERY_VARIANTS = [
  {
    id: 'title-only',
    label: 'Title only',
    buildText: (evaluationCase) => evaluationCase.title,
  },
  {
    id: 'content-only',
    label: 'Content only',
    buildText: (evaluationCase) => evaluationCase.content,
  },
  {
    id: 'title-tags',
    label: 'Title + tags',
    buildText: (evaluationCase) => [
      evaluationCase.title,
      `Tags: ${formatTags(evaluationCase.tags)}`,
    ].join('\n'),
  },
  {
    id: 'title-content',
    label: 'Title + content',
    buildText: (evaluationCase) => [
      evaluationCase.title,
      evaluationCase.content,
    ].join('\n'),
  },
  {
    id: 'title-content-tags',
    label: 'Title + content + tags',
    buildText: (evaluationCase) => [
      evaluationCase.title,
      evaluationCase.content,
      `Tags: ${formatTags(evaluationCase.tags)}`,
    ].join('\n'),
  },
  {
    id: 'title-content300-tags',
    label: 'Title + first 300 chars + tags',
    buildText: (evaluationCase) => [
      evaluationCase.title,
      evaluationCase.content.slice(0, 300),
      `Tags: ${formatTags(evaluationCase.tags)}`,
    ].join('\n'),
  },
  {
    id: 'title-weighted-content-tags',
    label: 'Title weighted + content + tags',
    buildText: (evaluationCase) => [
      evaluationCase.title,
      evaluationCase.title,
      evaluationCase.content,
      `Tags: ${formatTags(evaluationCase.tags)}`,
    ].join('\n'),
  },
  {
    id: 'current',
    label: 'Current query template',
    buildText: (evaluationCase) => [
      'Search Query:',
      `Category: ${evaluationCase.category}`,
      `Title: ${evaluationCase.title}`,
      'User draft or intent:',
      evaluationCase.content,
      '',
      `Tags: ${formatTags(evaluationCase.tags)}`,
    ].join('\n'),
  },
];

const EVALUATION_CASES = [
  {
    suite: 'legacy-id',
    id: 'rag-llm-service',
    name: 'RAG/LLM 서비스 설계',
    category: 'All',
    title: '게시판에 RAG 기반 글쓰기 도우미를 붙이는 방법',
    content:
      '기존 게시글을 검색해서 비슷한 글을 추천하고, 그 내용을 근거로 초안을 생성하는 RAG 기능을 설계하고 싶다.',
    tags: [],
    relevantPostIds: [395, 398, 402, 406, 416, 537, 582, 639, 670, 801, 1291],
  },
  {
    suite: 'legacy-id',
    id: 'github-actions-deploy',
    name: 'GitHub Actions 배포 자동화',
    category: 'All',
    title: 'GitHub Actions로 PR 머지 후 자동 배포하기',
    content:
      'GitHub Actions workflow, secrets, pull request merge 이벤트를 이용해서 배포 파이프라인을 자동화하는 내용을 찾고 싶다.',
    tags: [],
    relevantPostIds: [525, 526, 638, 646, 648, 1562],
  },
  {
    suite: 'legacy-id',
    id: 'kubernetes-eks',
    name: 'Kubernetes/EKS 운영',
    category: 'All',
    title: 'Kubernetes 환경에서 서비스 배포와 운영을 개선하기',
    content:
      'EKS, Kubernetes, container, canary deployment, private registry, cluster migration 같은 운영 사례를 참고하고 싶다.',
    tags: [],
    relevantPostIds: [399, 432, 583, 590, 598, 602, 610, 616, 630, 642, 660, 703, 903, 907, 1292, 1471],
  },
  {
    suite: 'legacy-id',
    id: 'redis-cache-session',
    name: 'Redis 캐시와 세션',
    category: 'All',
    title: 'Spring에서 Redis 캐시와 세션 저장소를 안정적으로 운영하기',
    content:
      'Spring Cache, Redis, 분산 락, Spring Session, connection 증가 문제와 캐시 삭제 이슈를 정리하고 싶다.',
    tags: [],
    relevantPostIds: [552, 579, 580, 595, 674, 689, 695],
  },
  {
    suite: 'legacy-id',
    id: 'react-frontend',
    name: 'React 프론트엔드 개발',
    category: 'All',
    title: 'React 프론트엔드와 JavaScript 번들 최적화',
    content:
      'React 19 마이그레이션, useState 같은 기본 훅, WebView, React Native, JavaScript bundle size 최적화 사례를 찾고 싶다.',
    tags: [],
    relevantPostIds: [397, 521, 522, 564, 566, 575, 604, 607, 664],
  },
  {
    suite: 'legacy-id',
    id: 'airflow-data-pipeline',
    name: 'Airflow 데이터 파이프라인',
    category: 'All',
    title: 'Airflow와 데이터 파이프라인 운영 경험',
    content:
      'Airflow, ELT, 로그 파이프라인, 데이터 분석 플랫폼, 데이터 디스커버리와 데이터 엔지니어링 운영 사례를 보고 싶다.',
    tags: [],
    relevantPostIds: [578, 796, 1158, 1232, 1285, 1347, 1458, 1463, 1577],
  },
  {
    suite: 'subject-heuristic',
    id: 'hardware-cpu-upgrade',
    name: '하드웨어 CPU 업그레이드',
    category: 'Daily',
    title: '7800X3D에서 9800X3D로 업그레이드할지 고민',
    content: '현재 7800X3D를 쓰고 있는데 CPU를 바꿀지 다음 세대를 기다릴지 고민 중입니다.',
    tags: ['하드웨어', 'CPU', '업그레이드'],
    requiredTerms: ['[hw-scenario]'],
    minimumAnyTermMatches: 2,
    anyTerms: ['cpu', 'upgrade', '9800x3d', '7800x3d'],
  },
  {
    suite: 'subject-heuristic',
    id: 'hardware-gpu-noise',
    name: '하드웨어 GPU 팬 소음',
    category: 'Daily',
    title: 'GPU 팬 소음과 언더볼팅 기록',
    content: '그래픽카드 팬 소음이 거슬려서 언더볼팅과 온도 변화를 기록하려고 합니다.',
    tags: ['하드웨어', 'GPU', '발열'],
    requiredTerms: ['[hw-scenario]'],
    minimumAnyTermMatches: 2,
    anyTerms: ['gpu', 'undervolt', 'fan', 'noise'],
  },
  {
    suite: 'subject-heuristic',
    id: 'github-actions-subject',
    name: 'GitHub Actions 주제 검색',
    category: 'Learning',
    title: 'GitHub Actions 배포 실패 정리',
    content: 'GitHub Actions workflow에서 배포가 실패해서 secrets와 CI 설정을 정리하려고 합니다.',
    tags: ['GitHub', 'CI'],
    requiredTerms: ['github'],
    minimumAnyTermMatches: 2,
    anyTerms: ['actions', 'workflow', 'deploy', 'ci', 'secrets'],
  },
  {
    suite: 'subject-heuristic',
    id: 'react-state-hooks-subject',
    name: 'React state와 hook',
    category: 'Learning',
    title: 'React state와 hook 학습 메모',
    content: 'useState와 custom hook을 쓰면서 상태 관리가 어떻게 분리되는지 정리하려고 합니다.',
    tags: ['React', 'State'],
    requiredTerms: ['react'],
    minimumAnyTermMatches: 1,
    anyTerms: ['hook', 'state', 'usestate'],
  },
  {
    suite: 'subject-heuristic',
    id: 'weather-briefing-subject',
    name: '날씨 브리핑',
    category: 'Briefing',
    title: '오늘 날씨 브리핑 작성',
    content: '오늘 지역 날씨와 기온을 짧은 게시글로 정리하려고 합니다.',
    tags: ['날씨', '브리핑'],
    requiredTerms: ['weather'],
    minimumAnyTermMatches: 1,
    anyTerms: ['temperature', 'forecast', 'current', 'location'],
  },
];

const STOP_WORDS = new Set([
  'the',
  'and',
  'for',
  'with',
  'that',
  'this',
  'from',
  'into',
  'are',
  'was',
  'were',
  'search',
  'query',
  'tags',
  'category',
  'title',
  'user',
  'draft',
  'intent',
  '있다',
  '있는',
  '합니다',
  '하고',
  '해서',
  '대한',
  '관련',
  '정리',
  '싶다',
  '같은',
  '기반',
  '사용',
  '게시글',
  '내용',
  '기능',
  '방법',
  '오늘',
]);

async function main() {
  loadDotEnv(DOTENV_PATH);
  const openAiConfigured = Boolean(process.env.OPENAI_API_KEY);
  const queryVariantsById = new Map(QUERY_VARIANTS.map((variant) => [variant.id, variant]));
  const posts = loadPostDocuments();
  const chunks = loadChunkDocuments(posts);
  const cases = EVALUATION_CASES.map((evaluationCase) => attachRelevantPostIds(evaluationCase, posts));
  const queryEmbeddingCache = openAiConfigured
    ? await loadOrCreateQueryEmbeddingCache(cases)
    : new Map();

  const context = {
    posts,
    chunks,
    cases,
    queryVariantsById,
    queryEmbeddingCache,
    openAiConfigured,
  };
  const groups = [
    buildQueryCompositionGroup(),
    buildVectorBm25WeightGroup(),
    buildChunkEvidenceWeightGroup(),
    buildRankedThresholdGroup(),
    buildVectorThresholdGroup(),
    buildHybridThresholdGroup(),
    buildBm25ParameterGroup(),
    buildRrfConstantGroup(),
    buildQueryTermLimitGroup(),
    buildMetadataGroup(),
    buildCoreRetrieverGroup(),
  ];

  const groupResults = groups.map((group) => evaluateGroup(context, group));
  const report = {
    generatedAt: new Date().toISOString(),
    note: 'Offline ablation using stored post/chunk embeddings from MySQL. It does not mutate MySQL/Qdrant and only creates query embeddings when OPENAI_API_KEY is configured.',
    k: K,
    openAiQueryEmbeddings: openAiConfigured,
    postCount: posts.length,
    chunkCount: chunks.length,
    evaluationCaseCount: cases.length,
    groups: groupResults,
  };

  const outputPath = resolve(OUTPUT_PATH);
  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8');

  for (const group of groupResults) {
    console.log(`\n${group.label}`);
    console.table(
      group.variants.map((variant) => ({
        variant: variant.label,
        precision: variant.average[`precision@${K}`],
        recall: variant.average[`recall@${K}`],
        mrr: variant.average[`mrr@${K}`],
        ndcg: variant.average[`ndcg@${K}`],
        hit: variant.average[`hit@${K}`],
      })),
    );
    console.log(`bestByNdcg=${group.bestByNdcg.label}`);
  }

  console.log(`\nWrote ${outputPath}`);
}

function loadDotEnv(dotEnvPath) {
  const absolutePath = resolve(dotEnvPath);
  if (!existsSync(absolutePath)) {
    return;
  }

  for (const line of readFileSync(absolutePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }

    const separatorIndex = trimmed.indexOf('=');
    if (separatorIndex === -1) {
      continue;
    }

    const key = trimmed.slice(0, separatorIndex).trim();
    let value = trimmed.slice(separatorIndex + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }

    if (!process.env[key]) {
      process.env[key] = value;
    }
  }
}

function loadPostDocuments() {
  const sql = `
SELECT
  p.id,
  HEX(p.category),
  HEX(p.title),
  HEX(p.content),
  HEX(COALESCE((
    SELECT GROUP_CONCAT(t.name ORDER BY t.name SEPARATOR ',')
    FROM post_tags pt
    JOIN tags t ON t.id = pt.tag_id
    WHERE pt.post_id = p.id
  ), '')),
  pe.embedding_json
FROM posts p
JOIN post_embeddings pe ON pe.post_id = p.id
WHERE p.status = 'PUBLISHED'
ORDER BY p.id;
`;
  return mysqlRows(sql).map(([id, categoryHex, titleHex, contentHex, tagsHex, embeddingJson]) => {
    const post = {
      id: Number(id),
      category: decodeHex(categoryHex),
      title: decodeHex(titleHex),
      content: decodeHex(contentHex),
      tags: decodeHex(tagsHex).split(',').map((tag) => tag.trim()).filter(Boolean),
      embedding: JSON.parse(embeddingJson),
    };

    return {
      ...post,
      terms: buildDocumentTerms(post.category, post.title, post.content, post.tags),
      guardTerms: buildGuardTerms(post.title, post.content, post.tags, DEFAULT_CONFIG.maxGuardTerms),
      subjectText: normalizeSynonyms(`${post.title} ${post.content} ${post.tags.join(' ')}`.toLowerCase()),
    };
  });
}

function loadChunkDocuments(posts) {
  const postsById = new Map(posts.map((post) => [post.id, post]));
  const sql = `
SELECT
  c.id,
  c.post_id,
  c.chunk_index,
  HEX(c.chunk_text),
  c.embedding_json
FROM post_embedding_chunks c
JOIN posts p ON p.id = c.post_id
WHERE p.status = 'PUBLISHED'
ORDER BY c.post_id, c.chunk_index;
`;
  return mysqlRows(sql)
    .map(([id, postId, chunkIndex, chunkTextHex, embeddingJson]) => {
      const post = postsById.get(Number(postId));
      if (!post) {
        return null;
      }
      const chunkText = decodeHex(chunkTextHex);
      return {
        id: Number(id),
        post,
        postId: Number(postId),
        chunkIndex: Number(chunkIndex),
        chunkText,
        embedding: JSON.parse(embeddingJson),
        terms: buildDocumentTerms(post.category, post.title, chunkText, post.tags),
      };
    })
    .filter(Boolean);
}

function mysqlRows(sql) {
  const output = execFileSync(
    'docker',
    [
      'exec',
      MYSQL_CONTAINER,
      'mysql',
      `-u${MYSQL_USER}`,
      `-p${MYSQL_PASSWORD}`,
      '--batch',
      '--raw',
      '--skip-column-names',
      MYSQL_DATABASE,
      '-e',
      sql,
    ],
    { encoding: 'utf8', maxBuffer: 1024 * 1024 * 1024 },
  );

  return output
    .trim()
    .split('\n')
    .filter(Boolean)
    .map((line) => line.split('\t'));
}

function decodeHex(hex) {
  if (!hex) {
    return '';
  }

  return Buffer.from(hex, 'hex').toString('utf8');
}

async function loadOrCreateQueryEmbeddingCache(cases) {
  const cachePath = resolve(EMBEDDING_CACHE_PATH);
  const existing = existsSync(cachePath)
    ? JSON.parse(readFileSync(cachePath, 'utf8'))
    : { model: embeddingModel(), items: {} };
  const cache = new Map(Object.entries(existing.items ?? {}));
  const missing = [];

  for (const evaluationCase of cases) {
    for (const queryVariant of QUERY_VARIANTS) {
      const text = queryVariant.buildText(evaluationCase);
      const key = cacheKey(queryVariant.id, evaluationCase.id, text);
      if (!cache.has(key)) {
        missing.push({ key, text });
      }
    }
  }

  if (missing.length > 0) {
    const embeddings = await createEmbeddings(missing.map((item) => item.text));
    missing.forEach((item, index) => cache.set(item.key, embeddings[index]));
    mkdirSync(dirname(cachePath), { recursive: true });
    writeFileSync(
      cachePath,
      `${JSON.stringify({ model: embeddingModel(), items: Object.fromEntries(cache) }, null, 2)}\n`,
      'utf8',
    );
  }

  return cache;
}

async function createEmbeddings(texts) {
  const baseUrl = process.env.OPENAI_BASE_URL ?? 'https://api.openai.com/v1';
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}/embeddings`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      model: embeddingModel(),
      input: texts,
    }),
  });
  const bodyText = await response.text();

  if (!response.ok) {
    throw new Error(`OpenAI embedding request failed: ${response.status} ${bodyText}`);
  }

  const body = JSON.parse(bodyText);
  return body.data
    .sort((left, right) => left.index - right.index)
    .map((item) => item.embedding);
}

function embeddingModel() {
  return process.env.OPENAI_EMBEDDING_MODEL ?? 'text-embedding-3-small';
}

function cacheKey(queryVariantId, caseId, text) {
  return `${embeddingModel()}::${queryVariantId}::${caseId}::${simpleHash(text)}`;
}

function simpleHash(text) {
  let hash = 0;
  for (let index = 0; index < text.length; index++) {
    hash = (Math.imul(31, hash) + text.charCodeAt(index)) | 0;
  }

  return (hash >>> 0).toString(16);
}

function attachRelevantPostIds(evaluationCase, posts) {
  if (evaluationCase.relevantPostIds) {
    return evaluationCase;
  }

  const relevantPostIds = posts
    .filter((post) => isSubjectRelevant(evaluationCase, post))
    .map((post) => post.id);

  return {
    ...evaluationCase,
    relevantPostIds,
  };
}

function isSubjectRelevant(evaluationCase, post) {
  const subjectText = post.subjectText;
  const requiredMatched = evaluationCase.requiredTerms.every((term) =>
    subjectText.includes(normalizeSynonyms(term.toLowerCase())),
  );
  if (!requiredMatched) {
    return false;
  }

  const anyMatches = evaluationCase.anyTerms.filter((term) =>
    subjectText.includes(normalizeSynonyms(term.toLowerCase())),
  ).length;

  return anyMatches >= evaluationCase.minimumAnyTermMatches;
}

function evaluateGroup(context, group) {
  const variants = group.variants.map((variant) => evaluateVariant(context, variant));
  const bestByNdcg = [...variants].sort((left, right) =>
    right.average[`ndcg@${K}`] - left.average[`ndcg@${K}`]
      || right.average[`precision@${K}`] - left.average[`precision@${K}`],
  )[0];

  return {
    id: group.id,
    label: group.label,
    description: group.description,
    variants,
    bestByNdcg,
  };
}

function evaluateVariant(context, variant) {
  const config = {
    ...DEFAULT_CONFIG,
    ...variant.config,
  };
  const queryVariant = context.queryVariantsById.get(config.queryVariant);
  const cases = context.cases.map((evaluationCase) =>
    evaluateCase(context, evaluationCase, queryVariant, config),
  );

  return {
    id: variant.id,
    label: variant.label,
    config,
    average: averageCaseResults(cases),
    cases,
  };
}

function evaluateCase(context, evaluationCase, queryVariant, config) {
  const queryText = queryVariant.buildText(evaluationCase);
  const queryEmbedding = context.queryEmbeddingCache.get(cacheKey(queryVariant.id, evaluationCase.id, queryText)) ?? [];
  const queryTerms = buildQueryTerms(queryText, config.maxQueryTerms);
  const guardTerms = buildQueryTerms(queryText, config.maxGuardTerms);
  const candidatePosts = filterPostsByMetadata(context.posts, evaluationCase, config.useMetadata);
  const candidateChunks = filterChunksByPostIds(
    context.chunks,
    new Set(candidatePosts.map((post) => post.id)),
  );
  const postResults = scorePosts(candidatePosts, queryEmbedding, queryTerms, guardTerms, config);
  const chunkScoresByPostId = config.useChunks
    ? scoreChunks(candidateChunks, queryEmbedding, queryTerms, guardTerms, config)
    : new Map();
  const retrievedPosts = postResults
    .map((result) => applyChunkEvidence(result, chunkScoresByPostId, config.chunkWeight))
    .filter((result) => config.rankedThreshold == null || result.score >= config.rankedThreshold)
    .sort((left, right) => right.score - left.score || left.post.id - right.post.id)
    .slice(0, K)
    .map((result) => ({
      postId: result.post.id,
      title: result.post.title,
      category: result.post.category,
      score: roundMetric(result.score),
    }));
  const metrics = evaluateRetrievedPosts(retrievedPosts, evaluationCase.relevantPostIds);

  return {
    id: evaluationCase.id,
    suite: evaluationCase.suite,
    name: evaluationCase.name,
    relevantTotal: evaluationCase.relevantPostIds.length,
    [`hit@${K}`]: metrics.hit,
    [`recall@${K}`]: roundMetric(metrics.recall),
    [`precision@${K}`]: roundMetric(metrics.precision),
    [`mrr@${K}`]: roundMetric(metrics.mrr),
    [`ndcg@${K}`]: roundMetric(metrics.ndcg),
    hits: `${metrics.hitCount}/${evaluationCase.relevantPostIds.length}`,
    retrievedPosts,
  };
}

function filterPostsByMetadata(posts, evaluationCase, useMetadata) {
  if (!useMetadata || !hasMetadataFilter(evaluationCase)) {
    return posts;
  }

  const stages = metadataStages(evaluationCase);
  for (const stage of stages) {
    const filtered = posts.filter((post) => metadataMatches(post, stage));
    if (filtered.length > 0) {
      return filtered;
    }
  }

  return posts;
}

function filterChunksByPostIds(chunks, postIds) {
  return chunks.filter((chunk) => postIds.has(chunk.postId));
}

function hasMetadataFilter(evaluationCase) {
  return (evaluationCase.category && evaluationCase.category !== 'All') || evaluationCase.tags.length > 0;
}

function metadataStages(evaluationCase) {
  const category = evaluationCase.category && evaluationCase.category !== 'All'
    ? evaluationCase.category
    : null;
  const tags = evaluationCase.tags ?? [];
  const stages = [];

  if (category && tags.length > 0) {
    stages.push({ category, tags });
  }
  if (category) {
    stages.push({ category, tags: [] });
  }
  if (tags.length > 0) {
    stages.push({ category: null, tags });
  }
  stages.push({ category: null, tags: [] });

  return stages;
}

function metadataMatches(post, metadata) {
  const categoryMatches = !metadata.category || post.category === metadata.category;
  const tagsMatch = metadata.tags.length === 0
    || metadata.tags.some((tag) => post.tags.map((postTag) => postTag.toLowerCase()).includes(tag.toLowerCase()));

  return categoryMatches && tagsMatch;
}

function scorePosts(posts, queryEmbedding, queryTerms, guardTerms, config) {
  const stats = createBm25Stats(posts.map((post) => post.terms), queryTerms);
  const scored = posts
    .map((post) => scoreDocument(post, queryEmbedding, queryTerms, guardTerms, stats, config))
    .filter((document) => passesPrefilter(document, queryTerms, config));

  return rankScoredDocuments(scored, config);
}

function scoreChunks(chunks, queryEmbedding, queryTerms, guardTerms, config) {
  const stats = createBm25Stats(chunks.map((chunk) => chunk.terms), queryTerms);
  const scored = chunks
    .map((chunk) => scoreDocument(chunk, queryEmbedding, queryTerms, guardTerms, stats, config))
    .filter((document) => passesPrefilter(document, queryTerms, config));
  const ranked = rankScoredDocuments(scored, config);
  const bestByPostId = new Map();

  for (const result of ranked) {
    const postId = result.post.id;
    const previous = bestByPostId.get(postId);
    if (previous == null || result.score > previous) {
      bestByPostId.set(postId, result.score);
    }
  }

  return bestByPostId;
}

function scoreDocument(document, queryEmbedding, queryTerms, guardTerms, stats, config) {
  const vectorScore = config.useVector && queryEmbedding.length > 0
    ? cosineSimilarity(queryEmbedding, document.embedding)
    : 0;
  const bm25 = config.useBm25
    ? bm25Score(document.terms, queryTerms, stats, config.bm25K1, config.bm25B)
    : 0;
  const keyword = keywordAlignment(document.guardTerms ?? document.post?.guardTerms ?? document.terms, guardTerms);

  return {
    document,
    vectorScore,
    bm25Score: bm25,
    keywordScore: keyword,
  };
}

function passesPrefilter(scored, queryTerms, config) {
  if (queryTerms.length === 0 || !config.useBm25) {
    return !config.useVector || scored.vectorScore >= config.vectorThreshold;
  }

  const hybridScore = clamp01(
    scored.vectorScore * config.vectorWeight + scored.bm25Score * config.bm25Weight,
  );

  return hybridScore >= config.hybridThreshold;
}

function rankScoredDocuments(scoredDocuments, config) {
  if (config.useRrf) {
    const vectorRanks = rankBy(scoredDocuments, 'vectorScore', config.useVector);
    const bm25Ranks = rankBy(scoredDocuments, 'bm25Score', config.useBm25);

    return scoredDocuments.map((scored) => {
      const fusion = rrfScore(scored.document.post?.id ?? scored.document.id, vectorRanks, bm25Ranks, config.rrfK);
      return toSearchResult(scored, fusion, config);
    });
  }

  return scoredDocuments.map((scored) => {
    const weightSum =
      (config.useVector ? config.vectorWeight : 0)
      + (config.useBm25 ? config.bm25Weight : 0);
    const vectorContribution = config.useVector ? scored.vectorScore * config.vectorWeight : 0;
    const bm25Contribution = config.useBm25 ? scored.bm25Score * config.bm25Weight : 0;
    const base = weightSum === 0 ? 0 : (vectorContribution + bm25Contribution) / weightSum;

    return toSearchResult(scored, base, config);
  });
}

function toSearchResult(scored, baseScore, config) {
  const keywordApplied = clamp01(
    baseScore * (1 - config.keywordWeight) + scored.keywordScore * config.keywordWeight,
  );
  const post = scored.document.post ?? scored.document;

  return {
    post,
    score: keywordApplied,
    vectorScore: scored.vectorScore,
    bm25Score: scored.bm25Score,
    keywordScore: scored.keywordScore,
  };
}

function applyChunkEvidence(result, chunkScoresByPostId, chunkWeight) {
  if (chunkWeight === 0) {
    return result;
  }

  const chunkScore = chunkScoresByPostId.get(result.post.id);
  if (chunkScore == null) {
    return result;
  }

  return {
    ...result,
    score: result.score * (1 - chunkWeight) + chunkScore * chunkWeight,
  };
}

function rankBy(scoredDocuments, scoreField, enabled) {
  if (!enabled) {
    return new Map();
  }

  return new Map(
    [...scoredDocuments]
      .filter((scored) => scored[scoreField] > 0)
      .sort((left, right) =>
        right[scoreField] - left[scoreField]
        || (left.document.post?.id ?? left.document.id) - (right.document.post?.id ?? right.document.id),
      )
      .map((scored, index) => [scored.document.post?.id ?? scored.document.id, index + 1]),
  );
}

function rrfScore(postId, vectorRanks, bm25Ranks, rrfK) {
  const raw = reciprocalRankScore(vectorRanks.get(postId), rrfK)
    + reciprocalRankScore(bm25Ranks.get(postId), rrfK);
  const maxPossible = 2 / (rrfK + 1);

  return clamp01(raw / maxPossible);
}

function reciprocalRankScore(rank, rrfK) {
  if (rank == null) {
    return 0;
  }

  return 1 / (rrfK + rank);
}

function buildDocumentTerms(category, title, content, tags) {
  return [
    ...tokenize(category),
    ...tokenize(title),
    ...tokenize(title),
    ...tokenize(Array.isArray(tags) ? tags.join(' ') : ''),
    ...tokenize(content),
  ];
}

function buildQueryTerms(text, limit) {
  return [...new Set(tokenize(text))].slice(0, limit);
}

function buildGuardTerms(title, content, tags, limit) {
  return buildQueryTerms([
    title,
    Array.isArray(tags) ? tags.join(' ') : '',
    content,
  ].join(' '), limit);
}

function tokenize(text) {
  return normalizeSynonyms(String(text ?? '').toLowerCase())
    .split(/[^0-9a-z가-힣#+.]+/u)
    .map((term) => term.trim())
    .filter((term) => term.length >= 2)
    .map(stripKoreanSuffix)
    .filter((term) => term.length >= 2)
    .filter((term) => !STOP_WORDS.has(term));
}

function normalizeSynonyms(text) {
  return text
    .replaceAll('깃허브', 'github')
    .replaceAll('깃헙', 'github')
    .replaceAll('깃 액션', 'github actions')
    .replaceAll('스프링부트', 'spring boot')
    .replaceAll('스프링 부트', 'spring boot')
    .replaceAll('리액트', 'react')
    .replaceAll('레디스', 'redis')
    .replaceAll('쿠버네티스', 'kubernetes')
    .replaceAll('도커', 'docker')
    .replaceAll('언더볼팅', 'undervolt');
}

function stripKoreanSuffix(term) {
  return term
    .replace(/(으로|에서|에게|보다|처럼|부터|까지|과는|와는|에는|으로는)$/u, '')
    .replace(/(을|를|이|가|은|는|와|과|도|만|에|의|로)$/u, '');
}

function createBm25Stats(documents) {
  const documentFrequencies = new Map();
  let totalLength = 0;

  for (const terms of documents) {
    totalLength += terms.length;
    for (const term of new Set(terms)) {
      documentFrequencies.set(term, (documentFrequencies.get(term) ?? 0) + 1);
    }
  }

  return {
    documentCount: documents.length,
    averageLength: documents.length === 0 ? 0 : totalLength / documents.length,
    documentFrequencies,
  };
}

function bm25Score(documentTerms, queryTerms, stats, k1, b) {
  if (queryTerms.length === 0 || stats.documentCount === 0 || documentTerms.length === 0) {
    return 0;
  }

  const termFrequencies = new Map();
  for (const term of documentTerms) {
    termFrequencies.set(term, (termFrequencies.get(term) ?? 0) + 1);
  }

  let rawScore = 0;
  for (const queryTerm of queryTerms) {
    const termFrequency = termFrequencies.get(queryTerm) ?? 0;
    const documentFrequency = stats.documentFrequencies.get(queryTerm) ?? 0;
    if (termFrequency === 0 || documentFrequency === 0) {
      continue;
    }

    const idf = Math.log(1 + (stats.documentCount - documentFrequency + 0.5) / (documentFrequency + 0.5));
    const lengthNormalization =
      1 - b + b * (documentTerms.length / Math.max(stats.averageLength, 1));
    const saturatedTermFrequency =
      (termFrequency * (k1 + 1)) / (termFrequency + k1 * lengthNormalization);
    rawScore += idf * saturatedTermFrequency;
  }

  return 1 - Math.exp(-rawScore);
}

function keywordAlignment(candidateTerms, guardTerms) {
  if (candidateTerms.length === 0 || guardTerms.length === 0) {
    return 0;
  }

  const candidateSet = new Set(candidateTerms);
  const matched = guardTerms.filter((term) => candidateSet.has(term)).length;

  return matched / guardTerms.length;
}

function cosineSimilarity(left, right) {
  if (!left || !right || left.length !== right.length || left.length === 0) {
    return 0;
  }

  let dot = 0;
  let leftNorm = 0;
  let rightNorm = 0;
  for (let index = 0; index < left.length; index++) {
    dot += left[index] * right[index];
    leftNorm += left[index] * left[index];
    rightNorm += right[index] * right[index];
  }

  if (leftNorm === 0 || rightNorm === 0) {
    return 0;
  }

  return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
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

function roundMetric(value) {
  return Number(value.toFixed(4));
}

function clamp01(value) {
  return Math.max(0, Math.min(1, value));
}

function formatTags(tags) {
  if (!Array.isArray(tags) || tags.length === 0) {
    return 'None';
  }

  return tags.join(', ');
}

function buildQueryCompositionGroup() {
  return {
    id: 'query-composition',
    label: 'Query Composition',
    description: '검색 쿼리에 제목/본문/태그/템플릿을 어떻게 넣을지 비교',
    variants: QUERY_VARIANTS.map((queryVariant) => ({
      id: queryVariant.id,
      label: queryVariant.label,
      config: { queryVariant: queryVariant.id },
    })),
  };
}

function buildVectorBm25WeightGroup() {
  return {
    id: 'vector-bm25-weight',
    label: 'Vector/BM25 Weight',
    description: '벡터 의미 검색과 BM25 단어 검색의 가중치 비교',
    variants: [
      [1.0, 0.0],
      [0.8, 0.2],
      [0.65, 0.35],
      [0.5, 0.5],
      [0.35, 0.65],
      [0.2, 0.8],
      [0.0, 1.0],
    ].map(([vectorWeight, bm25Weight]) => ({
      id: `v${vectorWeight}-b${bm25Weight}`,
      label: `Vector ${vectorWeight} / BM25 ${bm25Weight}`,
      config: { vectorWeight, bm25Weight, useRrf: false },
    })),
  };
}

function buildChunkEvidenceWeightGroup() {
  return {
    id: 'chunk-evidence-weight',
    label: 'Chunk Evidence Weight',
    description: '청크 점수를 최종 게시글 점수에 얼마나 반영할지 비교',
    variants: [0, 0.005, 0.01, 0.02, 0.03, 0.05, 0.1].map((chunkWeight) => ({
      id: `chunk-${chunkWeight}`,
      label: `Chunk ${roundMetric(chunkWeight * 100)}%`,
      config: { chunkWeight },
    })),
  };
}

function buildRankedThresholdGroup() {
  return {
    id: 'ranked-threshold',
    label: 'Ranked Result Threshold',
    description: '최종 점수 컷오프를 어느 정도로 둘지 비교',
    variants: [null, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9].map((rankedThreshold) => ({
      id: `ranked-${rankedThreshold ?? 'none'}`,
      label: rankedThreshold == null ? 'No ranked threshold' : `Ranked >= ${rankedThreshold}`,
      config: { rankedThreshold },
    })),
  };
}

function buildVectorThresholdGroup() {
  return {
    id: 'vector-threshold',
    label: 'Vector Threshold',
    description: '벡터 중심 검색일 때 최소 cosine 기준 비교',
    variants: [0.25, 0.35, 0.45, 0.55, 0.65].map((vectorThreshold) => ({
      id: `vector-threshold-${vectorThreshold}`,
      label: `Vector >= ${vectorThreshold}`,
      config: { vectorThreshold },
    })),
  };
}

function buildHybridThresholdGroup() {
  return {
    id: 'hybrid-threshold',
    label: 'Hybrid Threshold',
    description: 'Vector/BM25 hybrid prefilter 기준 비교',
    variants: [0.2, 0.3, 0.38, 0.45, 0.55, 0.65].map((hybridThreshold) => ({
      id: `hybrid-threshold-${hybridThreshold}`,
      label: `Hybrid >= ${hybridThreshold}`,
      config: { hybridThreshold },
    })),
  };
}

function buildBm25ParameterGroup() {
  const variants = [];
  for (const bm25K1 of [0.8, 1.2, 1.6, 2.0]) {
    for (const bm25B of [0.25, 0.5, 0.75, 1.0]) {
      variants.push({
        id: `k1-${bm25K1}-b-${bm25B}`,
        label: `k1 ${bm25K1} / b ${bm25B}`,
        config: { bm25K1, bm25B },
      });
    }
  }

  return {
    id: 'bm25-parameters',
    label: 'BM25 Parameters',
    description: 'BM25 k1/b 파라미터 비교',
    variants,
  };
}

function buildRrfConstantGroup() {
  return {
    id: 'rrf-constant',
    label: 'RRF Constant',
    description: 'RRF rank constant가 순위 결합에 주는 영향 비교',
    variants: [10, 30, 60, 100, 200].map((rrfK) => ({
      id: `rrf-${rrfK}`,
      label: `RRF k=${rrfK}`,
      config: { rrfK },
    })),
  };
}

function buildQueryTermLimitGroup() {
  return {
    id: 'query-term-limit',
    label: 'Query Term Limit',
    description: '쿼리 핵심어/guard term 개수 제한 비교',
    variants: [
      [8, 4],
      [12, 6],
      [16, 8],
      [24, 8],
      [24, 12],
      [32, 16],
    ].map(([maxQueryTerms, maxGuardTerms]) => ({
      id: `query-${maxQueryTerms}-guard-${maxGuardTerms}`,
      label: `query ${maxQueryTerms} / guard ${maxGuardTerms}`,
      config: { maxQueryTerms, maxGuardTerms },
    })),
  };
}

function buildMetadataGroup() {
  return {
    id: 'metadata-filter',
    label: 'Metadata Filter',
    description: '카테고리/태그 완화 필터 사용 여부 비교',
    variants: [
      {
        id: 'metadata-off',
        label: 'Metadata off',
        config: { useMetadata: false },
      },
      {
        id: 'metadata-on',
        label: 'Metadata on',
        config: { useMetadata: true },
      },
    ],
  };
}

function buildCoreRetrieverGroup() {
  return {
    id: 'core-retriever',
    label: 'Core Retriever',
    description: '검색 조합 자체 비교',
    variants: [
      {
        id: 'vector-only',
        label: 'Vector only',
        config: {
          useVector: true,
          useBm25: false,
          useRrf: false,
          keywordWeight: 0,
          useChunks: false,
          rankedThreshold: null,
        },
      },
      {
        id: 'bm25-only',
        label: 'BM25 only',
        config: {
          useVector: false,
          useBm25: true,
          useRrf: false,
          vectorThreshold: 0,
          hybridThreshold: 0,
          keywordWeight: 0,
          useChunks: false,
          rankedThreshold: null,
        },
      },
      {
        id: 'weighted-hybrid',
        label: 'Vector + BM25 weighted',
        config: {
          useVector: true,
          useBm25: true,
          useRrf: false,
          keywordWeight: 0,
          useChunks: false,
          rankedThreshold: null,
        },
      },
      {
        id: 'rrf-hybrid',
        label: 'Vector + BM25 RRF',
        config: {
          useVector: true,
          useBm25: true,
          useRrf: true,
          keywordWeight: 0,
          useChunks: false,
          rankedThreshold: null,
        },
      },
      {
        id: 'rrf-keyword-metadata',
        label: 'RRF + keyword + metadata',
        config: {
          useVector: true,
          useBm25: true,
          useRrf: true,
          useMetadata: true,
          keywordWeight: 0.25,
          useChunks: false,
          rankedThreshold: null,
        },
      },
      {
        id: 'current-like',
        label: 'Current-like full hybrid',
        config: DEFAULT_CONFIG,
      },
    ],
  };
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
