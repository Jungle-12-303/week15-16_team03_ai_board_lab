const API_BASE_URL = process.env.PROJECT_ALPHA_API_BASE_URL ?? 'http://localhost:8080';
const USERNAME = process.env.PROJECT_ALPHA_SEED_USERNAME ?? 'korean-seed';
const PASSWORD = process.env.PROJECT_ALPHA_SEED_PASSWORD ?? 'korean-seed-password';

const CATEGORY_QUOTAS = {
  Development: 21,
  Learning: 21,
  Project: 21,
  Daily: 21,
  Review: 21,
  Briefing: 20,
};

const FEEDS = [
  {
    source: '우아한형제들 기술블로그',
    url: 'https://techblog.woowahan.com/feed/',
  },
  {
    source: 'NAVER D2',
    url: 'https://d2.naver.com/d2.atom',
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
    source: 'Dev Sisters Tech',
    url: 'https://tech.devsisters.com/rss.xml',
  },
  {
    source: 'Toss Tech',
    url: 'https://toss.tech/rss.xml',
  },
];

const KEYWORDS = {
  Development:
    /개발|프론트|백엔드|서버|클라이언트|API|React|Spring|Java|Node|TypeScript|JavaScript|Kotlin|Python|MySQL|DB|데이터베이스|Docker|Kubernetes|인프라|아키텍처|테스트|성능|배포/i,
  Learning:
    /학습|이해|가이드|튜토리얼|시작|소개|알아보기|사용법|정리|기초|입문|원리|개념|교육|세미나|캠프|스터디/i,
  Project:
    /개발기|구축|적용|도입|전환|마이그레이션|자동화|프로젝트|사례|서비스|플랫폼|시스템|개선|실험|만들/i,
  Daily:
    /문화|커리어|일하는|조직|팀|협업|채용|성장|인터뷰|회고|생산성|동료|경험|일상|운영/i,
  Review:
    /리뷰|비교|회고|문제|장애|교훈|분석|고민|실패|개선|최적화|품질|보안|위험|이슈/i,
  Briefing:
    /출시|업데이트|공지|뉴스|발표|릴리즈|소식|행사|컨퍼런스|리전|지원|정책|가격|기능|서비스/i,
};

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

function stripHtml(text) {
  return decodeEntities(text)
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
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
    return decodeEntities(alternateLink[1]).trim();
  }

  const hrefLink = xml.match(/<link[^>]+href=["']([^"']+)["'][^>]*>/i);
  return hrefLink ? decodeEntities(hrefLink[1]).trim() : getTagValue(xml, 'link');
}

function getCategories(xml) {
  const categories = [];
  for (const match of xml.matchAll(/<category(?:\s[^>]*)?>([\s\S]*?)<\/category>/gi)) {
    const value = stripHtml(match[1]);
    if (value) {
      categories.push(value);
    }
  }

  for (const match of xml.matchAll(/<category[^>]+term=["']([^"']+)["'][^>]*>/gi)) {
    const value = stripHtml(match[1]);
    if (value) {
      categories.push(value);
    }
  }

  return categories;
}

function parseFeed(xml, source) {
  const items = [];
  const rssItems = [...xml.matchAll(/<item\b[\s\S]*?<\/item>/gi)].map((match) => match[0]);
  const atomEntries = [...xml.matchAll(/<entry\b[\s\S]*?<\/entry>/gi)].map((match) => match[0]);

  for (const itemXml of rssItems) {
    items.push({
      source,
      title: getTagValue(itemXml, 'title'),
      summary:
        getTagValue(itemXml, 'description') ||
        getTagValue(itemXml, 'content:encoded') ||
        getTagValue(itemXml, 'summary'),
      url: getRssLink(itemXml),
      author: getTagValue(itemXml, 'dc:creator') || getTagValue(itemXml, 'author'),
      publishedAt: getTagValue(itemXml, 'pubDate') || getTagValue(itemXml, 'published'),
      tags: getCategories(itemXml),
    });
  }

  for (const entryXml of atomEntries) {
    items.push({
      source,
      title: getTagValue(entryXml, 'title'),
      summary:
        getTagValue(entryXml, 'summary') ||
        getTagValue(entryXml, 'content') ||
        stripHtml(getRawTagValue(entryXml, 'content')),
      url: getAtomLink(entryXml),
      author: getTagValue(entryXml, 'name') || getTagValue(entryXml, 'author'),
      publishedAt: getTagValue(entryXml, 'published') || getTagValue(entryXml, 'updated'),
      tags: getCategories(entryXml),
    });
  }

  return items.filter((item) => item.title && item.url && hasHangul(`${item.title} ${item.summary}`));
}

