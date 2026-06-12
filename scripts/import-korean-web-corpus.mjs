const API_BASE_URL = process.env.PROJECT_ALPHA_API_BASE_URL ?? 'http://localhost:8080';
const USERNAME = process.env.PROJECT_ALPHA_SEED_USERNAME ?? 'korean-seed';
const PASSWORD = process.env.PROJECT_ALPHA_SEED_PASSWORD ?? 'korean-seed-password';
const TARGET_TOTAL = Number(process.env.PROJECT_ALPHA_WEB_CORPUS_TARGET_TOTAL ?? 1200);
const TARGET_PER_CATEGORY = Number(process.env.PROJECT_ALPHA_WEB_CORPUS_TARGET_PER_CATEGORY ?? 200);
const CANDIDATE_LIMIT = Number(process.env.PROJECT_ALPHA_WEB_CORPUS_CANDIDATE_LIMIT ?? 3500);
const CRAWL_CONCURRENCY = Number(process.env.PROJECT_ALPHA_WEB_CORPUS_CRAWL_CONCURRENCY ?? 8);
const MIN_TEXT_LENGTH = Number(process.env.PROJECT_ALPHA_WEB_CORPUS_MIN_TEXT_LENGTH ?? 250);
const DRY_RUN = process.env.PROJECT_ALPHA_WEB_CORPUS_DRY_RUN === 'true';

const CATEGORIES = ['Development', 'Learning', 'Project', 'Daily', 'Review', 'Briefing'];
const CATEGORY_QUOTAS = Object.fromEntries(
  CATEGORIES.map((category) => [category, TARGET_PER_CATEGORY]),
);

const SITEMAP_SOURCES = [
  {
    source: '우아한형제들 기술블로그',
    url: 'https://techblog.woowahan.com/sitemap.xml',
    include: (url) => /^https:\/\/techblog\.woowahan\.com\/\d+\/?$/.test(url),
  },
  {
    source: 'Hyperconnect Tech',
    url: 'https://hyperconnect.github.io/sitemap.xml',
    include: (url) => /^https:\/\/hyperconnect\.github\.io\/\d{4}\/\d{2}\/\d{2}\/.+\.html$/.test(url),
  },
  {
    source: 'Dev Sisters Tech',
    url: 'https://tech.devsisters.com/sitemap.xml',
    include: (url) => /^https:\/\/tech\.devsisters\.com\/posts\/.+\/?$/.test(url),
  },
  {
    source: '인프랩 기술블로그',
    url: 'https://tech.inflab.com/sitemap.xml',
    include: (url) => /^https:\/\/tech\.inflab\.com\/posts\/.+/.test(url) && !/\/posts\/(All|frontend|backend|devops|productivity)$/.test(url),
  },
  {
    source: '11번가 기술블로그',
    url: 'https://11st-tech.github.io/sitemap.xml',
    include: (url) => /^https:\/\/11st-tech\.github\.io\/\d{4}\/\d{2}\/\d{2}\/.+\/?$/.test(url),
  },
  {
    source: '다나와 기술블로그',
    url: 'https://danawalab.github.io/sitemap.xml',
    include: (url) => /^https:\/\/danawalab\.github\.io\/.+\/\d{4}\/\d{2}\/\d{2}\/.+\.html$/.test(url),
  },
  {
    source: '쏘카 기술블로그',
    url: 'https://tech.socarcorp.kr/sitemap.xml',
    include: (url) => /^https:\/\/tech\.socarcorp\.kr\/.+\/\d{4}\/\d{2}\/\d{2}\/.+\.html$/.test(url),
  },
  {
    source: 'AB180 Engineering',
    url: 'https://engineering.ab180.co/sitemap.xml',
    include: (url) => /^https:\/\/engineering\.ab180\.co\/stories\/.+/.test(url),
  },
];

