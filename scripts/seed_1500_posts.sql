BEGIN;

WITH topic_pool AS (
    SELECT *
    FROM (
        VALUES
            (1, '스프링부트', 'spring,backend,api'),
            (2, '리액트', 'react,frontend,ui'),
            (3, '포스트그레SQL', 'postgresql,database,sql'),
            (4, 'JWT 인증', 'jwt,auth,security'),
            (5, 'RAG 실험', 'rag,ai,search'),
            (6, '배포 경험', 'deploy,vercel,render'),
            (7, '댓글 기능', 'comment,crud,backend'),
            (8, '태그 기능', 'tag,filter,search'),
            (9, '검색 구현', 'search,query,ux'),
            (10, '페이징 처리', 'paging,list,performance'),
            (11, '에러 해결', 'debug,error,log'),
            (12, '테스트 코드', 'test,junit,quality'),
            (13, 'CSS 레이아웃', 'css,layout,responsive'),
            (14, 'TypeScript', 'typescript,type,frontend'),
            (15, 'JPA 매핑', 'jpa,entity,orm'),
            (16, '프로젝트 회고', 'retrospective,project,study'),
            (17, '성능 최적화', 'performance,optimization,server'),
            (18, '파일 구조 정리', 'structure,refactor,maintain')
    ) AS topics(topic_id, topic_name, tag_csv)
),
author_pool AS (
    SELECT *
    FROM (
        VALUES
            (1, 'woojin', '우진'),
            (2, 'tester2', '테스터2'),
            (3, 'tester3', '테스터3'),
            (4, 'backend_mentor', '백엔드멘토'),
            (5, 'frontend_mentor', '프론트멘토'),
            (6, 'db_lab', '디비연구원'),
            (7, 'deploy_runner', '배포러너'),
            (8, 'ai_note', 'AI노트'),
            (9, 'student_a', '학생A'),
            (10, 'student_b', '학생B'),
            (11, 'student_c', '학생C'),
            (12, 'study_log', '스터디로그')
    ) AS authors(author_id, owner_login_id, author_name)
),
generated_posts AS (
    INSERT INTO posts (title, content, author_name, created_at, owner_login_id)
    SELECT
        format('%s 학습 기록 %s', t.topic_name, gs.i) AS title,
        format(
            '%s 주제로 작성한 %s번째 테스트 게시글입니다. 구현 과정에서 겪은 내용, 배운 점, 주의할 점을 정리했습니다. API 연결, 화면 동작, 예외 처리, 리팩터링 아이디어까지 함께 메모해 두었습니다. 이 데이터는 게시글 목록, 상세조회, 검색, 페이징, 태그, RAG 테스트를 위해 생성되었습니다.',
            t.topic_name,
            gs.i
        ) AS content,
        a.author_name,
        NOW() - make_interval(days => (gs.i % 120), hours => (gs.i % 24), mins => (gs.i % 60)) AS created_at,
        a.owner_login_id
    FROM generate_series(1, 1500) AS gs(i)
    JOIN topic_pool t
      ON ((gs.i - 1) % 18) + 1 = t.topic_id
    JOIN author_pool a
      ON ((gs.i - 1) % 12) + 1 = a.author_id
    RETURNING id, title
),
post_topic_map AS (
    SELECT
        gp.id AS post_id,
        tp.tag_name
    FROM generated_posts gp
    JOIN LATERAL (
        SELECT
            ((gp.id - 1) % 18) + 1 AS topic_id,
            unnest(string_to_array((
                SELECT tag_csv
                FROM topic_pool
                WHERE topic_id = ((gp.id - 1) % 18) + 1
            ), ',')) AS tag_name
    ) tp ON TRUE
),
insert_tags AS (
    INSERT INTO tags (name)
    SELECT DISTINCT tag_name
    FROM post_topic_map
    ON CONFLICT (name) DO NOTHING
    RETURNING id, name
),
all_tags AS (
    SELECT id, name
    FROM tags
    WHERE name IN (SELECT DISTINCT tag_name FROM post_topic_map)
),
insert_post_tags AS (
    INSERT INTO post_tags (post_id, tag_id)
    SELECT DISTINCT ptm.post_id, at.id
    FROM post_topic_map ptm
    JOIN all_tags at
      ON at.name = ptm.tag_name
    ON CONFLICT DO NOTHING
)
SELECT COUNT(*) AS inserted_posts
FROM generated_posts;

COMMIT;
