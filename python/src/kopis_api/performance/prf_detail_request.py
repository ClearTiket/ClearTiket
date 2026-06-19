import os
from datetime import datetime
import pandas as pd
from pathlib import Path
import requests
import time
import xmltodict

from config.config import API_CONFIG, CACHE_DIR

KOPIS_REQUEST_URL = "http://www.kopis.or.kr/openApi/restful/pblprfr/"

def run_prf_detail_request(df_prf_list):
    print("(API)공연 상세 데이터 가져오기 시작...")

    params = {
        "service": API_CONFIG["key"],
    }

    prf_ids = df_prf_list["mt20id"]

    output_file_name = f"prf_detail_working.csv"
    output_path = Path.joinpath(CACHE_DIR, output_file_name)

    if os.path.exists(output_path):
        df_prf_detail = pd.read_csv(output_path)
        processed_id = set(df_prf_detail["mt20id"].astype(str).tolist())
    else:
        df_prf_detail = pd.DataFrame()
        processed_id = set()

    print(f"  전체 데이터 수: {len(prf_ids)}")
    try:
        for i, prf_id in enumerate(prf_ids):
            print(f"\r  [가져오는 중] {(i + 1.) / len(prf_ids) * 100:>5.1f}% | {i + 1:>5}/{len(prf_ids)} | {prf_id}", end="", flush=True)

            if prf_id in processed_id:
                continue

            while True:
                response = requests.get(f"{KOPIS_REQUEST_URL}{prf_id}", params=params)
                response.encoding = "utf-8"
                time.sleep(0.5)  # API 요청 간격이 너무 짧을 시 요청이 거부될 수 있음

                if response.ok:
                    xml_text = response.text
                    xml_dict = xmltodict.parse(xml_text)
                    data = xml_dict["dbs"]["db"]

                    # data를 list([])로 감싸지 않을 경우 response로 받아온 데이터 구조상 두 행으로 쪼개져서 저장됨
                    df_prf_detail = pd.concat([df_prf_detail, pd.DataFrame([data])])
                    break
    finally:
        if len(df_prf_detail) > 0:
            df_prf_detail.to_csv(output_path, index=False, encoding="utf-8-sig")
            print(f"\n  저장 완료: {output_path}")

    return df_prf_detail
