# 구현 계획서 — #{이슈번호} {이슈 요약}

> 설계 근거·아키텍처 토론: `.workspace/plan-design.md` 에 별도 작성

---

## 1. 선행 컨텍스트

| 문서    | 경로                                                                 | 숙지 포인트                                                         |
|:------|:-------------------------------------------------------------------|:---------------------------------------------------------------|
| 아키텍처  | `.claude/skills/kopang-harness-guide/references/ARCHITECTURE.md`   | {관련 레이어 규칙 — 예: Orchestrator 패턴, storage 변환, infra vs storage} |
| 예외 처리 | `.claude/skills/kopang-harness-guide/references/ERROR_HANDLING.md` | {새 예외 추가 여부, 보상 패턴 적용 여부}                                      |
| 테스트   | `.claude/skills/kopang-harness-guide/references/TEST_GUIDE.md`     | {이 기능에 해당하는 테스트 유형}                                            |
| 참조 코드 | `{경로}`                                                             | {참조할 패턴}                                                       |

---

## 2. 완료 조건 (Acceptance Criteria)

- [ ] {조건}
- [ ] {조건}

---

## 3. 설계 *(해당 시)*

### 데이터 구조 *(Redis 키 / DB 스키마 등 신규 구조가 있는 경우)*

| 키/테이블 | 타입 | TTL | 용도 |
|:------|:---|:----|:---|
|       |    |     |    |

### 전체 흐름

```
[1] 요청 → 처리 → 응답
[2] ...
```

**의존성 방향:** {레이어 간 의존 방향}

---

## 4. 구현 원칙

**공통:**

- domain interface(Port/Repository)를 통해서만 주입받으세요.
- Orchestrator/Scheduler는 `@Transactional` 없이 `@Component`를 사용하세요.
- 비즈니스 로직을 담당하는 Service의 모든 public 메서드에 `@Transactional`을 사용하세요. Redis 전용 Service는 제외 (`StockReservationService` 패턴).
- `@Scheduled`는 `fixedDelay` 속성을 사용하세요.

**이번 작업:**

- {이번 작업 특정 원칙}

---

## 5. 엣지 케이스

| # | 엣지 케이스 | 방어 전략 | 적용 위치 |
|:--|:-------|:------|:------|
| 1 |        |       |       |

---

## 6. 구현 순서

의존성이 적은 방향(domain → infra → service → api)으로 순차 진행. 각 Step은 독립적으로 컴파일·검증 가능.

---

### Step 1 — {작업 이름}

**패키지:** `{경로}`. {레이어 특성 한 줄}

- `{ClassName}`: `{method()}`. {역할}.

**수정:** `{파일}` — {변경 내용}

**검증:** `./gradlew compileJava`

---

### Step 2 — {작업 이름}

{위와 동일 형식}

---

### Step N — 테스트

아래 표에서 이 기능에 **해당하는 유형만** Y로 표시하고 구현하세요.

| 유형     | 해당  | 대상      | 핵심 케이스                   |
|:-------|:---:|:--------|:-------------------------|
| 도메인 단위 | Y/N | {클래스명}  | {상태전이 / 경계값 / 비즈니스 규칙 등} |
| 인수     | Y/N | {엔드포인트} | {정상 흐름, 오류 시나리오}         |
| 동시성    | Y/N | {시나리오}  | {정합성 조건}                 |

**공통 규칙:** `@Nested` 계층 구조, 메서드명 `테스트대상_상태_기대결과` (영문), Fixture는 `static final` 금지·메서드 체인 사용

**검증:** `./gradlew test --tests "{TestClass}"`

---

## 7. 검증 체크리스트

**구조:**

- [ ] Service가 Port interface만 주입받는가?
- [ ] `domain/` 패키지에 Spring/JPA 어노테이션이 없는가?
- [ ] Orchestrator/Scheduler에 `@Transactional`이 없는가?
- [ ] 섹션 4(구현 원칙)를 모두 준수하는가?

**비즈니스:**

- [ ] 섹션 5(엣지 케이스)의 방어 전략이 코드에 반영되었는가?
- [ ] 새 예외가 `GlobalExceptionController`에 등록되었는가?

**테스트:**

- [ ] Step N에서 Y로 표시한 테스트 유형이 모두 작성되었는가?
- [ ] 도메인 단위: 식별한 케이스(상태전이·경계값 등)가 누락 없이 포함되었는가? *(해당 시)*
- [ ] 인수: `@AcceptanceTest` + Map 타입을 사용했는가? *(해당 시)*
- [ ] 동시성: `CountDownLatch` + `ExecutorService` 패턴을 사용했는가? *(해당 시)*
- [ ] Fixture 상태 전이 순서가 올바른가?