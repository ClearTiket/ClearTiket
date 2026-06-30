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

-- 테스트용: 1번 공연(영웅 갈라콘서트, schedule_id=1)에 좌석을 연결합니다.
-- seat_id 1~100 : VIP
INSERT INTO seats (seat_id, row_num, seat_num, price, seat_grade, section_name, performance_id)
SELECT ((r-1)*10 + s) AS seat_id,
       r::varchar, s::varchar, 160000, 'VIP', 'A', 1
FROM generate_series(1,10) r, generate_series(1,10) s
ON CONFLICT (seat_id) DO NOTHING;

-- seat_id 101~200 : R
INSERT INTO seats (seat_id, row_num, seat_num, price, seat_grade, section_name, performance_id)
SELECT 100 + ((r-1)*10 + s) AS seat_id,
       r::varchar, s::varchar, 140000, 'R', 'B', 1
FROM generate_series(1,10) r, generate_series(1,10) s
ON CONFLICT (seat_id) DO NOTHING;

-- seat_id 201~300 : S
INSERT INTO seats (seat_id, row_num, seat_num, price, seat_grade, section_name, performance_id)
SELECT 200 + ((r-1)*10 + s) AS seat_id,
       r::varchar, s::varchar, 110000, 'S', 'C', 1
FROM generate_series(1,10) r, generate_series(1,10) s
ON CONFLICT (seat_id) DO NOTHING;
