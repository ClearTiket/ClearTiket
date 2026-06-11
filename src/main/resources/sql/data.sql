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