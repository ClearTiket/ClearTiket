import ast
from datetime import datetime
import pandas as pd
from pathlib import Path

from config.config import CACHE_DIR

def __mt13s_json_normalize(df):
    df["mt13s"] = df["mt13s"].apply(__mt13s_safe_parse_and_standardize)

    # 2. DataFrame을 다시 딕셔너리 리스트로 변환
    records = df.to_dict('records')

    # 3. json_normalize를 통해 데이터 펼치기
    result_df = pd.json_normalize(
        records,
        record_path=['mt13s', "mt13"],  # 여러 행으로 쪼갤(Unnest) 타겟 리스트의 경로
        meta=['mt10id', 'telno', 'relateurl', 'adres', 'la', 'lo']  # 쪼개지는 행들과 함께 그대로 복사해서 유지할 컬럼
    )

    return result_df
    # result_df.to_csv("ven_prepro_test.csv", index=False, encoding='utf-8')

def __merge_venue_list_and_detail(df1, df2):
    result_df = pd.merge(df1, df2, on='mt10id', how='left')
    return result_df

def __venue_drop_and_rename_columns(df):
    name_change_cols = {
        "mt13id": "kopis_id",
        "seatscale": "capacity",
        "telno": "telnum",
        "relateurl": "relateurl",
        "adres": "address",
        "la": "lat",
        "lo": "lon",
        "sidonm": "region"
    }
    drop_cols = [
        "prfplcnm",
        "stageorchat",
        "stagepracat",
        "stagedresat",
        "stageoutdrat",
        "disabledseatscale",
        "stagearea",
        "mt10id",
        "fcltynm",
        "mt13cnt",
        "fcltychartr",
        "gugunnm",
        "opende"
    ]
    df["name"] = (df["fcltynm"] + " - " + df["prfplcnm"]).fillna(df["fcltynm"])
    df["mt13id"] = df["mt13id"].fillna(df["mt10id"] + "-01")
    df = df.drop(columns=drop_cols)
    df = df.rename(columns=name_change_cols)
    return df

def __mt13s_safe_parse_and_standardize(val):
    if pd.isna(val):
        return {"mt13": [{}]}

    if isinstance(val, str):
        parsed_val = ast.literal_eval(val)
    else:
        parsed_val = val
    # 구조 통일하기 (단일 딕셔너리면 리스트로 감싸기)
    # 'data' 키의 값이 딕셔너리 타입이라면 리스트로 한 겹 감싸줍니다.
    if isinstance(parsed_val.get('mt13'), dict):
        parsed_val['mt13'] = [parsed_val['mt13']]
    return parsed_val

def __capacity_str_to_int(val):
    val_str = str(val)
    if val_str is None or val_str == "nan":
        return '0'
    else:
        return val_str.replace(',', '')

def run_venue_preprocessing(df_venue_list, df_venue_detail):
    print("공연장 데이터 전처리 시작...")
    print("  공연 시설 정보 분리...")
    df_mt13s_normalized = __mt13s_json_normalize(df_venue_detail)
    df_merged = __merge_venue_list_and_detail(df_mt13s_normalized, df_venue_list)
    print("  불필요 컬럼 제거 및 컬럼명 재설정...")
    df_column_refined = __venue_drop_and_rename_columns(df_merged)
    print("  수용 인원 컬럼 변환(str -> int)...")
    df_column_refined["capacity"] = df_column_refined["capacity"].apply(__capacity_str_to_int)

    output_file_name = f"venue_preprocessed_{datetime.now().strftime('%Y-%m-%d')}.csv"
    output_path = Path.joinpath(CACHE_DIR, output_file_name)
    df_column_refined.to_csv(output_path, index=False, encoding='utf-8')
    print(f"  저장 완료: {output_path}")

    return df_column_refined
