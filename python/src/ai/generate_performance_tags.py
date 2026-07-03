"""
performance_tags 자동 채우기 배치 스크립트
- GENRE 태그: title의 genre 컬럼으로 룰 기반 1:1 매핑 (임베딩 불필요)
- VIBE / WITH 태그: 공연 텍스트(title+castings+extractedText)와
  태그 설명 문장 간 코사인 유사도로 aiScore 산출
실행 환경: GTX 1660 (VRAM 6GB) 기준, CUDA + sentence-transformers 사용
"""
from collections import defaultdict

import psycopg2
import psycopg2.extras
import torch
from sentence_transformers import SentenceTransformer

from config.config import DB_CONFIG

DB_CONN_CONFIG = dict(
    host=DB_CONFIG["host"],
    port=DB_CONFIG["port"],
    dbname=DB_CONFIG["database"],
    user=DB_CONFIG["username"],
    password=DB_CONFIG["password"]
)
MODEL_NAME = "jhgan/ko-sroberta-multitask" # 한국어 특화 모델
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
BATCH_SIZE = 16 # 6GB VRAM 기준 안전한 배치 크기
TEXT_MAX_LEN = 600 # extractedText가 과도하게 긴 경우 자르기
VIBE_WITH_THRESHOLD = 0.3 # 이 점수 미만이면 해당 태그는 저장하지 않음 (노이즈 제거)

# ── 1. GENRE 룰 기반 매핑 ─────────────────────────────────
# performances.genre 컬럼 값 -> tags.tag_id
GENRE_TAG_MAP = {
    "뮤지컬": 101,
    "콘서트": 102,
    "클래식": 103,
}

