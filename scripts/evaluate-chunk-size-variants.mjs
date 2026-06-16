import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const MYSQL_CONTAINER = process.env.PROJECT_ALPHA_MYSQL_CONTAINER ?? 'project-alpha-mysql';
const MYSQL_USER = process.env.PROJECT_ALPHA_MYSQL_USER ?? 'alpha';
const MYSQL_PASSWORD = process.env.PROJECT_ALPHA_MYSQL_PASSWORD ?? 'alpha_password';
const MYSQL_DATABASE = process.env.PROJECT_ALPHA_MYSQL_DATABASE ?? 'project_alpha';
const OUTPUT_PATH =
  process.env.PROJECT_ALPHA_CHUNK_EVAL_OUTPUT ??
  'backend/build/chunk-size-variant-evaluation.json';
const K = Number(process.env.PROJECT_ALPHA_RAG_EVAL_K ?? 5);

const VARIANTS = [
  { id: 'extreme-s-300-45', label: 'EXTREME-S 300/45', maxLength: 300, overlap: 45 },
  { id: 's-450-68', label: 'S 450/68', maxLength: 450, overlap: 68 },
  { id: 's-600-90', label: 'S 600/90', maxLength: 600, overlap: 90 },
  { id: 'm-750-112', label: 'M 750/112', maxLength: 750, overlap: 112 },
  { id: 'm-900-135', label: 'M 900/135', maxLength: 900, overlap: 135 },
  { id: 'm-1050-158', label: 'M 1050/158', maxLength: 1050, overlap: 158 },
  { id: 'base-1200-180', label: 'BASE 1200/180', maxLength: 1200, overlap: 180 },
  { id: 'l-1350-203', label: 'L 1350/203', maxLength: 1350, overlap: 203 },
  { id: 'l-1500-225', label: 'L 1500/225', maxLength: 1500, overlap: 225 },
  { id: 'l-1800-270', label: 'L 1800/270', maxLength: 1800, overlap: 270 },
  { id: 'xl-2100-315', label: 'XL 2100/315', maxLength: 2100, overlap: 315 },
  { id: 'extreme-l-2400-360', label: 'EXTREME-L 2400/360', maxLength: 2400, overlap: 360 },
];

const EVALUATION_CASES = [
  {
    id: 'rag-llm-service',
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
    id: 'github-actions-deploy',
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
    id: 'kubernetes-eks',
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
    id: 'redis-cache-session',
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
    id: 'react-frontend',
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
    id: 'airflow-data-pipeline',
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
]);

function main() {
  const posts = loadPosts();
  const postDocuments = posts.map((post) => ({
    post,
    terms: buildTerms(post.category, post.title, post.content, post.tags),
  }));
  const postStats = createBm25Stats(postDocuments.map((document) => document.terms));
  const results = VARIANTS.map((variant) =>
    evaluateVariant(variant, posts, postDocuments, postStats),
  );
  const output = {
    generatedAt: new Date().toISOString(),
    note:
      'Offline chunk-size sensitivity test. It does not regenerate OpenAI/Qdrant chunk embeddings. It compares chunk size using the same DB posts and BM25/keyword-based chunk evidence.',
    k: K,
    postCount: posts.length,
    variants: results,
  };

  const absoluteOutputPath = resolve(OUTPUT_PATH);
  mkdirSync(dirname(absoluteOutputPath), { recursive: true });
  writeFileSync(absoluteOutputPath, `${JSON.stringify(output, null, 2)}\n`, 'utf8');

  console.table(
    results.map((result) => ({
      variant: result.label,
      chunks: result.chunkCount,
      avgChunks: result.averageChunksPerPost,
      chunkP: result.chunkOnlyAverage[`precision@${K}`],
      chunkR: result.chunkOnlyAverage[`recall@${K}`],
      chunkMRR: result.chunkOnlyAverage[`mrr@${K}`],
      chunkNDCG: result.chunkOnlyAverage[`ndcg@${K}`],
      hybridP: result.postPlusChunkAverage[`precision@${K}`],
      hybridR: result.postPlusChunkAverage[`recall@${K}`],
      hybridMRR: result.postPlusChunkAverage[`mrr@${K}`],
      hybridNDCG: result.postPlusChunkAverage[`ndcg@${K}`],
    })),
  );
  console.log(`Wrote ${absoluteOutputPath}`);
}

