import json
import re
import pandas as pd
from pathlib import Path
from datetime import datetime, timedelta
from sqlalchemy import create_engine
from sqlalchemy.dialects.postgresql import insert
import holidays

from config.config import CACHE_DIR, DB_CONFIG

def __performance_drop_and_rename_columns(df):
    name_change_cols = {
        "mt20id": "kopis_id",
        "mt13id": "venues.kopis_id",
        "prfnm": "title",
        "prfpdfrom": "start_date",
        "prfpdto": "end_date",
        "prfcast": "castings",
        "prfruntime": "runtime",
        "entrpsnm": "enterprise_name",
        "pcseguidance": "seats.price",
        "poster": "poster_url",
        "sty": "extracted_text",
        "area": "region",
        "genrenm": "genre",
        "prfstate": "status",
        "dtguidance": "schedules.show_time",
        "styurls": "intro_image_url"
    }
    drop_cols = [
        "frstregdt",
        "prfcrew",
        "prfage",
        "fcltynm",
        "entrpsnmP",
        "entrpsnmA",
        "entrpsnmH",
        "entrpsnmS",
        "openrun",
        "visit",
        "child",
        "daehakro",
        "festival",
        "musicallicense",
        "musicalcreate",
        "updatedate",
        "mt10id",
        "relates"
    ]
    df = df.rename(columns=name_change_cols)
    df = df.drop(columns=drop_cols)

    return df

def __time_text_to_minutes(text):
    match = re.search(r"(?:(\d+)시간)?\s*(?:(\d+)분)?", str(text))
    if match:
        parsed_time = match.groups('0')
        minutes = int(parsed_time[0]) * 60 + int(parsed_time[1])
    else:
        minutes = 0
    return minutes

def __parse_ticket_prices(text):
    text = str(text)
    # 1. 예외 상황 표준화 ('무료'를 ' 0원'으로 바꿔서 규칙을 통일합니다)
    text = text.replace("무료", " 0원")

    result = {}

    # 2. 항목을 구분하는 '원, '을 기준으로 문자열을 분리합니다.
    # 이렇게 하면 '토요일, 공휴일' 안의 쉼표는 건드리지 않습니다.
    items = text.split("원, ")

    for item in items:
        # 맨 마지막 항목에는 '원'이 남아있으므로 지워주고 양옆 공백을 정리합니다.
        item = item.replace("원", "").strip()

        if not item:
            continue

        # 3. rsplit(" ", 1) : 문자열의 오른쪽(뒤)에서부터 딱 1번만 띄어쓰기를 기준으로 자릅니다.
        # 예: "토요일, 공휴일 50,000" -> ["토요일, 공휴일", "50,000"]
        parts = item.rsplit(" ", 1)

        if len(parts) == 2:
            key = parts[0].strip()  # "토요일, 공휴일"
            price_str = parts[1].replace(",", "")  # "50000"

            result[key] = int(price_str)

    return [result]

def __parse_schedules_show_time(text):
    """
    스케줄 문자열을 분석하여 파이썬 딕셔너리 형태의 룰셋으로 변환합니다.
    """
    # 파이썬 datetime 요일 기준에 맞춘 매핑 테이블 (0:월 ~ 6:일)
    weekday_map = {
        "월요일": 0, "화요일": 1, "수요일": 2, "목요일": 3,
        "금요일": 4, "토요일": 5, "일요일": 6, "HOL": 6
    }

    rule_mapping = {}

    # 1. 정규표현식으로 패턴 추출
    # 그룹 1: 요일, 요일범위, HOL (예: '화요일', '목요일 ~ 금요일', 'HOL')
    # 그룹 2: 괄호 안의 시간들 (예: '20:00', '16:00,20:00')
    matches = re.findall(r"([가-힣A-Z\s~]+)\(([\d:,]+)\)", str(text))

    for day_part, time_part in matches:
        day_part = day_part.strip()  # 앞뒤 공백 제거
        times = time_part.split(",")  # 시간을 리스트로 변환 ['16:00', '20:00']

        # 조건 1: 공휴일(HOL)인 경우
        if day_part == "HOL":
            rule_mapping["HOL"] = times

        # 조건 2: '~' 기호가 포함된 범위인 경우 (예: 목요일 ~ 금요일)
        elif "~" in day_part:
            start_day, end_day = [d.strip() for d in day_part.split("~")]
            start_idx = weekday_map[start_day]
            end_idx = weekday_map[end_day]

            # 시작 요일부터 종료 요일까지 반복하며 딕셔너리에 추가
            # (만약 '금요일 ~ 일요일' 이라면 4, 5, 6이 됨)
            for i in range(start_idx, end_idx + 1):
                rule_mapping[i] = times

        # 조건 3: 단일 요일인 경우
        else:
            rule_mapping[weekday_map[day_part]] = times

    return rule_mapping

