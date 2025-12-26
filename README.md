# Xynerzy Studio API Server

[Read this in English](./documents/README.en.md)

![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)
![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)
[![en](https://img.shields.io/badge/lang-en-red.svg)](./documents/README.en.md)
[![ko](https://img.shields.io/badge/lang-ko-blue.svg)](./README.md)

> 인간과 AI의 사고가 만나는 곳.

**Xynerzy Studio** (*Synergy* 의 애너그램 )는 인간과 여러 개의 대규모 언어 모델(LLM) 참여자가 함께하는 다자간 채팅 컨퍼런스를 호스팅하기 위한 오픈소스 플랫폼입니다. 이 프로젝트의 주요 목표는 협업 지능(Collaborative Intelligence) 을 위한 매끄러운 환경을 만드는 것으로, AI 에이전트가 인간 사용자와 함께 실시간으로 보조하고, 기여하며, 정보를 처리할 수 있도록 하는 것입니다.

## ✨ 핵심 기능 (Core Features)

- **멀티 에이전트 컨퍼런싱**: 여러 사용자와 다수의 AI 에이전트가 실시간으로 상호작용하는 채팅룸 생성
- **유연한 LLM 연동**:
  - 여러 API 키를 사용해 주요 클라우드 기반 LLM (예: Google Gemini, OpenAI GPT) 연결
  - 로컬 LLM 지원을 통해 완전 오프라인 또는 프라이빗 네트워크 환경에서도 운영 가능
- **정보 처리 기능**: 대화 중 AI 에이전트를 활용해 요약, 번역, 액션 아이템 추출, 데이터 분석 등의 작업을 즉시 수행
- **확장 가능한 아키텍처**: 대규모 배포를 위한 멀티 서버 샤딩 등, 미래 확장을 고려한 설계

## 🏛️ 아키텍처 (Architecture)

Xynerzy Studio는 두 개의 주요 컴포넌트로 구성됩니다:

1.  **`xynerzy-studio-api`**: 백엔드 서비스로 다음 역할을 담당합니다:
  - 사용자 세션 및 인증 관리.
  - WebSocket 기반 실시간 메시징 처리.
  - 사용자와 다양한 AI 에이전트 간 상호작용 오케스트레이션.
  - 대화 데이터 처리 및 영속화.
  - (기술 스택: Java / Rust).

2.  **`xynerzy-studio-web`**: 프론트엔드 웹 클라이언트로 다음 기능을 제공합니다:
  - 현대적이고 직관적인 채팅룸 UI.
  - AI 에이전트 및 설정 관리 도구.
  - 실시간 대화 렌더링.
  - (기술 스택: Vue.js / React)

## 🛠️ 기술 스택 (Technology Stack)
- 백엔드: Java (Spring Boot) / Rust (Actix/Rocket)
- 프론트엔드: Vue.js / React
- 실시간 통신: WebSockets
- 데이터베이스: PostgreSQL / MySQL (미정)

## 🚀 시작하기 (Getting Started)

*(이 섹션은 프로젝트가 성숙해짐에 따라 업데이트될 예정입니다.)*

### 사전 요구사항 (Prerequisites)
- Java JDK 17+ 또는 Rust 툴체인
- Node.js 18+
- Docker (선택 사항)

### 백엔드 (`xynerzy-studio-api`)

```bash
# Clone the repository
git clone https://github.com/xynerzy/xynerzy-studio-api.git
cd xynerzy-studio-api

# Build and run (example for Java/Gradle)
./gradlew bootRun
```

### Frontend (`xynerzy-studio-web`)

```bash
# Clone the repository
git clone https://github.com/xynerzy/xynerzy-studio-web.git
cd xynerzy-studio-web

# Install dependencies
npm install

# Start the development server
npm run dev
```

## 🗺️ 로드맵 (Roadmap)

- [x] 초기 프로젝트 설정 및 아키텍처 설계
- [ ] 핵심: 인간 사용자 간 실시간 채팅 구현
- [ ] 연동: 첫 번째 클라우드 LLM 연결 (예: Gemini)
- [ ] 핵심: 단일 채팅 내에서 다수의 교체 가능한 LLM 에이전트 지원
- [ ] 연동: 표준화된 API를 통한 로컬 LLM 지원
- [ ] 기능: 고급 정보 처리 명령어 (예: `/summarize`)
- [ ] 핵심: 사용자 인증 및 권한 관리
- [ ] **향후 계획**: 고가용성과 확장성을 위한 멀티 서버 샤딩 구현

## ✨ 기여자 (Contributors)

- 정재백 (@lupfeliz) — 코어개발자
<!-- ALL-CONTRIBUTORS-LIST:START -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

모든 분들의 기여를 환영합니다! 참여를 원하신다면 다음 방법을 이용해 주세요:
- 버그 제보 또는 신규 기능 제안을 위한 이슈 등록
- 저장소를 포크한 뒤 Pull Request 제출
- 문서 개선

## 📄 License

본 프로젝트는 **Apache License 2.0** 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참고하세요.