function loadPosts() {
  const sql = `
SELECT
  p.id,
  HEX(p.category),
  HEX(p.title),
  HEX(p.content),
  HEX(COALESCE(GROUP_CONCAT(t.name ORDER BY t.name SEPARATOR ','), ''))
FROM posts p
LEFT JOIN post_tags pt ON pt.post_id = p.id
LEFT JOIN tags t ON t.id = pt.tag_id
WHERE p.status = 'PUBLISHED'
GROUP BY p.id, p.category, p.title, p.content
ORDER BY p.id;
`;
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
    { encoding: 'utf8', maxBuffer: 256 * 1024 * 1024 },
  );

  return output
    .trim()
    .split('\n')
    .filter(Boolean)
    .map((line) => {
      const [id, categoryHex, titleHex, contentHex, tagsHex] = line.split('\t');
      return {
        id: Number(id),
        category: decodeHex(categoryHex),
        title: decodeHex(titleHex),
        content: decodeHex(contentHex),
        tags: decodeHex(tagsHex)
          .split(',')
          .map((tag) => tag.trim())
          .filter(Boolean),
      };
    });
}

function decodeHex(hex) {
  if (!hex) {
    return '';
  }

  return Buffer.from(hex, 'hex').toString('utf8');
}

function evaluateVariant(variant, posts, postDocuments, postStats) {
  const chunkDocuments = posts.flatMap((post) =>
    splitText(post.content, variant.maxLength, variant.overlap).map((chunk, index) => ({
      post,
      chunkIndex: index,
      chunkText: chunk,
      terms: buildTerms(post.category, post.title, chunk, post.tags),
    })),
  );
  const chunkStats = createBm25Stats(chunkDocuments.map((document) => document.terms));
  const chunkOnlyCases = [];
  const postPlusChunkCases = [];

  for (const evaluationCase of EVALUATION_CASES) {
    const queryTerms = buildTerms(
      evaluationCase.query.category,
      evaluationCase.query.title,
      evaluationCase.query.content,
      evaluationCase.query.tags,
    );
    const guardTerms = queryTerms.slice(0, 12);
    const chunkResults = searchChunkOnly(
      chunkDocuments,
      chunkStats,
      queryTerms,
      guardTerms,
    );
    const postPlusChunkResults = searchPostPlusChunk(
      postDocuments,
      postStats,
      chunkResults.scoresByPostId,
      queryTerms,
      guardTerms,
    );

    chunkOnlyCases.push(toCaseResult(evaluationCase, chunkResults.posts));
    postPlusChunkCases.push(toCaseResult(evaluationCase, postPlusChunkResults));
  }

  return {
    id: variant.id,
    label: variant.label,
    maxLength: variant.maxLength,
    overlap: variant.overlap,
    chunkCount: chunkDocuments.length,
    averageChunksPerPost: roundMetric(chunkDocuments.length / posts.length),
    chunkOnlyAverage: averageCaseResults(chunkOnlyCases),
    postPlusChunkAverage: averageCaseResults(postPlusChunkCases),
    chunkOnlyCases,
    postPlusChunkCases,
  };
}

function searchChunkOnly(chunkDocuments, chunkStats, queryTerms, guardTerms) {
  const rawScores = chunkDocuments.map((document) => {
    const bm25 = bm25Score(queryTerms, document.terms, chunkStats);
    const keyword = keywordAlignment(guardTerms, document.terms);
    return {
      post: document.post,
      score: bm25 + keyword * 2,
      chunkIndex: document.chunkIndex,
    };
  });
  const normalizedScores = normalizeScores(rawScores);
  const bestByPostId = new Map();

  for (const scored of normalizedScores) {
    const previous = bestByPostId.get(scored.post.id);
    if (!previous || scored.score > previous.score) {
      bestByPostId.set(scored.post.id, scored);
    }
  }

  const posts = [...bestByPostId.values()]
    .sort((left, right) => right.score - left.score || left.post.id - right.post.id)
    .slice(0, K)
    .map((result) => toRetrievedPost(result.post, result.score));
  const scoresByPostId = new Map(
    [...bestByPostId.values()].map((result) => [result.post.id, result.score]),
  );

  return { posts, scoresByPostId };
}

