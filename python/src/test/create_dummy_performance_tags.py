import psycopg2
import random

from config.config import DB_CONFIG, ES_CONFIG

pg_conn = psycopg2.connect(
    host=DB_CONFIG["host"],
    database=DB_CONFIG["database"],
    user=DB_CONFIG["username"],
    password=DB_CONFIG["password"],
    port=DB_CONFIG["port"]
)
pg_cursor = pg_conn.cursor()

# 사용할 태그 ID
tags_200 = list(range(201, 210))  # 201~209
tags_300 = list(range(301, 306))  # 301~305

# 모든 product_id 조회
pg_cursor.execute("SELECT performance_id FROM performances")
product_ids = [row[0] for row in pg_cursor.fetchall()]

insert_data = []

for product_id in product_ids:
    # 200번대 태그 1~3개
    selected_200 = random.sample(
        tags_200,
        random.randint(1, 3)
    )

    # 300번대 태그 1~5개
    selected_300 = random.sample(
        tags_300,
        random.randint(1, 5)
    )

    # INSERT할 데이터 생성
    for tag_id in selected_200 + selected_300:
        insert_data.append((tag_id, product_id))

# 일괄 INSERT
pg_cursor.executemany(
    """
    INSERT INTO performance_tags (tag_id, performance_id, ai_score)
    VALUES (%s, %s, 0.9)
    """,
    insert_data
)

pg_conn.commit()

print(f"{len(insert_data)}개의 tags 데이터가 삽입되었습니다.")

pg_cursor.close()
pg_conn.close()