const FEED_SOURCES = [
  {
    source: 'Toss Tech',
    url: 'https://toss.tech/rss.xml',
  },
  {
    source: 'Kakao Tech',
    url: 'https://tech.kakao.com/feed/',
  },
  {
    source: 'AWS 기술 블로그',
    url: 'https://aws.amazon.com/ko/blogs/tech/feed/',
  },
  {
    source: 'AWS 한국 블로그',
    url: 'https://aws.amazon.com/ko/blogs/korea/feed/',
  },
  {
    source: 'LINE Yahoo Tech',
    url: 'https://techblog.lycorp.co.jp/ko/feed/index.xml',
  },
  {
    source: 'Spoqa Tech',
    url: 'https://spoqa.github.io/atom.xml',
  },
  {
    source: '뱅크샐러드 기술블로그',
    url: 'https://blog.banksalad.com/rss.xml',
  },
  {
    source: '당근 기술블로그',
    url: 'https://medium.com/feed/daangn',
  },
  {
    source: '무신사 기술블로그',
    url: 'https://medium.com/feed/musinsa-tech',
  },
  {
    source: '요기요 기술블로그',
    url: 'https://techblog.yogiyo.co.kr/feed',
  },
  {
    source: '리디 기술블로그',
    url: 'https://ridicorp.com/story-category/tech-blog/feed/',
  },
  {
    source: '쏘카 기술블로그',
    url: 'https://tech.socarcorp.kr/feed',
  },
  {
    source: '왓챠 기술블로그',
    url: 'https://medium.com/feed/watcha',
  },
  {
    source: '직방 기술블로그',
    url: 'https://medium.com/feed/zigbang',
  },
  {
    source: '컬리 기술블로그',
    url: 'https://helloworld.kurly.com/rss.xml',
  },
  {
    source: '29CM 기술블로그',
    url: 'https://medium.com/feed/29cm',
  },
  {
    source: '원티드 기술블로그',
    url: 'https://medium.com/feed/wantedjobs',
  },
];

const JSON_SOURCES = [
  {
    source: 'NAVER D2',
    url: 'https://d2.naver.com/api/v1/contents?categoryId=&page=0&size=1000',
    extract: (json) =>
      (json.content ?? [])
        .map((item) => ({
          source: 'NAVER D2',
          url: item.url ? `https://d2.naver.com${item.url}` : '',
          fallbackTitle: item.postTitle ?? '',
          fallbackText: stripHtml(item.postHtml ?? ''),
        }))
        .filter((item) => item.url),
  },
];

const KEYWORD_TAGS = [
  ['React', /react|리액트/i],
  ['Spring', /spring|스프링/i],
  ['Java', /java|자바/i],
  ['JavaScript', /javascript|자바스크립트|typescript|타입스크립트/i],
  ['Backend', /백엔드|서버|api|database|mysql|db|데이터베이스/i],
  ['Frontend', /프론트|ui|ux|browser|브라우저/i],
  ['Cloud', /aws|cloud|클라우드|ec2|s3|vpc|kubernetes|docker/i],
  ['AI', /ai|llm|rag|embedding|임베딩|머신러닝|machine learning/i],
  ['Performance', /성능|최적화|latency|scale|스케일|처리량/i],
  ['Security', /보안|인증|권한|jwt|oauth|security/i],
  ['Data', /데이터|분석|data|pipeline|warehouse/i],
  ['Testing', /테스트|test|qa/i],
];

const CATEGORY_KEYWORDS = {
  Development:
    /개발|프론트|백엔드|서버|클라이언트|API|React|Spring|Java|Node|TypeScript|JavaScript|Kotlin|Python|MySQL|DB|데이터베이스|Docker|Kubernetes|인프라|아키텍처|테스트|성능|배포/i,
  Learning:
    /학습|이해|가이드|튜토리얼|시작|소개|알아보기|사용법|정리|기초|입문|원리|개념|교육|세미나|스터디|방법/i,
  Project:
    /개발기|구축|적용|도입|전환|마이그레이션|자동화|프로젝트|사례|서비스|플랫폼|시스템|개선|실험|만들/i,
  Daily:
    /문화|커리어|일하는|조직|팀|협업|채용|성장|인터뷰|회고|생산성|동료|경험|일상|운영/i,
  Review:
    /리뷰|비교|회고|문제|장애|교훈|분석|고민|실패|개선|최적화|품질|보안|위험|이슈/i,
  Briefing:
    /출시|업데이트|공지|뉴스|발표|릴리즈|소식|행사|컨퍼런스|리전|지원|정책|가격|기능|서비스/i,
};

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function hasHangul(text) {
  return /[가-힣]/.test(text);
}