function searchPostPlusChunk(postDocuments, postStats, chunkScoresByPostId, queryTerms, guardTerms) {
  const rawPostScores = postDocuments.map((document) => {
    const bm25 = bm25Score(queryTerms, document.terms, postStats);
    const keyword = keywordAlignment(guardTerms, document.terms);
    return {
      post: document.post,
      score: bm25 + keyword * 2,
    };
  });
  const normalizedPostScores = normalizeScores(rawPostScores);

  return normalizedPostScores
    .map((result) => ({
      post: result.post,
      score: result.score + (chunkScoresByPostId.get(result.post.id) ?? 0) * 0.01,
    }))
    .sort((left, right) => right.score - left.score || left.post.id - right.post.id)
    .slice(0, K)
    .map((result) => toRetrievedPost(result.post, result.score));
}

function toRetrievedPost(post, score) {
  return {
    postId: post.id,
    title: post.title,
    category: post.category,
    score: roundMetric(score),
  };
}

function splitText(content, maxLength, overlap) {
  const normalizedContent = normalizeContent(content);
  if (!normalizedContent.trim()) {
    return [];
  }

  const paragraphs = normalizedContent
    .split(/\n\s*\n+/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean);
  const chunks = [];
  let currentChunk = '';
  let previousChunkText = '';

  for (const paragraph of paragraphs) {
    if (paragraph.length > maxLength) {
      previousChunkText = flushCurrentChunk(chunks, currentChunk, previousChunkText);
      currentChunk = '';
      previousChunkText = splitLongParagraph(paragraph, chunks, maxLength, overlap);
      continue;
    }

    if (!currentChunk) {
      currentChunk = appendWithOverlap('', previousChunkText, paragraph, maxLength, overlap);
      continue;
    }

    const nextLength = currentChunk.length + 2 + paragraph.length;
    if (nextLength <= maxLength) {
      currentChunk = `${currentChunk}\n\n${paragraph}`;
      continue;
    }

    previousChunkText = flushCurrentChunk(chunks, currentChunk, previousChunkText);
    currentChunk = appendWithOverlap('', previousChunkText, paragraph, maxLength, overlap);
  }

  flushCurrentChunk(chunks, currentChunk, previousChunkText);

  return chunks;
}

function splitLongParagraph(paragraph, chunks, maxLength, overlap) {
  let previousChunkText = '';
  let start = 0;

  while (start < paragraph.length) {
    const end = Math.min(start + maxLength, paragraph.length);
    const chunkText = paragraph.slice(start, end).trim();

    if (chunkText) {
      chunks.push(chunkText);
      previousChunkText = chunkText;
    }

    if (end === paragraph.length) {
      break;
    }

    start = Math.max(end - overlap, start + 1);
  }

  return previousChunkText;
}

function flushCurrentChunk(chunks, currentChunk, previousChunkText) {
  const chunkText = currentChunk.trim();
  if (!chunkText) {
    return previousChunkText;
  }

  chunks.push(chunkText);
  return chunkText;
}

function appendWithOverlap(currentChunk, previousChunkText, paragraph, maxLength, overlap) {
  const overlapText = trailingOverlap(previousChunkText, overlap);
  if (overlapText && overlapText.length + 2 + paragraph.length <= maxLength) {
    return `${currentChunk}${overlapText}\n\n${paragraph}`;
  }

  return `${currentChunk}${paragraph}`;
}

