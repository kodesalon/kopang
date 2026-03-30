# Testing Rules

## 테스트 원칙
- **FIRST:** Fast / Independent / Repeatable / Self-validating / Timely
- **가치 기반 선별:** 핵심 비즈니스 로직·복잡한 도메인 규칙을 우선 작성하세요. 단순 위임 코드는 생략하세요.

## 도메인 정책 테스트 (단위 테스트)
- Spring 컨텍스트 없이 순수 Java로 작성하세요. 실제 도메인 객체를 사용하세요.
- 경계값(boundary value) 중심으로 작성하세요.

## 유스케이스 인수 테스트 (@AcceptanceTest)
- `@AcceptanceTest` + RestAssured로 HTTP → DB 전 계층을 관통하세요.
- 요청/응답은 `Map` 타입을 사용하세요. (내부 구현과 분리된 블랙박스 테스트)
- 포트는 `AcceptanceTestExecutionListener`가 자동 설정합니다. `RestAssured.port` 설정을 생략하세요.
- JSON fixture: `src/test/resources/acceptance/` 하위. `"now()"` → SQL now(). `null` → NULL.
- 비동기 대기가 필요한 경우 테스트 구조를 동기적으로 설계하세요.

## 구조 규칙
- **메서드명:** `테스트대상_상태_기대결과` (예: `pay_PaymentInProgress_ReturnsPaidOrder`)
- **@DisplayName:** 완전한 한글 비즈니스 명세 문장 ("~할 때, ~하면, ~된다" 형태)
- **@Nested:** Describe(메서드/기능) → Context(조건) → It(결과) 계층
- **본문:** `// given / // when / // then` 주석으로 3단계 분리

## Fixture 규칙 (`src/test/java/.../fixtures/`)
- 상태 전이 순서를 준수하세요:
  ```java
  // GOOD
  public static Order paidOrder() { return paymentInProgressOrder().pay(); }
  // BAD — 클래스 로딩 시 예외
  public static final Order PAID_ORDER = PENDING_ORDER.pay();
  ```

## 테스트 대역 기준
| 상황 | 대역 |
|:---|:---|
| 외부 서비스 (제어 불가) | Fake (실제처럼 동작하는 구현체) |
| 비즈니스 외 부수효과 (알림 등) | Dummy (아무것도 안 하는 구현체) |
| 데이터 확보 어려운 내부 서비스 | Stub (Mockito `given(...).willReturn(...)`) |
| 그 외 | 실제 객체 |