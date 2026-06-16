import { spawn } from 'node:child_process';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const BACKEND_DIR = resolve('backend');
const OUTPUT_JSON_PATH =
  process.env.PROJECT_ALPHA_RAG_SCENARIO_JSON ??
  'backend/build/rag-scenario-evaluation.json';
const OUTPUT_MARKDOWN_PATH =
  process.env.PROJECT_ALPHA_RAG_SCENARIO_MARKDOWN ??
  'docs/rag-scenario-evaluation.md';
const API_BASE_URL =
  process.env.PROJECT_ALPHA_RAG_SCENARIO_API_BASE_URL ?? 'http://localhost:18081';
const SERVER_PORT = Number(new URL(API_BASE_URL).port || 80);
const START_SERVER = process.env.PROJECT_ALPHA_RAG_SCENARIO_START_SERVER !== 'false';
const DRAFT_IDS = new Set(
  (process.env.PROJECT_ALPHA_RAG_SCENARIO_DRAFT_IDS ?? '')
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean),
);
const WITH_DRAFT =
  process.env.PROJECT_ALPHA_RAG_SCENARIO_WITH_DRAFT === 'true' || DRAFT_IDS.size > 0;
const USERNAME = process.env.PROJECT_ALPHA_SEED_USERNAME ?? 'korean-seed';
const PASSWORD = process.env.PROJECT_ALPHA_SEED_PASSWORD ?? 'korean-seed-password';
const K = Number(process.env.PROJECT_ALPHA_RAG_SCENARIO_K ?? 5);