function trailingOverlap(text, overlap) {
  if (overlap === 0 || !text || !text.trim()) {
    return '';
  }

  const normalizedText = text.trim();
  if (normalizedText.length <= overlap) {
    return normalizedText;
  }

  return normalizedText.slice(normalizedText.length - overlap).trim();
}

function normalizeContent(content) {
  return String(content ?? '')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .split('\n')
    .map((line) => line.replace(/\s+/g, ' ').trim())
    .join('\n')
    .trim();
}

function buildTerms(category, title, content, tags) {
  return [
    ...tokenize(category),
    ...tokenize(title),
    ...tokenize(title),
    ...tokenize(Array.isArray(tags) ? tags.join(' ') : ''),
    ...tokenize(content),
  ];
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
    .replaceAll('도커', 'docker');
}

function stripKoreanSuffix(term) {
  return term
    .replace(/(으로|에서|에게|보다|처럼|부터|까지|과는|와는|에는|으로는)$/u, '')
    .replace(/(을|를|이|가|은|는|와|과|도|만|에|의|로)$/u, '');
}

function createBm25Stats(documents) {
  const documentFrequencies = new Map();
  let totalLength = 0;

  for (const documentTerms of documents) {
    totalLength += documentTerms.length;
    for (const term of new Set(documentTerms)) {
      documentFrequencies.set(term, (documentFrequencies.get(term) ?? 0) + 1);
    }
  }

  return {
    documentCount: documents.length,
    averageLength: documents.length === 0 ? 0 : totalLength / documents.length,
    documentFrequencies,
  };
}

function bm25Score(queryTerms, documentTerms, stats) {
  if (queryTerms.length === 0 || documentTerms.length === 0 || stats.documentCount === 0) {
    return 0;
  }

  const termFrequencies = new Map();
  for (const term of documentTerms) {
    termFrequencies.set(term, (termFrequencies.get(term) ?? 0) + 1);
  }

  let score = 0;
  const k1 = 1.2;
  const b = 0.75;
  const docLength = documentTerms.length;

  for (const queryTerm of new Set(queryTerms)) {
    const tf = termFrequencies.get(queryTerm) ?? 0;
    if (tf === 0) {
      continue;
    }

    const df = stats.documentFrequencies.get(queryTerm) ?? 0;
    const idf = Math.log(1 + (stats.documentCount - df + 0.5) / (df + 0.5));
    const denominator =
      tf + k1 * (1 - b + b * (docLength / Math.max(stats.averageLength, 1)));
    score += idf * ((tf * (k1 + 1)) / denominator);
  }

  return score;
}

function keywordAlignment(guardTerms, documentTerms) {
  if (guardTerms.length === 0 || documentTerms.length === 0) {
    return 0;
  }

  const documentTermSet = new Set(documentTerms);
  const uniqueGuardTerms = [...new Set(guardTerms)];
  const matched = uniqueGuardTerms.filter((term) => documentTermSet.has(term)).length;

  return matched / uniqueGuardTerms.length;
}

function normalizeScores(scoredItems) {
  const maxScore = Math.max(...scoredItems.map((item) => item.score), 0);
  if (maxScore <= 0) {
    return scoredItems.map((item) => ({ ...item, score: 0 }));
  }

  return scoredItems.map((item) => ({ ...item, score: item.score / maxScore }));
}

function toCaseResult(evaluationCase, retrievedPosts) {
  const metrics = evaluateRetrievedPosts(retrievedPosts, evaluationCase.relevantPostIds);

  return {
    id: evaluationCase.id,
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

function averageCaseResults(caseResults) {
  return {
    cases: caseResults.length,
    [`hit@${K}`]: roundMetric(average(caseResults, `hit@${K}`)),
    [`recall@${K}`]: roundMetric(average(caseResults, `recall@${K}`)),
    [`precision@${K}`]: roundMetric(average(caseResults, `precision@${K}`)),
    [`mrr@${K}`]: roundMetric(average(caseResults, `mrr@${K}`)),
    [`ndcg@${K}`]: roundMetric(average(caseResults, `ndcg@${K}`)),
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

main();
