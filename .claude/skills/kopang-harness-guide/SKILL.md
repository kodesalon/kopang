# kopang-harness-guide

## Trigger
Order/Payment 도메인 코드를 작성·수정·리뷰할 때 반드시 이 가이드를 로드한다.
주문 예약, 결제, 재고, 배치 스케줄러 관련 작업 포함.

## 로드 순서
다음 파일을 순서대로 읽고 모든 규칙을 컨텍스트에 반영한다:
1. `ARCHITECTURE.md` — 아키텍처 철학, 레이어 의존성, Domain/Storage/Service 규칙
2. `ERROR_HANDLING.md` — 예외 계층, HTTP 매핑, 보상 패턴
3. `TESTING.md` — 테스트 분류, 작성 기준, 구조 규칙

## 핵심 원칙 요약

**Architecture:**
- Domain POJO ≠ JPA Entity. 완전 분리.
- 외부 의존성 인터페이스는 반드시 `domain` 패키지에. (DIP)
- Service = 원자적 단위 + @Transactional. Orchestrator = 흐름 조율 + @Transactional 금지.
- Domain 객체는 불변. 상태 변경은 새 인스턴스 반환.

**Error Handling:**
- Domain → IllegalStateException / IllegalArgumentException
- Service → Custom Exception (static factory)
- 새 예외 추가 시 GlobalExceptionController에 핸들러 필수 등록.
- 보상 후 예외는 반드시 재전파.

**Testing:**
- FIRST + Complete + Concise. 가치 기반 선별 작성.
- 도메인 정책 테스트: 단위 테스트, 실제 객체, 경계값 중심.
- 유스케이스 테스트: @AcceptanceTest, RestAssured, 블랙박스, H2 + JSON fixture.
- 메서드명: `테스트대상_상태_기대결과`. @DisplayName: 완전한 한글 비즈니스 명세 문장.