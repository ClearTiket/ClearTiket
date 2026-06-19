import json
import docker
import os
import glob
import time
import argparse
import traceback

import pandas as pd
from datetime import timedelta, datetime

from docker.errors import DockerException

from config.config import CACHE_DIR

# 공연장 데이터 처리 함수 임포트
from kopis_api.venue.venue_list_request import run_venue_list_request
from kopis_api.venue.venue_details_request import run_venue_detail_request
from kopis_api.venue.venue_preprocessing import run_venue_preprocessing
from kopis_api.venue.venue_data_to_db import run_venue_data_to_db

# 공연 데이터 처리 함수 임포트
from kopis_api.performance.prf_list_request import run_prf_list_request
from kopis_api.performance.prf_detail_request import run_prf_detail_request
from kopis_api.performance.prf_preprocessing import run_prf_preprocessing

# Elasticsearch 처리 함수 임포트
from elasticsearch_processing.db_to_es import create_es_index_with_nori, migrate_data_to_es

STATE_FILE = ".state.json"
def get_last_updated():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, "r") as f:
            state = json.load(f)
            return state.get("last_updated")

    default_date = None#(datetime.now() - timedelta(days=30)).strftime("%Y%m%d")
    return default_date

def save_current_status():
    with open(STATE_FILE, "w") as f:
        json.dump({"last_updated": datetime.now().strftime("%Y%m%d")}, f)


def clear_old_caches(days_to_keep=7):
    """지정한 날짜보다 오래된 캐시 파일 삭제"""
    now = time.time()
    for file_path in glob.glob(os.path.join(CACHE_DIR, "*.csv")):
        if os.stat(file_path).st_mtime < now - (days_to_keep * 86400):
            os.remove(file_path)
            print(f"오래된 csv 삭제 완료: {os.path.basename(file_path)}")

def archive_working_files():
    today_str = datetime.now().strftime("%Y%m%d")

    files_to_archive = {
        "venue_list_working.csv": f"venue_list_{today_str}.csv",
        "venue_detail_working.csv": f"venue_detail_{today_str}.csv",
        "prf_list_working.csv": f"prf_list_{today_str}.csv",
        "prf_detail_working.csv": f"prf_detail_{today_str}.csv",
    }

    for src_name, dst_name in files_to_archive.items():
        src_path = os.path.join(CACHE_DIR, src_name)
        dst_path = os.path.join(CACHE_DIR, dst_name)

        if os.path.exists(src_path):
            os.rename(src_path, dst_path)
            print(f"결과 파일 백업 완료: {src_name} -> {dst_name}")

def is_docker_running():
    try:
        client = docker.from_env()
        client.ping()
        return True
    except DockerException:
        return False

def main():
    # 🌟 터미널에서 --afterdate 옵션을 줄 수 있도록 설정
    # 실행 예시: python main.py --afterdate 2026-06-15
    # parser = argparse.ArgumentParser(description="공연 데이터 동기화 파이프라인")
    # parser.add_argument("--afterdate", type=str, default=None, help="이 날짜 이후로 변경된 데이터만 수집 (YYYY-MM-DD)")
    # args = parser.parse_args()

    # if not args.afterdate:
    #     args.afterdate = get_last_updated()

    afterdate = get_last_updated()

    print("=" * 30)
    print("공연 및 공연장 데이터 처리 파이프라인")
    print("=" * 30)

    try:
        print("\n----- 공연장 데이터 처리 시작 -----")
        venue_list = run_venue_list_request(afterdate)
        venue_detail = run_venue_detail_request(venue_list)
        venue_preprocessed = run_venue_preprocessing(venue_list, venue_detail)
        run_venue_data_to_db(venue_preprocessed)

        # 공연 테이블에서 공연장 PK를 FK로 참조하기 때문에
        # 공연장 데이터 DB 적재 완료 후 공연 데이터를 DB에 적재해야 함
        print("\n----- 공연 데이터 처리 시작-----")
        prf_list = run_prf_list_request(afterdate=afterdate)
        prf_detail = run_prf_detail_request(prf_list)
        run_prf_preprocessing(prf_detail)

        print("\n----- DB 데이터 Elasticsearch 적재 시작 -----")
        if is_docker_running():
            create_es_index_with_nori()
            migrate_data_to_es()
        else:
            print("Docker가 켜져있지 않음")
            print("Elasticsearch 적재 건너뜀")

        archive_working_files()
        save_current_status()

    except Exception as e:
        print(f"\n처리 중 오류 발생: {e}")
        traceback.print_exc()

if __name__ == "__main__":
    main()