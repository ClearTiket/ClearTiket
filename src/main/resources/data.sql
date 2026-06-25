INSERT INTO tags (tag_id, tag_name, display_name, tag_category) VALUES
                                                                    (101, 'musical', '뮤지컬', 'GENRE'),
                                                                    (102, 'concert', '콘서트', 'GENRE'),
                                                                    (103, 'classic', '클래식', 'GENRE'),

                                                                    (201, 'tearburst', '눈물폭발', 'VIBE'),
                                                                    (202, 'earpleasure', '귀호강', 'VIBE'),
                                                                    (203, 'majestic', '웅장한', 'VIBE'),
                                                                    (204, 'relief', '스트레스해소', 'VIBE'),
                                                                    (205, 'thrill', '심장쫄깃', 'VIBE'),
                                                                    (206, 'funny', '배꼽빠지는', 'VIBE'),
                                                                    (207, 'spectacle', '화려한볼거리', 'VIBE'),
                                                                    (208, 'comfort', '잔잔한위로', 'VIBE'),
                                                                    (209, 'beginner', '입문자추천', 'VIBE'),

                                                                    (301, 'alone', '혼자서', 'WITH'),
                                                                    (302, 'couple', '연인과 함께', 'WITH'),
                                                                    (303, 'parents', '부모님과 함께 ', 'WITH'),
                                                                    (304, 'friends', '친구와 함께', 'WITH'),
                                                                    (305, 'kids', '아이와 함께', 'WITH')
ON CONFLICT (tag_id) DO NOTHING;

-- ──────────────────────────────────────────────────────────────────
-- seats 시드 데이터
-- 구역: A(왼쪽) / B(중앙) / C(오른쪽)
-- 등급: VIP(A~B행) / R(C~F행) / S(G~J행)
-- 가격: VIP 160,000 / R 140,000 / S 110,000
-- performance_id=1, venue_id=1 기준 (환경에 맞게 수정)
-- ──────────────────────────────────────────────────────────────────
INSERT INTO seats (performance_id, venue_id, seat_grade, section_name, row_num, seat_num, price)
SELECT
    1 AS performance_id,
    1 AS venue_id,
    CASE
        WHEN row_label IN ('A','B')           THEN 'VIP'
        WHEN row_label IN ('C','D','E','F')   THEN 'R'
        ELSE 'S'
        END AS seat_grade,
    section_name,
    row_label  AS row_num,
    CAST(col_num AS VARCHAR) AS seat_num,
    CASE
        WHEN row_label IN ('A','B')           THEN 160000
        WHEN row_label IN ('C','D','E','F')   THEN 140000
        ELSE 110000
        END AS price
FROM (
         SELECT
             s  AS section_name,
             r  AS row_label,
             c  AS col_num
         FROM
             UNNEST(ARRAY['A','B','C'])                                    AS s,
             UNNEST(ARRAY['A','B','C','D','E','F','G','H','I','J']) AS r,
             generate_series(1, 10)                                        AS c
     ) sub
ON CONFLICT DO NOTHING;

SELECT schedule_id, performance_id, show_date, show_time, round_number
FROM schedules
WHERE performance_id = 1;