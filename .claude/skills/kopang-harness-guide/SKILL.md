---
name: kopang-harness-guide
description: >
  코팡 프로젝트의 아키텍처 결정, 코딩 컨벤션, 예외 처리, 테스트 가이드라인을
  제공합니다. domain/, service/, storage/, infra/, api/, scheduler/ 디렉토리의
  Java 파일을 생성하거나 편집할 때, plan.md를 작성할 때, 코드 리뷰를 할 때
  자동으로 참조합니다.
user-invocable: false
---

## 참조 파일

- [ARCHITECTURE.md](references/ARCHITECTURE.md) — 레이어 의존성, Domain/Storage/Service/Orchestrator 규칙
- [ERROR_HANDLING.md](references/ERROR_HANDLING.md) — 예외 계층, HTTP 매핑, 보상 패턴
- [TEST_GUIDE.md](references/TEST_GUIDE.md) — 테스트 분류, 작성 기준, 구조 규칙

## 문서 선택 기준

| 작업 상황 | 읽어야 할 문서 |
|:---|:---|
| 새 클래스/인터페이스 생성 | ARCHITECTURE.md |
| 예외 클래스 추가 | ERROR_HANDLING.md |
| 테스트 코드 작성 | TEST_GUIDE.md |
| plan.md 작성 | 3개 문서 전부 + `.claude/templates/plan-template.md` |
| 코드 리뷰 (/review) | 3개 문서 전부 |

## 핵심 원칙 요약

### Architecture
- Domain POJO ≠ JPA Entity. 완전 분리.
- 외부 의존성 인터페이스는 반드시 `domain` 패키지에. (DIP)
- DB(JPA) 작업 Service = `@Transactional` 필수. Redis 전용 Service = `@Transactional` 없음 (`StockReservationService` 패턴).
- Orchestrator = 흐름 조율 + `@Transactional` 금지.
- Domain 객체는 불변. 상태 변경은 새 인스턴스 반환. Static Factory Method 필수.

### Error Handling
- Domain → `IllegalStateException` / `IllegalArgumentException`
- Service → Custom Exception (static factory)
- 새 예외 추가 시 `GlobalExceptionController`에 `@ExceptionHandler` 핸들러 필수 등록.
- 보상 후 예외는 반드시 `throw e`로 재전파하세요.

### Testing
- FIRST + 가치 기반 선별 작성.
- 도메인 정책 테스트: 단위 테스트, 실제 객체, 경계값 중심.
- 유스케이스 테스트: `@AcceptanceTest`, RestAssured, 블랙박스, H2 + JSON fixture.
- 메서드명: `테스트대상_상태_기대결과`. `@DisplayName`: 완전한 한글 비즈니스 명세 문장.