# ── 2. VIBE / WITH 태그 설명 문장 (태그당 여러 문장) ──────
# 한 문장만 쓰면 특정 표현에 치우쳐 유사도가 불안정해지므로,
# 태그당 3~5개의 다른 표현을 준비하고 평균(mean pooling)해서 대표 벡터로 사용
TAG_DESCRIPTIONS = {
    201: [
        "관객을 눈물짓게 만드는 슬프고 감동적인 서사의 공연",
        "이별, 그리움, 상실을 다루며 마음 깊이 울림을 주는 공연",
        "보고 나면 한참을 울게 되는 신파적이고 감동적인 작품",
        "가슴 뭉클한 명대사와 음악으로 눈물샘을 자극하는 공연",
        "삶과 죽음, 가족애를 다루며 깊은 여운을 남기는 무대",
    ],
    202: [
        "귀가 즐거운, 음악과 가창력이 뛰어난 공연",
        "라이브 보컬과 연주 실력이 돋보이는 음악 중심 공연",
        "넘버(노래)가 아름답고 듣는 즐거움이 큰 작품",
        "성악, 라이브밴드 등 사운드 퀄리티가 뛰어난 무대",
        "음악적 완성도가 높아 음원으로도 소장하고 싶은 공연",
    ],
    203: [
        "스케일이 크고 웅장한 연출과 무대를 가진 공연",
        "대규모 오케스트라, 합창, 무대 장치로 압도적인 스케일을 보여주는 공연",
        "역사적, 서사시적 배경을 다루는 대형 작품",
        "웅장한 사운드와 화려한 군무로 시각·청각을 압도하는 무대",
    ],
    204: [
        "신나고 흥겨워서 스트레스가 풀리는 공연",
        "관객과 함께 떼창하고 즐기는 신나는 라이브",
        "댄스, 밴드 사운드 등 에너지 넘치고 흥겨운 분위기의 공연",
        "보고 나면 기분이 좋아지는 경쾌하고 활기찬 작품",
    ],
    205: [
        "긴장감 넘치고 손에 땀을 쥐게 하는 스릴 있는 공연",
        "범죄, 미스터리, 추격을 다루는 긴장감 있는 서사의 작품",
        "반전이 있고 몰입도 높은 스릴러 성격의 공연",
        "심장이 쫄깃해지는 서스펜스 넘치는 무대",
    ],
    206: [
        "웃기고 유쾌해서 배꼽 빠지게 웃을 수 있는 공연",
        "코미디 요소가 강하고 관객을 자주 웃기는 작품",
        "위트 있는 대사와 슬랩스틱으로 즐거움을 주는 공연",
        "가볍고 유쾌하게 웃으며 볼 수 있는 코믹한 무대",
    ],
    207: [
        "화려한 무대 연출과 볼거리가 많은 공연",
        "화려한 의상, 조명, 영상 연출이 돋보이는 작품",
        "시각적 스펙터클이 강조된 화려한 퍼포먼스",
        "무대 전환과 특수효과 등 볼거리가 풍부한 공연",
    ],
    208: [
        "잔잔하고 차분하게 위로받을 수 있는 공연",
        "조용한 어쿠스틱 사운드로 마음을 편안하게 해주는 공연",
        "일상의 위로와 휴식을 주는 잔잔한 분위기의 작품",
        "과하지 않고 담백하게 감성을 자극하는 차분한 무대",
    ],
    209: [
        "처음 공연을 접하는 입문자도 편하게 즐기기 좋은 공연",
        "내용이 어렵지 않고 친숙해서 누구나 즐길 수 있는 공연",
        "유명하고 대중적이라 처음 보기에도 부담 없는 작품",
        "스토리가 직관적이어서 공연 입문용으로 좋은 무대",
    ],
    301: [
        "혼자 가서 조용히 몰입하기 좋은 공연",
        "개인적인 사색과 몰입에 적합한 차분한 작품",
        "혼자 관람해도 충분히 만족스러운 깊이 있는 공연",
    ],
    302: [
        "연인과 함께 보기 좋은 로맨틱한 공연",
        "사랑, 설렘을 다루는 로맨스 중심의 작품",
        "데이트 코스로 인기 있는 분위기 좋은 공연",
    ],
    303: [
        "부모님을 모시고 가기 좋은 공연",
        "트로트, 추억의 명곡 등 중장년층이 좋아할 만한 공연",
        "효도 공연으로 인기 있는 편안하고 친숙한 무대",
    ],
    304: [
        "친구들과 함께 즐기기 좋은 공연",
        "단체로 관람하며 신나게 즐길 수 있는 공연",
        "친구들과의 추억 만들기에 좋은 활기찬 무대",
    ],
    305: [
        "아이와 함께 보기 좋은 가족 친화적인 공연",
        "어린이 눈높이에 맞춘 동화, 캐릭터 기반의 공연",
        "온 가족이 함께 즐길 수 있는 건전하고 밝은 작품",
    ],
}
VIBE_WITH_TAG_IDS = list(TAG_DESCRIPTIONS.keys())

def build_text(row) -> str:
    """공연 1건을 임베딩용 텍스트로 변환"""
    parts = [row["title"]]
    if row["castings"]:
        parts.append(row["castings"])
    if row["extracted_text"]:
        parts.append(row["extracted_text"][:TEXT_MAX_LEN])
    return " ".join(p for p in parts if p)

def fetch_performances(conn):
    sql = """
          SELECT performance_id, title, genre, castings, extracted_text
          FROM performances
          WHERE performance_id IN (401, 666, 1044, 1059);
          """
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(sql)
        return cur.fetchall()

def upsert_performance_tags(conn, rows):
    """
    (performance_id, tag_id) 기준 UPSERT.
    perf_tag_id는 IDENTITY라 ON CONFLICT 대상 유니크 제약이 필요합니다.
    아래 제약이 없다면 먼저 추가해주세요:
        ALTER TABLE performance_tags
        ADD CONSTRAINT uq_perf_tag UNIQUE (performance_id, tag_id);
    """
    sql = """
          INSERT INTO performance_tags (performance_id, tag_id, ai_score)
          VALUES (%s, %s, %s)
          ON CONFLICT (performance_id, tag_id)
              DO UPDATE SET ai_score = EXCLUDED.ai_score;
          """
    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(cur, sql, rows, page_size=500)
    conn.commit()