async function fetchFeed(feed) {
  const response = await fetch(feed.url, {
    headers: {
      'User-Agent': 'ProjectAlphaSeedBot/1.0',
      Accept: 'application/rss+xml, application/atom+xml, application/xml, text/xml, */*',
    },
  });

  if (!response.ok) {
    throw new Error(`${feed.source} feed failed: ${response.status}`);
  }

  return parseFeed(await response.text(), feed.source);
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

function candidateCategories(item) {
  const haystack = `${item.title} ${item.summary} ${(item.tags ?? []).join(' ')}`;
  const categories = [];

  for (const [category, keyword] of Object.entries(KEYWORDS)) {
    if (keyword.test(haystack)) {
      categories.push(category);
    }
  }

  if (categories.length === 0) {
    categories.push('Review');
  }

  return categories;
}

function buildContent(item, category) {
  const summary =
    shorten(item.summary, 900) ||
    'RSS 요약문이 비어 있어 제목, 태그, 출처 정보를 기준으로 수집한 참고 게시글입니다.';
  const tagHint = item.tags?.slice(0, 8).join(', ') || '없음';

  return shorten(
    `출처: ${item.source}
원문 링크: ${item.url}
작성자/게시자: ${item.author || '알 수 없음'}
게시/수집 시각: ${item.publishedAt || '알 수 없음'}

요약:
${summary}

Project Alpha 분류:
이 글은 ${category} 카테고리의 한국어 참고 게시글로 수집했다. 제목, RSS 요약문, 태그, 출처 메타데이터를 기준으로 분류했고, RAG 검색 테스트에서 실제 한국어 글의 주제 다양성을 확보하기 위한 데이터로 사용한다.

태그 힌트:
${tagHint}`,
    5000,
  );
}

function buildTags(item, category) {
  const tags = [];
  const tagKeys = new Set();

  for (const rawTag of [category, item.source, ...(item.tags ?? [])]) {
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

function pickBalancedItems(items) {
  const picked = [];
  const usedUrls = new Set();

  for (const [category, quota] of Object.entries(CATEGORY_QUOTAS)) {
    let count = 0;
    for (const item of items) {
      if (count >= quota) {
        break;
      }
      if (usedUrls.has(item.url)) {
        continue;
      }
      if (!candidateCategories(item).includes(category)) {
        continue;
      }

      picked.push({ item, category });
      usedUrls.add(item.url);
      count++;
    }
  }

  for (const [category, quota] of Object.entries(CATEGORY_QUOTAS)) {
    let count = picked.filter((candidate) => candidate.category === category).length;
    for (const item of items) {
      if (count >= quota) {
        break;
      }
      if (usedUrls.has(item.url)) {
        continue;
      }

      picked.push({ item, category });
      usedUrls.add(item.url);
      count++;
    }
  }

  return picked;
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
    body: JSON.stringify(body),
  });
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

async function main() {
  const itemsByUrl = new Map();

  for (const feed of FEEDS) {
    const items = await fetchFeed(feed);
    for (const item of items) {
      if (!itemsByUrl.has(item.url)) {
        itemsByUrl.set(item.url, item);
      }
    }
  }

  const items = [...itemsByUrl.values()];
  const pickedItems = pickBalancedItems(items);

  if (pickedItems.length !== 125) {
    throw new Error(`Expected 125 Korean seed posts, but picked ${pickedItems.length}.`);
  }

  const auth = await loginOrSignUp();
  let createdPosts = 0;
  const categoryCounts = {};

  for (const { item, category } of pickedItems) {
    await postJson(
      '/api/posts',
      {
        category,
        title: shorten(`[${item.source}] ${item.title}`, 120),
        content: buildContent(item, category),
        tags: buildTags(item, category),
      },
      auth.token,
    );
    createdPosts++;
    categoryCounts[category] = (categoryCounts[category] ?? 0) + 1;
  }

  console.log(
    JSON.stringify(
      {
        collectedSources: items.length,
        createdPosts,
        categoryCounts,
        author: USERNAME,
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