const SCENARIOS = [
  {
    id: 'github-actions-secrets',
    name: 'GitHub Actions secrets 배포 실패',
    intent: 'GitHub Actions, workflow, secrets, deploy 관련 글이 상위에 와야 한다.',
    expected: 'GitHub Actions/Secrets/Workflow/Deploy',
    query: {
      category: 'Development',
      title: 'GitHub Actions 배포가 secrets 때문에 실패함',
      content:
        'PR 머지 후 deploy workflow가 실행되지만 AWS_ACCESS_KEY_ID를 읽지 못해서 실패합니다. GitHub Actions secrets 설정과 배포 파이프라인을 정리하고 싶습니다.',
      tags: ['GitHub Actions', 'CI/CD', 'Deploy'],
    },
    expectedGroups: [
      ['github', '깃허브'],
      ['actions', 'action'],
      ['workflow', '워크플로우', '워크플로'],
      ['secrets', 'secret', '시크릿'],
      ['deploy', 'deployment', '배포', 'ci'],
    ],
    minimumMatchedGroups: 2,
    rejectTerms: ['날씨', 'cpu', 'gpu', '쿠키런', '배달'],
  },
  {
    id: 'cpu-upgrade',
    name: '7800X3D에서 9800X3D 업그레이드 고민',
    intent: 'CPU 업그레이드, 온도, 부스트 클럭, 하드웨어 체감 글이 상위에 와야 한다.',
    expected: 'CPU/7800X3D/9800X3D/업그레이드',
    query: {
      category: 'Daily',
      title: '7800X3D에서 9800X3D로 갈지 고민',
      content:
        '현재 7800X3D를 사용 중인데 9800X3D로 업그레이드할지, 아니면 다음 세대를 기다릴지 고민하고 있습니다. 온도와 부스트 클럭, 실제 체감 차이가 궁금합니다.',
      tags: ['CPU', 'Hardware', 'Upgrade'],
    },
    expectedGroups: [
      ['cpu', '시피유', '씨피유'],
      ['7800x3d', '9800x3d', 'x3d'],
      ['upgrade', '업그레이드'],
      ['온도', '부스트', '클럭', '체감'],
    ],
    minimumMatchedGroups: 2,
    rejectTerms: ['날씨', 'github', 'actions', 'react', 'airflow'],
  },
  {
    id: 'react-usestate-form',
    name: 'React useState 폼 상태 관리',
    intent: 'React, hook, useState, 프론트엔드 상태 관리 글이 상위에 와야 한다.',
    expected: 'React/useState/hook/state',
    query: {
      category: 'Learning',
      title: 'React에서 useState로 글쓰기 폼 상태 관리하기',
      content:
        '제목, 내용, 태그 input을 useState로 관리하고 submit 시 API 요청을 보내는 흐름을 공부하고 있습니다. 컴포넌트 분리와 custom hook도 같이 보고 싶습니다.',
      tags: ['React', 'useState', 'Form'],
    },
    expectedGroups: [
      ['react', '리액트'],
      ['usestate', 'state', '상태'],
      ['hook', 'hooks', '훅'],
      ['form', '폼', 'input', 'component', '컴포넌트'],
    ],
    minimumMatchedGroups: 2,
    rejectTerms: ['날씨', 'cpu', 'redis', 'airflow'],
  },
  {
    id: 'redis-session-cache',
    name: 'Spring Redis 세션과 캐시',
    intent: 'Redis, Spring Session, cache, 분산 락 관련 글이 상위에 와야 한다.',
    expected: 'Redis/Spring Session/Cache',
    query: {
      category: 'Development',
      title: 'Spring Boot에서 Redis 세션과 캐시를 안정적으로 쓰기',
      content:
        'Spring Session을 Redis에 저장하고 API 캐시도 Redis로 관리하려고 합니다. connection 증가, TTL, 캐시 삭제, 분산 락 이슈를 정리하고 싶습니다.',
      tags: ['Spring Boot', 'Redis', 'Cache'],
    },
    expectedGroups: [
      ['redis'],
      ['session', '세션'],
      ['cache', '캐시'],
      ['spring', 'boot', '스프링'],
      ['ttl', 'lock', '락', 'connection'],
    ],
    minimumMatchedGroups: 2,
    rejectTerms: ['날씨', 'cpu', 'react', 'github actions'],
  },
  {
    id: 'weather-yongin',
    name: '용인 오늘 날씨 브리핑',
    intent: '날씨라는 대주제는 맞아야 하고, 지역까지 맞으면 더 좋다.',
    expected: '날씨 + 용인',
    query: {
      category: 'Briefing',
      title: '용인 오늘 날씨 브리핑',
      content:
        '오늘 용인 날씨를 바탕으로 외출 전 참고할 짧은 게시글을 쓰고 싶습니다. 비가 오는지, 체감온도와 옷차림은 어떤지 정리하고 싶습니다.',
      tags: ['Weather', 'Briefing', '용인'],
    },
    expectedGroups: [
      ['weather', '날씨', 'forecast', '비', '체감온도'],
      ['용인'],
    ],
    minimumMatchedGroups: 1,
    idealMatchedGroups: 2,
    rejectTerms: ['github', 'actions', 'react', 'cpu', 'airflow'],
  },
  {
    id: 'airflow-pipeline',
    name: 'Airflow 데이터 파이프라인 운영',
    intent: 'Airflow, ELT, 로그 파이프라인, 데이터 플랫폼 관련 글이 상위에 와야 한다.',
    expected: 'Airflow/ELT/Data pipeline',
    query: {
      category: 'Project',
      title: 'Airflow로 로그 데이터 파이프라인 운영하기',
      content:
        'Airflow DAG로 로그 수집과 ELT 파이프라인을 운영하고 있습니다. 실패 재시도, 스케줄링, 데이터 품질 체크, 분석 플랫폼 연동 사례를 찾고 싶습니다.',
      tags: ['Airflow', 'Data Pipeline', 'ELT'],
    },
    expectedGroups: [
      ['airflow'],
      ['pipeline', '파이프라인'],
      ['data', '데이터'],
      ['elt', 'etl', 'dag', '스케줄링', '로그'],
    ],
    minimumMatchedGroups: 2,
    rejectTerms: ['날씨', 'cpu', 'react hook', 'github actions'],
  },
  {
    id: 'weak-source-diary',
    name: '자료가 거의 없는 일상 회고',
    intent: '회고/일상뿐 아니라 운동/수면/컨디션까지 직접 맞는 근거가 있는지 확인한다.',
    expected: '회고/일상/운동/컨디션',
    query: {
      category: 'Daily',
      title: '퇴근 후 운동 루틴을 다시 잡아보려는 회고',
      content:
        '요즘 밤에 늦게 자고 운동을 빼먹어서 컨디션이 무너졌습니다. 다음 주에는 퇴근 후 30분만이라도 가볍게 운동하고 수면 시간을 고정해보려고 합니다.',
      tags: ['Daily', 'Review', 'Health'],
    },
    expectedGroups: [
      ['운동', 'health', '컨디션', '수면'],
      ['회고', 'daily', '일상'],
    ],
    minimumMatchedGroups: 2,
    rejectTerms: ['github', 'actions', 'redis', 'airflow', 'cpu', 'gpu'],
  },
];

