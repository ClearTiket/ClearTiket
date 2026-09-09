# ClearTiket

![AI](https://img.shields.io/badge/AI-3줄요약-4285F4?style=flat-square) ![팀프로젝트](https://img.shields.io/badge/팀프로젝트-6인-9C27B0?style=flat-square)

공연이 처음이든 익숙하든, AI가 공연 정보를 3줄로 요약해 한눈에 파악할 수 있는 
티켓 예매 서비스입니다.

## 🎯 프로젝트 목표
AI 기반 공연 정보 요약, 검색 교정, 맞춤 공연 추천, 실시간 좌석 동기화를 통해 
사용자 중심의 편리한 공연 예매 플랫폼을 구현하는 것을 목표로 했습니다.

## 📊 주요 성과

![성과](https://img.shields.io/badge/시간단축-약50%25-4CAF50?style=flat-square)

- **정보 습득 시간 약 50% 단축**: 기존 예매 사이트에서 공연 정보를 확인하는 데 
  평균 약 20초가 걸리는 반면, AI 3줄 요약을 활용하면 약 10초로 단축됨을 
  타이머 실측 비교 영상으로 검증

<img width="800" height="450" alt="--ezgif com-optimize" src="https://github.com/user-attachments/assets/12a181c3-dfe3-45f0-8821-cf8aa4820986" />

## 👥 팀원 구성 및 역할

| 이름 | 역할 | 담당 업무 & 핵심 구현 기능 |
|---|---|---|
| 김혜인 | FE / BE | AI 오타 교정 검색, 할인쿠폰 엔진 설계, Lemon Squeezy 결제 플랫폼 연동 및 정합성 검증 구현 |
| 김기운 | FE / BE | 데이터 수집 및 전처리, 공연장/공연 검색 기능, 사용자 맞춤 AI 공연 추천 기능 |
| **이상진 (본인)** | FE / BE | 공연 정보 통합 기능, 실시간 좌석 동기화, Gemini 기반 AI 3줄요약 |

## 💡 My Contributions

![핵심담당](https://img.shields.io/badge/핵심담당-AI%203줄요약-4285F4?style=flat-square)

**AI 3줄 요약**
- Java 백엔드에서 Naver CLOVA OCR API를 호출해 공연 포스터 이미지에서 텍스트 추출
- 추출된 텍스트를 Gemini API로 전달해 3줄 요약 생성
- **기술 선택 과정**: 초기에는 Python 머신러닝 활용을 고려했으나 Java 백엔드와의 호환성 문제로 
  강사님과 논의 후 방향 전환. OpenAI API를 우선 검토했지만 무료 토큰 미제공으로, 
  비용 효율성을 고려해 Gemini API를 최종 채택

**공연 정보 통합**
- KOPIS API로 수집한 공연 데이터를 메인 화면 → 상세 화면으로 연결하는 API Controller 구현

## 🔧 트러블슈팅

![문제](https://img.shields.io/badge/문제-FF6B6B?style=flat-square)  
OCR로 추출한 포스터 텍스트를 요약할 방법이 필요했음

![원인](https://img.shields.io/badge/원인-FFD93D?style=flat-square)  
Python 머신러닝 활용을 검토했으나 Java 백엔드와의 호환성 문제로 적용이 어려웠고, 이어서 검토한 OpenAI API는 무료 토큰을 제공하지 않아 비용 부담이 있었음

![해결](https://img.shields.io/badge/해결-4CAF50?style=flat-square)  
강사님과 논의 후 Java 백엔드에서 직접 호출 가능하고 무료 토큰을 제공하는 Gemini API로 전환하여 3줄 요약 기능을 구현함