def __create_time_table(start_date, end_date, show_time):
    # 3. 2026년 5월의 공휴일 지정 (예시 출력 기준 5/1 노동절 포함, 5/5 어린이날, 5/24 부처님오신날 등)
    # hols = {"2026.05.01", "2026.05.05", "2026.05.24", "2026.05.25"}

    kr_holidays = holidays.KR(years=2026)
    hols = kr_holidays.keys()

    # 4. 날짜 문자열을 datetime 객체로 변환
    start_dt = datetime.strptime(start_date, "%Y.%m.%d").date()
    end_dt = datetime.strptime(end_date, "%Y.%m.%d").date()

    schedules = []
    schedule_no = 1
    current_dt = start_dt

    # 5. 시작일부터 종료일까지 하루씩 루프를 돕니다.

    while current_dt <= end_dt:
        # date_str = current_dt.strftime("%Y.%m.%d")
        weekday_num = current_dt.weekday()  # 0 ~ 6 반환

        # 공휴일인지 먼저 체크 (공휴일 우선순위 적용)
        # if date_str in hols:
        if current_dt in hols:
            target_times = show_time.get("HOL", [])
        else:
            target_times = show_time.get(weekday_num, [])

        # 해당 날짜에 배정된 시간만큼 스케줄 항목 생성
        for t in target_times:
            schedules.append({
                "schedule_no": schedule_no,
                "date": current_dt.strftime("%Y.%m.%d"),
                "time": t
            })
            schedule_no += 1

        # 다음 날로 이동
        current_dt += timedelta(days=1)

    # 6. 최종 결과를 JSON 형태로 변환
    output_data = {"schedules": schedules}
    return output_data
    # print(json.dumps(output_data, indent=2, ensure_ascii=False))

def __change_genre_name(text):
    if text == "서양음악(클래식)":
        return "클래식"
    elif text == "대중음악":
        return "콘서트"
    else:
        return text

def __change_status_name(text):
    if text == "공연예정":
        return "PREPARING"
    elif text == "공연중":
        return "ON_SALE"
    elif text == "공연완료":
        return "CLOSED"
    else:
        return "CLOSED"

def __json_normalize_schedules(df, record_path):
    records = df.to_dict('records')
    result_df = pd.json_normalize(
        records,
        record_path=record_path,
        meta=["kopis_id"]
    )
    result_df = result_df.rename(columns={
        "schedule_no": "round_number",
        "date": "show_date",
        "time": "show_time"
    })
    return result_df

def __json_normalize_seats(df):
    records = df.to_dict('records')

    parsed_rows = []
    for item in records:
        current_id = item["kopis_id"]
        for data_dict in item["seats.price"]:
            for key, val in data_dict.items():
                parsed_rows.append({
                    "kopis_id": current_id,
                    "seat_grade": key,
                    "price": val
                })

    result_df = pd.DataFrame(parsed_rows)
    return result_df

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

def run_prf_preprocessing(df_prf_detail):
    print(f"공연 데이터 전처리 시작...")
    # print("    csv 파일 불러오는 중...")
    # df = pd.read_csv(f"prf_detail_{genre}.csv")
    print("  컬럼명 재설정 및 불필요 컬럼 제거...")
    df_prf_detail = __performance_drop_and_rename_columns(df_prf_detail)

    print("  장르명, 공연상태 텍스트 재설정...")
    df_prf_detail["genre"] = df_prf_detail["genre"].apply(__change_genre_name)
    df_prf_detail["status"] = df_prf_detail["status"].apply(__change_status_name)

    print("  공연 시간, 가격 파싱...")
    df_prf_detail["runtime"] = df_prf_detail["runtime"].apply(__time_text_to_minutes)
    df_prf_detail["seats.price"] = df_prf_detail["seats.price"].apply(__parse_ticket_prices)

    print("  가격, 시간표 정보 분리...")
    df_prf_detail["schedules.show_time"] = df_prf_detail["schedules.show_time"].apply(__parse_schedules_show_time)
    df_prf_detail["schedules.show_time"] = df_prf_detail.apply(
        lambda x: __create_time_table(x["start_date"], x["end_date"], x["schedules.show_time"]),
        axis=1
    )

    df_schedules = df_prf_detail[["kopis_id", "schedules.show_time"]]
    df_seats = df_prf_detail[["kopis_id", "seats.price"]]
    df_prf_detail = df_prf_detail.drop(columns=["schedules.show_time", "seats.price"])

    print("  공연 데이터 DB 적재...")
    df_prf_detail = __get_column_from_database(df_prf_detail, "venues.kopis_id", "venues", "venue_id")
    df_prf_detail = df_prf_detail.drop(columns=["venues.kopis_id"])
    df_prf_detail["intro_image_url"] = df_prf_detail["intro_image_url"].apply(
        lambda x: json.dumps(x, ensure_ascii=False)
        if isinstance(x, (dict, list))
        else x
    )
    __df_to_database(df_prf_detail, "performances")

    print("  가격, 시간표 정규화...")
    df_schedules = __json_normalize_schedules(df_schedules, ["schedules.show_time", "schedules"])
    df_seats = __json_normalize_seats(df_seats)

    df_schedules = __get_column_from_database(df_schedules, "kopis_id", "performances", "performance_id")
    df_seats = __get_column_from_database(df_seats, "kopis_id", "performances", "performance_id")
    df_seats = pd.merge(df_seats, df_prf_detail[["venue_id", "kopis_id"]], on="kopis_id", how="inner")

    print("  가격, 시간표 데이터 DB 적재...")
    df_schedules = df_schedules.drop(columns=["kopis_id"])
    __df_to_database(df_schedules, "schedules")
    df_seats = df_seats.drop(columns=["kopis_id"])
    __df_to_database(df_seats, "seats")

    print("  저장 중...")

    def save(df, save_str):
        output_file_name = f"{save_str}_preprocessed_{datetime.now().strftime("%Y-%m-%d")}.csv"
        output_path = Path.joinpath(CACHE_DIR, output_file_name)
        df.to_csv(output_path, index=False, encoding="utf-8-sig")
        print(f"    저장 완료: {output_path}")

    save(df_prf_detail, "prf")
    save(df_seats, "seat")
    save(df_schedules, "schedule")

    print(f"  작업 완료.")