async function main() {
  if (process.env.PROJECT_ALPHA_RAG_SCENARIO_RENDER_ONLY === 'true') {
    const report = JSON.parse(readFileSync(resolve(OUTPUT_JSON_PATH), 'utf8'));
    writeMarkdown(OUTPUT_MARKDOWN_PATH, report);
    console.log(`Rendered ${resolve(OUTPUT_MARKDOWN_PATH)} from ${resolve(OUTPUT_JSON_PATH)}`);
    return;
  }

  const server = START_SERVER ? await startServer() : null;

  try {
    const auth = await login();
    const scenarioResults = [];

    for (const scenario of SCENARIOS) {
      console.log(`\n=== ${scenario.name} ===`);
      const similarResponse = await postJson('/api/ai/similar-posts', {
        ...scenario.query,
        limit: K,
      }, auth.token);
      const posts = Array.isArray(similarResponse.posts) ? similarResponse.posts : [];
      const shouldCreateDraft =
        WITH_DRAFT && (DRAFT_IDS.size === 0 || DRAFT_IDS.has(scenario.id));
      const draftResponse = shouldCreateDraft
        ? await postJson('/api/ai/draft', {
            ...scenario.query,
            limit: K,
          }, auth.token)
        : null;
      const assessedPosts = posts.slice(0, K).map((post, index) =>
        assessPost(scenario, post, index + 1));
      const judgement = judgeScenario(scenario, assessedPosts, draftResponse);

      console.table(assessedPosts.map((post) => ({
        rank: post.rank,
        postId: post.postId,
        title: post.title,
        score: post.score,
        groups: post.matchedGroupCount,
        relevant: post.relevant,
      })));
      console.log(`Verdict: ${judgement.verdict} - ${judgement.reason}`);

      scenarioResults.push({
        id: scenario.id,
        name: scenario.name,
        intent: scenario.intent,
        expected: scenario.expected,
        query: scenario.query,
        judgement,
        retrievedPosts: assessedPosts,
        draft: draftResponse
          ? {
              message: draftResponse.message ?? '',
              sourceTitles: (draftResponse.sources ?? []).map((source) => source.title),
              text: draftResponse.draft ?? '',
              excerpt: excerpt(draftResponse.draft ?? '', 600),
            }
          : null,
      });
    }

    const report = {
      generatedAt: new Date().toISOString(),
      apiBaseUrl: API_BASE_URL,
      k: K,
      withDraft: WITH_DRAFT,
      draftIds: Array.from(DRAFT_IDS),
      note: 'Scenario evaluation for human-readable RAG input/output review. Judgement is heuristic; top result titles and draft excerpts should be read directly.',
      scenarios: scenarioResults,
    };

    writeJson(OUTPUT_JSON_PATH, report);
    writeMarkdown(OUTPUT_MARKDOWN_PATH, report);

    console.log(`\nWrote ${resolve(OUTPUT_JSON_PATH)}`);
    console.log(`Wrote ${resolve(OUTPUT_MARKDOWN_PATH)}`);
  } finally {
    if (server) {
      await stopServer(server);
    }
  }
}

