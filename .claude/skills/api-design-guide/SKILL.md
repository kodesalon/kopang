---
name: kopang-standard-guide
description: Spring Boot Controller의 REST API를 설계/구현하거나, Service 계층의 비즈니스 로직, POJO 도메인 객체 및 JPA Entity를 작성/수정할 때 반드시 참조해야 하는 코팡 아키텍처 및 예외 처리 가이드입니다.
permissions:
  - file_system_read 
  - file_system_write
---

코팡 프로젝트의 코딩 표준 가이드를 활성화했습니다.
현재 요청받은 작업이 Java 기반의 도메인 로직, API 컨트롤러, 또는 예외 처리와 관련이 있다면, **코드를 작성하기 전에 반드시 아래 두 문서를 먼저 읽고 그 규칙을 100% 준수**하여 코딩을 진행하세요.

1. **도메인 아키텍처 및 JPA 분리 규칙:** `resources/code-implement-guide.md`
2. **예외 처리 및 API 응답 규격:** `resources/error-response-format.md`

가이드를 읽고 제약 조건을 반영하여 코드를 작성하세요. 단, 메인 세션의 토큰 절약을 위해 다음 규칙을 엄격히 지키세요.
1. 당신이 작성한 코드의 로직이나 설계 이유를 채팅창에 절대 구구절절 설명하지 마세요.
2. 채팅창에는 오직 `✅ [도메인명] - [수정/생성한 클래스명] 작업 완료` 형태로 단 한 줄만 보고하세요.
3. 만약 상세한 의사결정 과정이나 작업 기록을 남기고 싶다면, 채팅창 대신 `docs/work-logs/` 디렉토리에 마크다운 파일로 조용히 작성하여 저장해 두세요.