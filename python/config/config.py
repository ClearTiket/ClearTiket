import os
from pathlib import Path
import yaml

# 경로 설정
BASE_DIR = Path(__file__).resolve().parent.parent  # 현재 파일(config.py)이 위치한 디렉토리
YAML_FILE_PATH = Path.joinpath(BASE_DIR, "config.yaml")
CACHE_DIR = Path.joinpath(BASE_DIR, ".data_cache")

# 캐시 폴더 생성
os.makedirs(CACHE_DIR, exist_ok=True)

# 설정 데이터를 불러올 딕셔너리 선언
DB_CONFIG = {}
API_CONFIG = {}
ES_CONFIG = {}

# config.yaml 파일이 프로젝트 루트 경로에 없다면 오류를 발생시킴
if not Path.exists(YAML_FILE_PATH):
    raise FileNotFoundError(
        f"'config.yaml' 파일을 찾을 수 없음.\n"
        f"'config.yaml' 파일이 프로젝트 루트 경로에 있는지 확인"
    )

# config.yaml 파일 로드
with open(YAML_FILE_PATH, "r", encoding="utf-8") as f:
    try:
        config_data = yaml.safe_load(f)

        # 읽어온 yaml 파일을 딕셔너리로 추출
        DB_CONFIG = config_data.get("db", {})
        API_CONFIG = config_data.get("api", {})
        ES_CONFIG = config_data.get("elasticsearch", {})
    except yaml.YAMLError as exc:
        raise ValueError(f"config.yaml 파일의 내용을 읽을 수 없음: {exc}")