async function startServer() {
  const args = [
    `--server.port=${SERVER_PORT}`,
    '--spring.output.ansi.enabled=never',
    '--app.embedding-worker.enabled=false',
  ];
  const child = spawn(
    'cmd.exe',
    ['/c', '.\\gradlew.bat', 'bootRun', `--args=${args.join(' ')}`],
    {
      cwd: BACKEND_DIR,
      stdio: ['ignore', 'pipe', 'pipe'],
      windowsHide: true,
    },
  );
  let logs = '';

  child.stdout.on('data', (data) => {
    logs = (logs + data.toString()).slice(-30000);
  });
  child.stderr.on('data', (data) => {
    logs = (logs + data.toString()).slice(-30000);
  });

  await waitForServer(child, () => logs);
  return child;
}

async function waitForServer(child, getLogs) {
  const startedAt = Date.now();

  while (Date.now() - startedAt < 180000) {
    if (child.exitCode != null) {
      throw new Error(`bootRun exited early with code ${child.exitCode}\n${getLogs()}`);
    }

    try {
      const response = await fetch(`${API_BASE_URL}/actuator/health`);
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

async function stopServer(child) {
  if (!child.pid || child.exitCode != null) {
    return;
  }

  await new Promise((resolve) => {
    const killer = spawn('cmd.exe', ['/c', 'taskkill', '/PID', String(child.pid), '/T', '/F'], {
      stdio: 'ignore',
      windowsHide: true,
    });
    killer.on('exit', resolve);
    killer.on('error', resolve);
  });
}

async function login() {
  return postJson('/api/auth/login', {
    username: USERNAME,
    password: PASSWORD,
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
    body: JSON.stringify(body),
  });
  const responseText = await response.text();

  if (!response.ok) {
    throw new Error(`${path} failed: ${response.status} ${responseText}`);
  }

  return responseText ? JSON.parse(responseText) : null;
}

function assessPost(scenario, post, rank) {
  const text = normalize([
    post.title,
    post.category,
    post.content,
    post.scoreBreakdown?.matchedTerms?.join(' '),
  ].join(' '));
  const matchedGroups = scenario.expectedGroups
    .map((group, index) => ({
      index,
      terms: group,
      matched: group.some((term) => text.includes(normalize(term))),
    }))
    .filter((group) => group.matched);
  const rejectMatches = (scenario.rejectTerms ?? [])
    .filter((term) => text.includes(normalize(term)));

  return {
    rank,
    postId: post.postId,
    title: post.title,
    category: post.category,
    score: Number(post.score?.toFixed?.(3) ?? post.score),
    matchedGroupCount: matchedGroups.length,
    matchedGroups: matchedGroups.map((group) => group.terms.join('/')),
    rejectMatches,
    relevant: matchedGroups.length >= scenario.minimumMatchedGroups,
    contentExcerpt: excerpt(post.content, 240),
    matchedTerms: post.scoreBreakdown?.matchedTerms ?? [],
  };
}

function judgeScenario(scenario, posts, draftResponse) {
  const top1 = posts[0];
  const relevantCount = posts.filter((post) => post.relevant).length;
  const idealMatchedGroups = scenario.idealMatchedGroups ?? scenario.minimumMatchedGroups;
  const top1Strong = top1 && top1.matchedGroupCount >= idealMatchedGroups;
  const top1Partial = top1 && top1.matchedGroupCount >= scenario.minimumMatchedGroups;
  const draftText = normalize(draftResponse?.draft ?? '');
  const draftRejectTerms = (scenario.rejectTerms ?? [])
    .filter((term) => draftText.includes(normalize(term)));

  if (top1Strong && relevantCount >= 3 && draftRejectTerms.length === 0) {
    return {
      verdict: 'pass',
      reason: `Top1이 기대 주제와 강하게 맞고 top${K} 중 관련 후보가 ${relevantCount}개다.`,
      relevantCount,
      draftRejectTerms,
    };
  }

  if (top1Partial || relevantCount > 0) {
    const risk = draftRejectTerms.length > 0
      ? ` 초안에 경계 단어(${draftRejectTerms.join(', ')})가 섞였다.`
      : '';
    return {
      verdict: 'partial',
      reason: `일부 결과는 맞지만 top${K} 전체 안정성은 부족하다.${risk}`,
      relevantCount,
      draftRejectTerms,
    };
  }

  return {
    verdict: 'fail',
    reason: `Top${K}에서 기대 주제를 충분히 만족하는 결과를 찾지 못했다.`,
    relevantCount,
    draftRejectTerms,
  };
}

function writeJson(path, report) {
  const outputPath = resolve(path);
  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8');
}

function writeMarkdown(path, report) {
  const lines = [
    '# RAG 시나리오 입출력 평가',
    '',
    '이 문서는 정량 지표와 별개로 실제 사용자가 넣을 법한 입력에서 어떤 게시글이 추천되고, 초안 생성이 어떤 메시지를 내는지 확인하기 위한 기록이다.',
    '',
    `- 생성 시각: ${report.generatedAt}`,
    `- API: \`${report.apiBaseUrl}\``,
    `- Top K: ${report.k}`,
    `- 초안 생성 포함: ${report.withDraft ? '예' : '아니오'}`,
    `- 초안 생성 대상: ${report.draftIds.length > 0 ? report.draftIds.join(', ') : report.withDraft ? '전체' : '없음'}`,
    '',
    '판정은 보조 기준이다. 실제 판단에서는 각 시나리오의 입력, 추천 제목, 초안 excerpt를 직접 읽는다.',
    '',
    '## 요약',
    '',
    '| Scenario | Verdict | Relevant in Top5 | Top1 | Reason |',
    '|---|---|---:|---|---|',
    ...report.scenarios.map((scenario) => markdownTableRow([
      scenario.name,
      scenario.judgement.verdict,
      scenario.judgement.relevantCount,
      scenario.retrievedPosts[0]?.title ?? '',
      scenario.judgement.reason,
    ])),
    '',
  ];

  for (const scenario of report.scenarios) {
    lines.push(
      `## ${scenario.name}`,
      '',
      `- 기대 의도: ${scenario.intent}`,
      `- 기대 주제: ${scenario.expected}`,
      `- 판정: **${scenario.judgement.verdict}** - ${scenario.judgement.reason}`,
      '',
      '### 입력',
      '',
      `- Category: \`${scenario.query.category}\``,
      `- Title: ${scenario.query.title}`,
      `- Tags: ${scenario.query.tags.join(', ') || 'None'}`,
      '',
      '```text',
      scenario.query.content,
      '```',
      '',
      '### 유사 게시글 출력',
      '',
      '| Rank | Post ID | Score | Category | Matched Groups | Title |',
      '|---:|---:|---:|---|---:|---|',
      ...scenario.retrievedPosts.map((post) => markdownTableRow([
        post.rank,
        post.postId,
        post.score,
        post.category,
        post.matchedGroupCount,
        post.title,
      ])),
      '',
    );

    if (scenario.draft) {
      lines.push(
        '### 초안 생성 출력',
        '',
        `- Message: ${scenario.draft.message || 'None'}`,
        `- Sources: ${scenario.draft.sourceTitles.length > 0
          ? scenario.draft.sourceTitles.map((title) => `\`${title}\``).join(', ')
          : 'None'}`,
        '',
        '```text',
        scenario.draft.excerpt || '(초안 없음)',
        '```',
        '',
      );
    }
  }

  const outputPath = resolve(path);
  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, `${lines.join('\n')}\n`, 'utf8');
}

function markdownTableRow(cells) {
  return `| ${cells.map(escapeTable).join(' | ')} |`;
}

function escapeTable(value) {
  return String(value ?? '').replaceAll('|', '\\|').replace(/\s+/g, ' ').trim();
}

function excerpt(text, maxLength) {
  const normalizedText = String(text ?? '').replace(/\s+/g, ' ').trim();
  if (normalizedText.length <= maxLength) {
    return normalizedText;
  }

  return `${normalizedText.slice(0, maxLength)}...`;
}

function normalize(value) {
  return String(value ?? '').toLowerCase();
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