function decodeEntities(text) {
  return String(text ?? '')
    .replace(/<!\[CDATA\[([\s\S]*?)]]>/g, '$1')
    .replace(/&#(\d+);/g, (_, code) => String.fromCharCode(Number(code)))
    .replace(/&#x([0-9a-fA-F]+);/g, (_, code) => String.fromCharCode(parseInt(code, 16)))
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&apos;/g, "'");
}

function normalizeText(text) {
  return decodeEntities(text)
    .replace(/\r/g, '')
    .replace(/\t/g, ' ')
    .replace(/[ ]{2,}/g, ' ')
    .replace(/\n[ \t]+/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

function stripHtml(html) {
  return normalizeText(
    decodeEntities(html)
      .replace(/<script[\s\S]*?<\/script>/gi, ' ')
      .replace(/<style[\s\S]*?<\/style>/gi, ' ')
      .replace(/<noscript[\s\S]*?<\/noscript>/gi, ' ')
      .replace(/<svg[\s\S]*?<\/svg>/gi, ' ')
      .replace(/<nav[\s\S]*?<\/nav>/gi, ' ')
      .replace(/<header[\s\S]*?<\/header>/gi, ' ')
      .replace(/<footer[\s\S]*?<\/footer>/gi, ' ')
      .replace(/<aside[\s\S]*?<\/aside>/gi, ' ')
      .replace(/<form[\s\S]*?<\/form>/gi, ' ')
      .replace(/<(br|p|li|h[1-6]|div|section|article|tr|blockquote)\b[^>]*>/gi, '\n')
      .replace(/<[^>]+>/g, ' '),
  );
}

function shorten(text, maxLength) {
  const value = String(text ?? '').trim();
  if (value.length <= maxLength) {
    return value;
  }

  return `${value.slice(0, maxLength - 1).trim()}…`;
}

function cleanTag(tag) {
  const value = String(tag ?? '')
    .replace(/^#+/, '')
    .replace(/\s+/g, '-')
    .trim();

  if (!value) {
    return '';
  }

  return shorten(value, 30);
}

function normalizeUrl(url) {
  try {
    const parsed = new URL(decodeEntities(url).trim());
    parsed.hash = '';
    parsed.searchParams.sort();
    const value = parsed.toString();
    return value.endsWith('/') ? value.slice(0, -1) : value;
  } catch {
    return '';
  }
}

async function fetchText(url, accept = 'text/html,application/xhtml+xml,application/xml,text/xml,*/*') {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(url, {
      headers: {
        'User-Agent': 'ProjectAlphaSeedBot/1.0',
        Accept: accept,
      },
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(`${response.status} ${response.statusText}`);
    }

    return await response.text();
  } finally {
    clearTimeout(timeoutId);
  }
}

function extractLocs(xml) {
  return [...decodeEntities(xml).matchAll(/<loc>([\s\S]*?)<\/loc>/gi)]
    .map((match) => normalizeUrl(match[1]))
    .filter(Boolean);
}

async function collectSitemapCandidates(sourceConfig) {
  const queue = [sourceConfig.url];
  const seenSitemaps = new Set();
  const candidates = [];

  while (queue.length > 0 && seenSitemaps.size < 40) {
    const sitemapUrl = queue.shift();
    const normalizedSitemapUrl = normalizeUrl(sitemapUrl);
    if (!normalizedSitemapUrl || seenSitemaps.has(normalizedSitemapUrl)) {
      continue;
    }

    seenSitemaps.add(normalizedSitemapUrl);

    try {
      const xml = await fetchText(sitemapUrl, 'application/xml,text/xml,*/*');
      for (const loc of extractLocs(xml)) {
        if (/sitemap/i.test(loc)) {
          queue.push(loc);
          continue;
        }

        if (sourceConfig.include(loc)) {
          candidates.push({
            source: sourceConfig.source,
            url: loc,
          });
        }
      }
    } catch (error) {
      console.warn(`sitemap skipped: ${sourceConfig.source} ${sitemapUrl} ${error.message}`);
    }
  }

  return candidates;
}

function getTagValue(xml, tagName) {
  const match = xml.match(new RegExp(`<${tagName}(?:\\s[^>]*)?>([\\s\\S]*?)<\\/${tagName}>`, 'i'));
  return match ? stripHtml(match[1]) : '';
}

function getRawTagValue(xml, tagName) {
  const match = xml.match(new RegExp(`<${tagName}(?:\\s[^>]*)?>([\\s\\S]*?)<\\/${tagName}>`, 'i'));
  return match ? decodeEntities(match[1]).trim() : '';
}

function getRssLink(xml) {
  return getTagValue(xml, 'link');
}

function getAtomLink(xml) {
  const alternateLink = xml.match(/<link[^>]+rel=["']alternate["'][^>]+href=["']([^"']+)["'][^>]*>/i);
  if (alternateLink) {
    return normalizeUrl(alternateLink[1]);
  }

  const hrefLink = xml.match(/<link[^>]+href=["']([^"']+)["'][^>]*>/i);
  return hrefLink ? normalizeUrl(hrefLink[1]) : normalizeUrl(getTagValue(xml, 'link'));
}

function parseFeed(xml, source) {
  const items = [];
  const rssItems = [...xml.matchAll(/<item\b[\s\S]*?<\/item>/gi)].map((match) => match[0]);
  const atomEntries = [...xml.matchAll(/<entry\b[\s\S]*?<\/entry>/gi)].map((match) => match[0]);

  for (const itemXml of rssItems) {
    const url = normalizeUrl(getRssLink(itemXml));
    if (url) {
      items.push({
        source,
        url,
        fallbackTitle: getTagValue(itemXml, 'title'),
        fallbackText:
          getTagValue(itemXml, 'description') ||
          getTagValue(itemXml, 'content:encoded') ||
          getTagValue(itemXml, 'summary'),
      });
    }
  }

  for (const entryXml of atomEntries) {
    const url = getAtomLink(entryXml);
    if (url) {
      items.push({
        source,
        url,
        fallbackTitle: getTagValue(entryXml, 'title'),
        fallbackText:
          getTagValue(entryXml, 'summary') ||
          getTagValue(entryXml, 'content') ||
          stripHtml(getRawTagValue(entryXml, 'content')),
      });
    }
  }

  return items;
}

async function collectFeedCandidates(sourceConfig) {
  try {
    const xml = await fetchText(sourceConfig.url, 'application/rss+xml,application/atom+xml,application/xml,text/xml,*/*');
    return parseFeed(xml, sourceConfig.source);
  } catch (error) {
    console.warn(`feed skipped: ${sourceConfig.source} ${sourceConfig.url} ${error.message}`);
    return [];
  }
}

async function collectJsonCandidates(sourceConfig) {
  try {
    const text = await fetchText(sourceConfig.url, 'application/json,*/*');
    return sourceConfig.extract(JSON.parse(text));
  } catch (error) {
    console.warn(`json source skipped: ${sourceConfig.source} ${sourceConfig.url} ${error.message}`);
    return [];
  }
}

async function collectCandidates() {
  const collected = [];
  for (const sourceConfig of SITEMAP_SOURCES) {
    const candidates = await collectSitemapCandidates(sourceConfig);
    console.log(`collected sitemap candidates: ${sourceConfig.source} ${candidates.length}`);
    collected.push(...candidates);
  }

  for (const sourceConfig of JSON_SOURCES) {
    const candidates = await collectJsonCandidates(sourceConfig);
    console.log(`collected json candidates: ${sourceConfig.source} ${candidates.length}`);
    collected.push(...candidates);
  }

  for (const sourceConfig of FEED_SOURCES) {
    const candidates = await collectFeedCandidates(sourceConfig);
    console.log(`collected feed candidates: ${sourceConfig.source} ${candidates.length}`);
    collected.push(...candidates);
  }

  const deduped = new Map();
  for (const candidate of collected) {
    const url = normalizeUrl(candidate.url);
    if (url && !deduped.has(url)) {
      deduped.set(url, {
        ...candidate,
        url,
      });
    }
  }

  return roundRobinBySource([...deduped.values()]).slice(0, CANDIDATE_LIMIT);
}

function roundRobinBySource(candidates) {
  const groups = new Map();
  for (const candidate of candidates) {
    if (!groups.has(candidate.source)) {
      groups.set(candidate.source, []);
    }
    groups.get(candidate.source).push(candidate);
  }

  const ordered = [];
  while ([...groups.values()].some((items) => items.length > 0)) {
    for (const items of groups.values()) {
      const item = items.shift();
      if (item) {
        ordered.push(item);
      }
    }
  }

  return ordered;
}

function getMetaContent(html, key) {
  const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const patterns = [
    new RegExp(`<meta[^>]+property=["']${escapedKey}["'][^>]+content=["']([^"']+)["'][^>]*>`, 'i'),
    new RegExp(`<meta[^>]+name=["']${escapedKey}["'][^>]+content=["']([^"']+)["'][^>]*>`, 'i'),
    new RegExp(`<meta[^>]+content=["']([^"']+)["'][^>]+property=["']${escapedKey}["'][^>]*>`, 'i'),
    new RegExp(`<meta[^>]+content=["']([^"']+)["'][^>]+name=["']${escapedKey}["'][^>]*>`, 'i'),
  ];

  for (const pattern of patterns) {
    const match = html.match(pattern);
    if (match) {
      return stripHtml(match[1]);
    }
  }

  return '';
}

function getFirstTagText(html, tagName) {
  const match = html.match(new RegExp(`<${tagName}\\b[^>]*>([\\s\\S]*?)<\\/${tagName}>`, 'i'));
  return match ? stripHtml(match[1]) : '';
}

function getFirstContainerHtml(html) {
  const selectors = ['article', 'main'];
  for (const selector of selectors) {
    const match = html.match(new RegExp(`<${selector}\\b[^>]*>([\\s\\S]*?)<\\/${selector}>`, 'i'));
    if (match) {
      return match[1];
    }
  }

  const bodyMatch = html.match(/<body\b[^>]*>([\s\S]*?)<\/body>/i);
  return bodyMatch ? bodyMatch[1] : html;
}

function extractTitle(html) {
  return (
    getMetaContent(html, 'og:title') ||
    getMetaContent(html, 'twitter:title') ||
    getFirstTagText(html, 'h1') ||
    getFirstTagText(html, 'title')
  );
}

function extractKeywords(html) {
  const keywords = getMetaContent(html, 'keywords');
  if (!keywords) {
    return [];
  }

  return keywords
    .split(/[,|]/)
    .map(cleanTag)
    .filter(Boolean);
}

async function crawlDocument(candidate) {
  let html = '';
  try {
    html = await fetchText(candidate.url);
  } catch (error) {
    html = '';
  }

  const extractedTitle = html ? extractTitle(html).replace(/\s+\|\s+.*$/, '') : '';
  const title = shorten(extractedTitle || candidate.fallbackTitle || '', 120);
  const containerHtml = html ? getFirstContainerHtml(html) : '';
  let text = stripHtml(containerHtml);

  if (text.length < MIN_TEXT_LENGTH && candidate.fallbackText) {
    text = stripHtml(candidate.fallbackText);
  }

  if (!title || !text || text.length < MIN_TEXT_LENGTH || !hasHangul(`${title} ${text}`)) {
    return null;
  }

  return {
    ...candidate,
    title,
    text,
    keywords: extractKeywords(html),
  };
}

function scoreCategories(document) {
  const haystack = `${document.title}\n${document.text}\n${document.keywords.join(' ')}`;
  const scored = [];

  for (const category of CATEGORIES) {
    const matches = haystack.match(CATEGORY_KEYWORDS[category]);
    scored.push({
      category,
      score: matches ? matches.length + 1 : 0,
    });
  }

  scored.sort((left, right) => right.score - left.score);
  return scored.filter((item) => item.score > 0).map((item) => item.category);
}

function chooseCategory(document, categoryCounts) {
  const candidates = scoreCategories(document);
  for (const category of candidates) {
    if ((categoryCounts[category] ?? 0) < CATEGORY_QUOTAS[category]) {
      return category;
    }
  }

  return CATEGORIES
    .slice()
    .sort((left, right) => (categoryCounts[left] ?? 0) - (categoryCounts[right] ?? 0))[0];
}

function buildTags(document, category) {
  const tags = [];
  const tagKeys = new Set();
  const baseTags = [category, document.source, ...document.keywords];

  for (const [tag, pattern] of KEYWORD_TAGS) {
    if (pattern.test(`${document.title}\n${document.text}`)) {
      baseTags.push(tag);
    }
  }

  for (const rawTag of baseTags) {
    const tag = cleanTag(rawTag);
    const tagKey = tag.toLowerCase();
    if (tag && !tagKeys.has(tagKey)) {
      tags.push(tag);
      tagKeys.add(tagKey);
    }

    if (tags.length >= 8) {
      break;
    }
  }

  return tags;
}

function buildContent(document) {
  const bodyMaxLength = 5000 - 220 - document.url.length - document.source.length;
  return shorten(
    `출처: ${document.source}
원문 링크: ${document.url}

${shorten(document.text, Math.max(bodyMaxLength, 1000))}`,
    5000,
  );
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

async function getJson(path, token) {
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { headers });
  const responseText = await response.text();

  if (!response.ok) {
    throw new Error(`${path} failed: ${response.status} ${responseText}`);
  }

  return responseText ? JSON.parse(responseText) : null;
}

async function loginOrSignUp() {
  try {
    return await postJson('/api/auth/signup', {
      username: USERNAME,
      password: PASSWORD,
    });
  } catch {
    return await postJson('/api/auth/login', {
      username: USERNAME,
      password: PASSWORD,
    });
  }
}

function extractSourceUrl(content) {
  const match =
    String(content ?? '').match(/원문 링크:\s*(https?:\/\/\S+)/) ||
    String(content ?? '').match(/Source:\s*(https?:\/\/\S+)/);
  return match ? normalizeUrl(match[1]) : '';
}

async function loadExistingState() {
  const existingUrls = new Set();
  const existingTitles = new Set();
  const categoryCounts = Object.fromEntries(CATEGORIES.map((category) => [category, 0]));
  let page = 0;
  let sourceBackedPosts = 0;

  while (true) {
    const pageResult = await getJson(`/api/posts?page=${page}&size=100`);
    for (const post of pageResult.posts ?? []) {
      existingTitles.add(post.title);
      if (categoryCounts[post.category] !== undefined) {
        categoryCounts[post.category] += 1;
      }

      const sourceUrl = extractSourceUrl(post.content);
      if (sourceUrl) {
        existingUrls.add(sourceUrl);
        sourceBackedPosts += 1;
      }
    }

    if (page >= (pageResult.totalPages ?? 1) - 1) {
      break;
    }
    page += 1;
  }

  return {
    existingUrls,
    existingTitles,
    categoryCounts,
    sourceBackedPosts,
  };
}

async function mapWithConcurrency(items, concurrency, callback) {
  const results = [];
  let nextIndex = 0;

  async function worker() {
    while (nextIndex < items.length) {
      const currentIndex = nextIndex;
      nextIndex += 1;
      try {
        results[currentIndex] = await callback(items[currentIndex], currentIndex);
      } catch (error) {
        results[currentIndex] = {
          error,
          candidate: items[currentIndex],
        };
      }
    }
  }

  await Promise.all(Array.from({ length: concurrency }, worker));
  return results;
}

async function main() {
  const auth = await loginOrSignUp();
  const existingState = await loadExistingState();
  const desiredNewPosts = Math.max(TARGET_TOTAL - existingState.sourceBackedPosts, 0);

  console.log(
    JSON.stringify({
      mode: DRY_RUN ? 'dry-run' : 'import',
      targetTotal: TARGET_TOTAL,
      existingSourceBackedPosts: existingState.sourceBackedPosts,
      desiredNewPosts,
      categoryCounts: existingState.categoryCounts,
    }),
  );

  if (desiredNewPosts === 0) {
    return;
  }

  const candidates = (await collectCandidates()).filter(
    (candidate) => !existingState.existingUrls.has(normalizeUrl(candidate.url)),
  );
  console.log(`candidate urls after existing-url skip: ${candidates.length}`);

  let createdPosts = 0;
  let crawledDocuments = 0;
  let skippedDocuments = 0;
  const createdCategoryCounts = Object.fromEntries(CATEGORIES.map((category) => [category, 0]));

  for (let start = 0; start < candidates.length && createdPosts < desiredNewPosts; start += CRAWL_CONCURRENCY) {
    const batch = candidates.slice(start, start + CRAWL_CONCURRENCY);
    const documents = await mapWithConcurrency(batch, CRAWL_CONCURRENCY, crawlDocument);

    for (const result of documents) {
      if (createdPosts >= desiredNewPosts) {
        break;
      }

      if (!result || result.error) {
        skippedDocuments += 1;
        continue;
      }

      crawledDocuments += 1;
      const title = shorten(`[${result.source}] ${result.title}`, 120);
      if (existingState.existingTitles.has(title)) {
        skippedDocuments += 1;
        continue;
      }

      const category = chooseCategory(result, existingState.categoryCounts);
      const payload = {
        category,
        title,
        content: buildContent(result),
        tags: buildTags(result, category),
      };

      if (!DRY_RUN) {
        await postJson('/api/posts', payload, auth.token);
        await sleep(50);
      }

      existingState.existingUrls.add(normalizeUrl(result.url));
      existingState.existingTitles.add(title);
      existingState.categoryCounts[category] = (existingState.categoryCounts[category] ?? 0) + 1;
      createdCategoryCounts[category] += 1;
      createdPosts += 1;

      if (createdPosts % 25 === 0) {
        console.log(
          JSON.stringify({
            createdPosts,
            crawledDocuments,
            skippedDocuments,
            latestTitle: title,
            categoryCounts: existingState.categoryCounts,
          }),
        );
      }
    }
  }

  console.log(
    JSON.stringify(
      {
        createdPosts,
        crawledDocuments,
        skippedDocuments,
        createdCategoryCounts,
        finalCategoryCounts: existingState.categoryCounts,
      },
      null,
      2,
    ),
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
