import os
from datetime import datetime, timedelta
from sqlalchemy import create_engine
import math
import pandas as pd
from pathlib import Path
import requests
import time
import xmltodict
from sqlalchemy.dialects.postgresql import insert

from config.config import API_CONFIG, CACHE_DIR, DB_CONFIG

KOPIS_REQUEST_URL = "http://kopis.or.kr/openApi/restful/boxoffice"

def __get_db_url():
    db_datasource = DB_CONFIG["datasource"]
    db_username = DB_CONFIG["username"]
    db_password = DB_CONFIG["password"]
    db_host = DB_CONFIG["host"]
    db_port = DB_CONFIG["port"]
    db_name = DB_CONFIG["database"]

    db_url = f"{db_datasource}://{db_username}:{db_password}@{db_host}:{db_port}/{db_name}"
    return db_url

def __get_column_from_database(df, id_column_name, table_name, target_column_name):
    db_url = __get_db_url()
    engine = create_engine(db_url)

    kopis_id_tuple = tuple(df[id_column_name].dropna().unique())
    as_col_name = "k_id"
    query = f"""
        SELECT {target_column_name}, kopis_id AS {as_col_name}
        FROM {table_name}
        WHERE kopis_id IN %(id_list)s
    """

    db_df = pd.read_sql(query, con=engine, params={'id_list': kopis_id_tuple})

    result_df = pd.merge(db_df, df, left_on=as_col_name, right_on=id_column_name, how="inner")
    result_df = result_df.drop(columns=[as_col_name])
    return result_df

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

def __df_to_database(df: pd.DataFrame, table_name):
    db_url = __get_db_url()
    engine = create_engine(db_url)

    df.to_sql(
        name=table_name,
        con=engine,
        if_exists="append",
        index=False,
        chunksize=500,
        method=__postgres_upsert if table_name == "performances" else None
    )

def prf_ranking_preprocessing(df_prf_ranking):
    drop_cols = ["cate", "prfnm", "prfpd", "prfdtcnt", "area", "prfplcnm", "seatcnt", "poster"]
    name_change_cols = {
        "rnum": "ranking",
        "mt20id": "kopis_id",
    }

    df_prf_ranking = df_prf_ranking.drop(columns=drop_cols)
    df_prf_ranking = df_prf_ranking.rename(columns=name_change_cols)

    df_prf_ranking = __get_column_from_database(df_prf_ranking, "kopis_id", "performances", "performance_id")

    __df_to_database(df_prf_ranking, "rankings")

    return df_prf_ranking

def run_prf_ranking_request(genres=None, end_date=None):
    print("(API)공연 랭킹 가져오기 시작")

    if genres is None:
        genres = [("CCCA", "클래식"), ("CCCD", "콘서트"), ("GGGA", "뮤지컬"), ("", "전체")]

    dateformat = "%Y%m%d"
    if end_date is None:
        end_date = datetime.today()

    end_date_str = end_date.strftime(dateformat)
    start_date_1day = end_date.strftime(dateformat)
    start_date_1week = (end_date - timedelta(days=6)).strftime(dateformat)
    start_date_1month = (end_date - timedelta(days=30)).strftime(dateformat)

    periods = [(start_date_1day, "daily"), (start_date_1week, "weekly"), (start_date_1month, "monthly")]

    output_file_name = "prf_ranking_working.csv"
    output_path = Path.joinpath(CACHE_DIR, output_file_name)

    result_df = pd.DataFrame()

    params = {
        "service": API_CONFIG["key"],
        "eddate": end_date_str
    }

    try:
        for period_str, period_name in periods:
            print(f"  [{period_name}] 랭킹 가져오기 시작...")
            params["stdate"] = period_str

            for genre_code, genre_name in genres:
                params["catecode"] = genre_code

                while True:
                    response = requests.get(KOPIS_REQUEST_URL, params=params)
                    response.encoding = "utf-8"
                    time.sleep(0.5)

                    if response.ok:
                        xml_text = response.text
                        xml_dict = xmltodict.parse(xml_text)

                        if xml_dict["boxofs"] is None: break

                        data = xml_dict["boxofs"]["boxof"]

                        if isinstance(data, dict):
                            data = [data]

                        df = pd.DataFrame(data)
                        df["period"] = period_name
                        df["genre"] = genre_name

                        result_df = pd.concat([result_df, df])

                        break

        print("  전체 작업 완료.")
        print(f"  총 데이터 수: {len(result_df)}")
    finally:
        pass
        if len(result_df) > 0:
            result_df.to_csv(output_path, index=False, encoding="utf-8-sig")
            print(f"  저장 완료: {output_path}")

    return result_df

if __name__ == "__main__":
    # ddff = pd.read_csv("C:/Users/tj/IdeaProjects/Clearticket/python/.data_cache/prf_ranking_working.csv")
    df = run_prf_ranking_request(end_date=datetime(2026, 6, 22))
    prf_ranking_preprocessing(df)
