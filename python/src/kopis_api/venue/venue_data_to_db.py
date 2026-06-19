from sqlalchemy import create_engine
from sqlalchemy.dialects.postgresql import insert

from config.config import DB_CONFIG

# UPSERT 처리를 위한 커스텀 함수 정의
def __postgres_upsert(table, conn, keys, data_iter):
    # 판다스가 제공하는 데이터 행(data_iter)을 딕셔너리 형태로 변환
    data = [dict(zip(keys, row)) for row in data_iter]

    # PostgreSQL 전용 INSERT 구문 생성
    insert_stmt = insert(table.table).values(data)

    # 충돌이 발생했을 때 업데이트할 컬럼 목록 작성 (충돌 기준인 'id' 컬럼은 제외)
    update_dict = {key: insert_stmt.excluded[key] for key in keys if key != 'id'}

    # ON CONFLICT (id) DO UPDATE SET ... 구문 완성
    upsert_stmt = insert_stmt.on_conflict_do_update(
        index_elements=['kopis_id'],  # 충돌을 감지할 기본키(PK) 혹은 유니크 컬럼명 지정
        set_=update_dict
    )

    # 쿼리 실행
    conn.execute(upsert_stmt)

def run_venue_data_to_db(df):
    print("DB에 저장 시작...")

    print("  DB 설정 불러오기...")
    db_datasource = DB_CONFIG["datasource"]
    db_username = DB_CONFIG["username"]
    db_password = DB_CONFIG["password"]
    db_host = DB_CONFIG["host"]
    db_port = DB_CONFIG["port"]
    db_name = DB_CONFIG["database"]

    db_url = f"{db_datasource}://{db_username}:{db_password}@{db_host}:{db_port}/{db_name}"
    engine = create_engine(db_url)

    print("  DB 연결 및 데이터 저장...")
    df.to_sql(
        name="venues",
        con=engine,
        if_exists="append",
        index=False,
        chunksize=500,
        method=__postgres_upsert
    )

    print("  작업 완료.")
# import pandas as pd
# df = pd.read_csv("C:/Users/tj/IdeaProjects/python-api-elasticsearch/.data_cache/venue_preprocessed_2026-06-19.csv")
# run_venue_data_to_db(df)