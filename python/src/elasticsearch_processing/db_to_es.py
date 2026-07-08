import psycopg2
from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk

#from config.config import DB_CONFIG, ES_CONFIG

# 엘라스틱서치 클라이언트 연결
#es_url = f"http://{ES_CONFIG["host"]}:{ES_CONFIG["port"]}"
es_url = f"http://localhost:9200"
es = Elasticsearch(es_url)

# Postgres 연결
pg_conn = psycopg2.connect(
    # host=DB_CONFIG["host"],
    # database=DB_CONFIG["database"],
    # user=DB_CONFIG["username"],
    # password=DB_CONFIG["password"],
    # port=DB_CONFIG["port"]
    host="localhost",
    port="5432",
    dbname="clearticket",
    user="postgres",
    password="tjee1234"
)
pg_cursor = pg_conn.cursor()

INDEX_NAME_PERFORMANCES = "performances"
INDEX_NAME_VENUES = "venues"

def create_es_index_with_nori():
    """Nori 분석기를 적용한 Elasticsearch 인덱스 생성"""
    print("인덱스 생성...")
    # 이미 인덱스가 있다면 초기화
    if es.indices.exists(index=INDEX_NAME_PERFORMANCES):
        es.indices.delete(index=INDEX_NAME_PERFORMANCES)
        print(f"  {INDEX_NAME_PERFORMANCES} 인덱스 삭제")

    if es.indices.exists(index=INDEX_NAME_VENUES):
        es.indices.delete(index=INDEX_NAME_VENUES)
        print(f"  {INDEX_NAME_VENUES} 인덱스 삭제")

    # Nori Tokenizer 세팅
    index_performances_config = {
        "settings": {  # 설정
            "analysis": {
                "analyzer": {
                    "nori_korean_analyzer": {
                        "type": "custom",
                        "tokenizer": "nori_tokenizer",
                        "filter": ["lowercase"]
                    }
                }
            }
        },
        "mappings": {  # DB 매핑
            "properties": {
                "performance_id": {"type": "long"},
                "title": {
                    "type": "text",
                    "analyzer": "nori_korean_analyzer"  # 제목 검색에 nori 적용
                },
                "genre": {"type": "keyword"},  # 장르, 지역, 공연 상태는 검색 시 키워드로 매핑
                "region": {"type": "keyword"},
                "status": {"type": "keyword"},
                "start_date": {"type": "date"},
                "end_date": {"type": "date"},
                "castings": {
                    "type": "text",
                    "analyzer": "nori_korean_analyzer"
                },
                "poster_url": {
                    "type": "keyword",
                    "index": False
                },
                "extracted_text": {
                    "type": "text",
                    "analyzer": "nori_korean_analyzer"  # 상세 설명 nori 적용
                },
                "tagsVibe": {"type": "keyword"},
                "tagsWith": {"type": "keyword"}
            }
        }
    }

    index_venues_config = {
        "settings": {  # 설정
            "analysis": {
                "analyzer": {
                    "nori_korean_analyzer": {
                        "type": "custom",
                        "tokenizer": "nori_tokenizer",
                        "filter": ["lowercase"]
                    }
                }
            }
        },
        "mappings": {
            "properties": {
                "venue_id": {"type": "long"},
                "name": {
                    "type": "text",
                    "analyzer": "nori_korean_analyzer"
                },
                "address": {"type": "text"},
                "region": {"type": "keyword"},
                "location": {"type": "geo_point"},
                "telnum": {"type": "keyword"},
                "relateurl": {
                    "type": "keyword",
                    "index": False
                },
                "capacity": {"type": "integer"}
            }
        }
    }

    es.indices.create(index=INDEX_NAME_PERFORMANCES, body=index_performances_config)
    print(f"  {INDEX_NAME_PERFORMANCES} 인덱스 생성 완료")

    es.indices.create(index=INDEX_NAME_VENUES, body=index_venues_config)
    print(f"  {INDEX_NAME_VENUES} 인덱스 생성 완료")

def migrate_data_to_es():
    """DB 데이터 -> ES"""
    print("DB 데이터 -> ES")

    print("  DB 데이터 조회...")
    query_performances = "SELECT performance_id, title, start_date, end_date, genre, region, status, castings, poster_url, extracted_text FROM performances"
    pg_cursor.execute(query_performances)
    rows_performances = pg_cursor.fetchall()

    query_tags = "SELECT performance_id, tag_id FROM performance_tags"
    pg_cursor.execute(query_tags)
    rows_tags = pg_cursor.fetchall()

    tags_vibe_dict = {}
    tags_with_dict = {}
    for row in rows_tags:
        pid, tid = row[0], row[1]
        if 200 < int(tid) < 300:
            if pid not in tags_vibe_dict:
                tags_vibe_dict[pid] = []
            tags_vibe_dict[pid].append(tid)
        elif 300 < int(tid):
            if pid not in tags_with_dict:
                tags_with_dict[pid] = []
            tags_with_dict[pid].append(tid)

    query_venues = "SELECT venue_id, name, region, telnum, capacity, address, relateurl, lat, lon FROM venues"
    pg_cursor.execute(query_venues)
    rows_venues = pg_cursor.fetchall()

    print(f"  DB 데이터 조회 건수")
    print(f"    performances: {len(rows_performances)}건")
    print(f"    venues: {len(rows_venues)}건")
    print(f"  ES 적재 시작")

    # ES 벌크 api 규격에 맞춰 제너레이터 형태로 변환
    def generate_actions_performances():
        for row in rows_performances:
            performance_id = row[0]
            performance_tags_vibe = tags_vibe_dict.get(performance_id, [])
            performance_tags_with = tags_with_dict.get(performance_id, [])

            yield {
                "_index": INDEX_NAME_PERFORMANCES,
                "_id": performance_id,
                "_source": {
                    "performance_id": performance_id,
                    "title": row[1],
                    "start_date": row[2],
                    "end_date": row[3],
                    "genre": row[4],
                    "region": row[5],
                    "status": row[6],
                    "castings": row[7],
                    "poster_url": row[8],
                    "extracted_text": row[9],
                    "tags_vibe": performance_tags_vibe,
                    "tags_with": performance_tags_with
                }
            }

    def generate_actions_venues():
        for row in rows_venues:
            yield {
                "_index": INDEX_NAME_VENUES,
                "_id": row[0],
                "_source": {
                    "venue_id": row[0],
                    "name": row[1],
                    "region": row[2],
                    "telnum": row[3],
                    "capacity": row[4],
                    "address": row[5],
                    "relateurl": row[6],
                    "lat": row[7],
                    "lng": row[8]
                }
            }

    # elasticsearch의 helpers.bulk를 이용해 데이터를 묶어서 적재
    success_p, errors_p = bulk(es, generate_actions_performances())
    print(f"  performances | 적재 성공: {success_p}건 / 실패 {len(errors_p) if isinstance(errors_p, list) else errors_p}건")
    success_v, errors_v = bulk(es, generate_actions_venues())
    print(f"  venues | 적재 성공: {success_v}건 / 실패 {len(errors_v) if isinstance(errors_v, list) else errors_v}건")

if __name__ == "__main__":
    try:
        create_es_index_with_nori()
        migrate_data_to_es()
    finally:
        pg_cursor.close()
        pg_conn.close()
