import os
from datetime import datetime

import math
import pandas as pd
from pathlib import Path
import requests
import time
import xmltodict

from config.config import API_CONFIG, CACHE_DIR

KOPIS_REQUEST_URL = "http://kopis.or.kr/openApi/restful/pblprfr"

def run_prf_list_request(genres=None, start_date=None, end_date=None, afterdate:str | None=None):
    print("(API)공연 전체 목록 가져오기 시작...")

    if genres is None:
        genres = [("CCCA", "클래식"), ("CCCD", "콘서트"), ("GGGA", "뮤지컬")]
        # KOPIS 장르 코드는 아래 링크 문서에서 확인
        # https://kopis.or.kr/upload/openApi/%EA%B3%B5%EC%97%B0%EC%98%88%EC%88%A0%ED%86%B5%ED%95%A9%EC%A0%84%EC%82%B0%EB%A7%9DOpenAPI%EA%B3%B5%ED%86%B5%EC%BD%94%EB%93%9C.pdf

    print("  날짜 설정 및 형식 검사...")
    date_format = "%Y%m%d"

    year = datetime.now().year
    if start_date is None:
        start_date = datetime(year, 6, 1).strftime(date_format)

    if end_date is None:
        end_date = datetime(year, 12, 31).strftime(date_format)

    start_date = datetime.strptime(start_date, date_format).strftime(date_format)
    end_date = datetime.strptime(end_date, date_format).strftime(date_format)

    params = {
        "service": API_CONFIG["key"],
        "stdate": start_date,
        "eddate": end_date,
        "rows": 100  # 한 번에 KOPIS에 요청할 수 있는 최대치
    }

    if afterdate:
        params["afterdate"] = afterdate

    output_file_name = f"prf_list_working.csv"
    output_path = Path.joinpath(CACHE_DIR, output_file_name)

    if os.path.exists(output_path):
        result_df = pd.read_csv(output_path)
    else:
        result_df = pd.DataFrame()

    try:
        for genre_code, genre_name in genres:
            print(f"  [{genre_name}] 목록 가져오기 시작...")

            params["shcate"] = genre_code

            if len(result_df) > 0 and len(result_df[result_df["genrenm"] == genre_name]) > 0:
                page = math.ceil(len(result_df[result_df["genrenm"] == genre_name]) / 100) + 1
            else:
                page = 1

            while True:
                params["cpage"] = page

                response = requests.get(KOPIS_REQUEST_URL, params=params)
                response.encoding = "utf-8"
                time.sleep(0.5)  # API 요청 간격이 너무 짧을 시 요청이 거부될 수 있음

                if response.ok:
                    xml_text = response.text
                    xml_dict = xmltodict.parse(xml_text)

                    # 데이터가 없다면 마지막 페이지로 간주하고 작업 종료
                    if xml_dict["dbs"] is None: break

                    data = xml_dict["dbs"]["db"]

                    if isinstance(data, dict):
                        data = [data]

                    df = pd.DataFrame(data)
                    df["genrenm"] = genre_name

                    result_df = pd.concat([result_df, df])
                    print(
                        f"\r    [가져오는 중] Page: {page} | 가져온 데이터 수: {len(result_df[result_df["genrenm"] == genre_name])}",
                        end="", flush=True)
                    page += 1

            print(f"\n  [{genre_name}] 요청 완료. 데이터 수: {len(result_df[result_df["genrenm"] == genre_name])}")
        print("  전체 작업 완료.")
        print(f"  총 데이터 수: {len(result_df)}")
    # 중간 결과 csv 저장
    finally:
        if len(result_df) > 0:
            result_df.to_csv(output_path, index=False, encoding="utf-8-sig")
            print(f"  저장 완료: {output_path}")

    return result_df
