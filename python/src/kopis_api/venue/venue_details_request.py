import os
from datetime import datetime
import pandas as pd
from pathlib import Path
import requests
import time
import xmltodict

from config.config import API_CONFIG, CACHE_DIR

KOPIS_REQUEST_URL = "http://www.kopis.or.kr/openApi/restful/prfplc/"

def run_venue_detail_request(df_venue_list):
    print("(API)공연장 상세 데이터 가져오기 시작...")

    params = {
        "service": API_CONFIG["key"]
    }

    venue_ids = df_venue_list["mt10id"]

    output_file_name = f"venue_detail_working.csv"
    output_path = Path.joinpath(CACHE_DIR, output_file_name)

    if os.path.exists(output_path):
        df_venue_detail = pd.read_csv(output_path)
        processed_ids = set(df_venue_detail["mt10id"].astype(str).tolist())
    else:
        df_venue_detail = pd.DataFrame()
        processed_ids = set()

    print(f"  전체 데이터 수: {len(venue_ids)}")

    try:
        for i, venue_id in enumerate(venue_ids):
            print(f"\r  [가져오는 중] {(i + 1.) / len(venue_ids) * 100:>5.1f}% | {i + 1:>4}/{len(venue_ids)} | {venue_id}", end="", flush=True)

            if venue_id in processed_ids:
                continue

            while True:
                response = requests.get(f"{KOPIS_REQUEST_URL}{venue_id}", params=params)
                response.encoding = "utf-8"
                time.sleep(0.5) # API 요청 간격이 너무 짧을 시 요청이 거부될 수 있음

                if response.ok:
                    xml_text = response.text
                    xml_dict = xmltodict.parse(xml_text)
                    data = xml_dict["dbs"]["db"]
                    if data["mt13s"] is None:
                        data["mt13s"] = {'mt13': [{}]}
                    df_venue_detail = pd.concat([df_venue_detail, pd.DataFrame([data])])
                    break
    finally:
        if len(df_venue_detail) > 0:
            df_venue_detail.to_csv(output_path, index=False, encoding="utf-8-sig")
            print(f"\n  저장 완료: {output_path}")

    return df_venue_detail