def main():
    print(f"device = {DEVICE}")
    model = SentenceTransformer(MODEL_NAME, device=DEVICE)

    conn = psycopg2.connect(**DB_CONN_CONFIG)
    performances = fetch_performances(conn)
    print(f"로드된 공연 수: {len(performances)}건")

    # ── GENRE 태그: 즉시 처리 (임베딩 불필요) ──
    genre_rows = []
    for row in performances:
        tag_id = GENRE_TAG_MAP.get(row["genre"])
        if tag_id:
            genre_rows.append((row["performance_id"], tag_id, 1.0))
    print(f"GENRE 태그 {len(genre_rows)}건 매핑 완료")

    # ── VIBE/WITH 태그: 임베딩 유사도 ──
    perf_texts = [build_text(row) for row in performances]
    print("태그 설명 임베딩 계산 중 (태그당 여러 문장 평균)...")
    tag_embeddings = []
    for tag_id in VIBE_WITH_TAG_IDS:
        sentences = TAG_DESCRIPTIONS[tag_id]
        sent_emb = model.encode(
            sentences, convert_to_tensor=True, device=DEVICE, normalize_embeddings=True
        )
        tag_embeddings.append(sent_emb.mean(dim=0))
    tag_embeddings = torch.stack(tag_embeddings)
    tag_embeddings = torch.nn.functional.normalize(tag_embeddings, dim=1)  # 평균 후 재정규화

    print("공연 텍스트 임베딩 계산 중 (배치 처리)...")
    perf_embeddings = model.encode(
        perf_texts,
        batch_size=BATCH_SIZE,
        convert_to_tensor=True,
        device=DEVICE,
        normalize_embeddings=True,
        show_progress_bar=True,
    )

    # 코사인 유사도 = 정규화된 벡터의 내적
    sim_matrix = perf_embeddings @ tag_embeddings.T  # (N_perf, N_tag)

    vibe_with_rows = []
    for i, row in enumerate(performances):
        for j, tag_id in enumerate(VIBE_WITH_TAG_IDS):
            score = float(sim_matrix[i, j])
            if score >= VIBE_WITH_THRESHOLD:
                vibe_with_rows.append((row["performance_id"], tag_id, round(score, 4)))

    print(f"VIBE/WITH 태그 {len(vibe_with_rows)}건 산출 완료 (threshold={VIBE_WITH_THRESHOLD})")

    # 해당하는 태그가 많을 경우 분위기, 동행 태그에서 각 점수가 가장 높은 상위 3개 태그만 남김
    print(f"태그를 3개 초과하여 가진 항목 태그 정리")
    tags_grouped = defaultdict(lambda: {"200s": [], "300s": []})

    for pid, tid, score in vibe_with_rows:
        if 200 <= tid < 300:
            tags_grouped[pid]["200s"].append((pid, tid, score))
        elif 300 <= tid < 400:
            tags_grouped[pid]["300s"].append((pid, tid, score))

    tags_grouped_result = []
    for pid, categories in tags_grouped.items():
        top_200s = sorted(categories["200s"], key=lambda x: x[2], reverse=True)[:3]
        top_300s = sorted(categories["300s"], key=lambda x: x[2], reverse=True)[:3]

        tags_grouped_result.extend(top_200s)
        tags_grouped_result.extend(top_300s)

    print(f"정리 후 태그 수: {len(tags_grouped_result)}건")

    all_rows = genre_rows + tags_grouped_result
    print("DB에 upsert...")
    upsert_performance_tags(conn, all_rows)
    print(f"완료: {len(all_rows)}건 저장")
    
    conn.close()

def model_test():
    """모델 정상 동작 테스트"""
    sentences = ["안녕하세요?", "한국어 문장 임베딩을 위한 버트 모델입니다."]

    model = SentenceTransformer('jhgan/ko-sroberta-multitask')
    embeddings = model.encode(sentences)
    print(embeddings)

if __name__ == "__main__":
    main()