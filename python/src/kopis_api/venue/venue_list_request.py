import os
from datetime import datetime

import math
import pandas as pd
from pathlib import Path
import requests
import time
import xmltodict

from config.config import API_CONFIG, CACHE_DIR

KOPIS_REQUEST_URL = "http://kopis.or.kr/openApi/restful/prfplc"

def run_venue_list_request(afterdate:str | None=None):
    print("(API)공연장 전체 목록 가져오기 시작...")

    params = {
        "service": API_CONFIG["key"],
        "rows": 100  # 한 번에 KOPIS에 요청할 수 있는 최대치
    }

    if afterdate:
        params["afterdate"] = afterdate

    output_file_name = f"venue_list_working.csv"
    output_path = Path.joinpath(CACHE_DIR, output_file_name)

    if os.path.exists(output_path):
        result_df = pd.read_csv(output_path)
        page = math.ceil(len(result_df) / 100) + 1
    else:
        result_df = pd.DataFrame()
        page = 1

    looping = True

    try:
        while looping:
            print(f"\r  [가져오는 중] Page: {page} | 가져온 데이터 수: {len(result_df)}", end="", flush=True)

            params["cpage"] = page

            while True:  # API 요청 실패 시 재시도를 위한 반복문
                response = requests.get(KOPIS_REQUEST_URL, params=params)
                response.encoding = "utf-8"
                time.sleep(0.5)  # API 요청 간격이 너무 짧을 시 요청이 거부될 수 있음

                if response.ok:
                    xml_text = response.text
                    xml_dict = xmltodict.parse(xml_text)

                    # 데이터가 없다면 마지막 페이지로 간주하고 작업 종료
                    if xml_dict["dbs"] is None:
                        looping = False
                        break

                    data = xml_dict["dbs"]["db"]

                    if isinstance(data, dict):
                        data = [data]

                    result_df = pd.concat([result_df, pd.DataFrame(data)])  # 기존 df와 가져온 100건의 데이터 합치기

                    page += 1
                    break


        print(f"\n  요청 완료. 전체 데이터 수: {len(result_df)}")
    finally:
        if len(result_df) > 0:
            # 중간 결과 csv 저장
            result_df.to_csv(output_path, index=False, encoding="utf-8-sig")
            print(f"\n  저장 완료: {output_path}")

    return result_df
