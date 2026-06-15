-- Development seed data for posts, tags, and post_tags.
-- Run manually against a local PostgreSQL database when you need sample board data.

WITH seed_user AS (
    INSERT INTO users (email, password_hash, nickname, created_at)
    VALUES (
        'seed@example.com',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiKwmqKHuA3YYLHvC7P7S6u1gqC3EuO',
        'seed-user',
        now()
    )
    ON CONFLICT (email) DO UPDATE
        SET nickname = EXCLUDED.nickname
    RETURNING id
),
seed_tags AS (
    INSERT INTO tags (name)
    VALUES
        ('spring'),
        ('react'),
        ('rag'),
        ('pgvector'),
        ('openai'),
        ('mcp'),
        ('agent'),
        ('jwt'),
        ('docker'),
        ('stock'),
        ('database'),
        ('frontend')
    ON CONFLICT (name) DO NOTHING
    RETURNING id, name
),
all_tags AS (
    SELECT id, name FROM seed_tags
    UNION
    SELECT id, name
    FROM tags
    WHERE name IN (
        'spring',
        'react',
        'rag',
        'pgvector',
        'openai',
        'mcp',
        'agent',
        'jwt',
        'docker',
        'stock',
        'database',
        'frontend'
    )
),
seed_post_data AS (
    SELECT *
    FROM (
        VALUES
            (
                'Spring Security JWT 로그인 흐름 정리',
                '로그인은 사용자가 이메일과 비밀번호를 보내면 서버가 사용자 정보를 확인하고 JWT를 발급하는 순서로 처리됩니다. 이후 프론트엔드는 JWT를 localStorage에 저장하고 API 요청마다 Authorization 헤더에 Bearer 토큰을 붙입니다. 백엔드는 JWT 필터에서 토큰을 검증하고 SecurityContext에 인증 정보를 저장합니다.'
            ),
            (
                'Spring AI RAG 게시판 Q&A 봇 구현 메모',
                '게시판 Q&A 봇은 사용자의 질문을 임베딩으로 바꾼 뒤 pgvector의 vector_store 테이블에서 관련 게시글 chunk를 검색합니다. 검색된 Document의 metadata에서 postId, title, content를 꺼내 출처로 보여주고, LLM에는 검색 결과만 근거로 답변하도록 프롬프트를 구성합니다.'
            ),
            (
                'pgvector와 PostgreSQL vector_store 역할',
                'pgvector는 PostgreSQL에서 벡터 타입과 유사도 검색을 사용할 수 있게 해주는 확장입니다. Spring AI의 VectorStore는 추상화 인터페이스이고, PgVectorStore 구현체가 실제 PostgreSQL vector_store 테이블에 임베딩과 metadata를 저장합니다.'
            ),
            (
                'OpenAI 임베딩 API가 필요한 이유',
                'RAG에서 게시글과 질문을 비교하려면 둘 다 숫자 벡터로 변환해야 합니다. OpenAI embedding model은 텍스트의 의미를 벡터로 바꿔주며, 이 벡터를 기준으로 비슷한 게시글을 찾습니다. Chat model은 검색된 문서를 바탕으로 최종 답변을 생성합니다.'
            ),
            (
                'MCP GitHub 조회 기능 설계',
                'MCP 기능은 JSON-RPC 스타일 요청을 받아 외부 시스템을 호출하는 구조입니다. 예를 들어 github.getUser 요청이 들어오면 서버가 GitHub API를 호출하고 사용자 프로필 정보를 응답합니다. API Key나 토큰은 application properties에 직접 쓰지 않고 환경 변수로 관리해야 합니다.'
            ),
            (
                'AI Agent 작성 보조 기능 흐름',
                'Agent 기능은 사용자의 글 초안을 보고 필요한 도구를 선택하는 추론 루프를 가집니다. 현재 구조에서는 태그 추천, 유사 게시글 검색, MCP 외부 API 호출 같은 도구를 선택하고 실행한 뒤 최종 작성 보조 메시지를 만듭니다. 무한 루프를 막기 위해 최대 반복 횟수를 둡니다.'
            ),
            (
                'React 게시글 목록과 상세 화면 구성',
                '프론트엔드는 게시글 목록, 상세, 작성, 수정 모드를 하나의 PostListPage에서 관리합니다. 목록에서는 검색어와 태그로 게시글을 조회하고, 상세 화면에서는 댓글과 AI 참고 결과를 보여줍니다. 챗봇은 대화 저장 없이 React state로만 메시지를 관리합니다.'
            ),
            (
                'Docker Compose로 로컬 개발 환경 실행하기',
                'Docker Compose를 사용하면 PostgreSQL, pgvector, Spring Boot, React 개발 서버를 함께 실행할 수 있습니다. 데이터베이스 URL, 사용자명, 비밀번호, OpenAI API Key는 환경 변수로 주입하는 것이 안전합니다.'
            ),
            (
                '주식 투자 게시글',
                '삼성전자, 나스닥, 금리, 주가 전망에 대해 이야기하는 게시글입니다. 금리가 높아지면 성장주 밸류에이션에 부담이 생길 수 있고, 반도체 업황과 환율도 삼성전자 주가를 볼 때 함께 확인해야 합니다.'
            ),
            (
                '게시글 수정 시 RAG 인덱스도 갱신해야 하는 이유',
                '게시글 본문이 수정되면 기존 vector_store chunk는 예전 내용을 담고 있을 수 있습니다. 그래서 수정 시 postId metadata로 기존 벡터를 삭제한 뒤 새 제목과 본문으로 다시 chunk를 만들고 임베딩해서 저장해야 합니다.'
            )
    ) AS seed_post(title, content)
),
inserted_posts AS (
    INSERT INTO posts (user_id, title, content, created_at, updated_at)
    SELECT seed_user.id, seed_post.title, seed_post.content, now(), now()
    FROM seed_user
    CROSS JOIN seed_post_data seed_post
    WHERE NOT EXISTS (
        SELECT 1
        FROM posts
        WHERE posts.user_id = seed_user.id
          AND posts.title = seed_post.title
    )
    RETURNING id, title
),
all_seed_posts AS (
    SELECT id, title FROM inserted_posts
    UNION
    SELECT posts.id, posts.title
    FROM posts
    JOIN seed_user
        ON seed_user.id = posts.user_id
    JOIN seed_post_data
        ON seed_post_data.title = posts.title
)
INSERT INTO post_tags (post_id, tag_id)
SELECT all_seed_posts.id, all_tags.id
FROM all_seed_posts
JOIN (
    VALUES
        ('Spring Security JWT 로그인 흐름 정리', 'spring'),
        ('Spring Security JWT 로그인 흐름 정리', 'jwt'),
        ('Spring AI RAG 게시판 Q&A 봇 구현 메모', 'spring'),
        ('Spring AI RAG 게시판 Q&A 봇 구현 메모', 'rag'),
        ('Spring AI RAG 게시판 Q&A 봇 구현 메모', 'openai'),
        ('pgvector와 PostgreSQL vector_store 역할', 'pgvector'),
        ('pgvector와 PostgreSQL vector_store 역할', 'database'),
        ('pgvector와 PostgreSQL vector_store 역할', 'rag'),
        ('OpenAI 임베딩 API가 필요한 이유', 'openai'),
        ('OpenAI 임베딩 API가 필요한 이유', 'rag'),
        ('MCP GitHub 조회 기능 설계', 'mcp'),
        ('MCP GitHub 조회 기능 설계', 'spring'),
        ('AI Agent 작성 보조 기능 흐름', 'agent'),
        ('AI Agent 작성 보조 기능 흐름', 'rag'),
        ('React 게시글 목록과 상세 화면 구성', 'react'),
        ('React 게시글 목록과 상세 화면 구성', 'frontend'),
        ('Docker Compose로 로컬 개발 환경 실행하기', 'docker'),
        ('Docker Compose로 로컬 개발 환경 실행하기', 'database'),
        ('주식 투자 게시글', 'stock'),
        ('게시글 수정 시 RAG 인덱스도 갱신해야 하는 이유', 'rag'),
        ('게시글 수정 시 RAG 인덱스도 갱신해야 하는 이유', 'pgvector')
) AS post_tag(title, tag_name)
    ON post_tag.title = all_seed_posts.title
JOIN all_tags
    ON all_tags.name = post_tag.tag_name
ON CONFLICT (post_id, tag_id) DO NOTHING;
