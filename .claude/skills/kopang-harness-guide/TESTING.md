# Testing Rules

## 1. 좋은 테스트의 기준

### FIRST 원칙
- **Fast**: 빠르게 실행되어 자주 돌릴 수 있어야 한다.
- **Independent**: 테스트 간 순서·상태 의존 없이 독립적으로 실행 가능해야 한다.
- **Repeatable**: 어느 환경에서도 동일한 결과를 낸다.
- **Self-Validating**: 성공/실패를 자체적으로 판단한다. 사람이 결과를 해석할 필요 없음.
- **Timely**: 테스트 대상 코드 직전에 작성한다.

### Complete + Concise
- **Complete(완전)**: 테스트를 이해하는 데 필요한 모든 정보가 테스트 본문에 있다.
- **Concise(간결)**: 불필요한 정보는 포함하지 않는다. 복잡하거나 산만하지 않다.

### 가치 기반 선별 작성
- 모든 코드에 테스트를 작성하지 않는다. **작성 가치를 먼저 판단한다.**
- 핵심 비즈니스 로직, 복잡한 도메인 규칙, 주된 변경 지점 → 작성 가치 높음
- 단순 위임, 부수효과만 있는 코드, 수명이 짧은 이벤트성 기능 → 작성 가치 낮음
- **가치 있는 20%의 테스트로 80% 이상의 신뢰성을 목표로 한다.**

---

## 2. 테스트 분류

### [도메인 정책 테스트] — Domain 비즈니스 규칙 검증

**목적:** 특정 도메인 정책이 올바른지 빠르게 검증. 테스트 자체가 비즈니스 명세 문서가 된다.

**작성 방식:**
- **단위 테스트** — Spring 컨텍스트 없이 순수 Java로 작성
- **실제 객체(Real Collaborator)** — 가능한 한 Mock 없이 실제 도메인 객체 사용
- 모의 객체 프레임워크(Mockito 등) 사용 최소화
- **경계값(boundary value)** 중심으로 테스트

```java
class OrderTest {
    @Nested
    @DisplayName("preparePayment — 주문 결제 준비")
    class PreparePayment {

        @Test
        @DisplayName("결제 대기(PENDING) 상태의 주문에 올바른 금액으로 결제를 준비하면, 결제 진행 중(PAYMENT_IN_PROGRESS) 상태가 된다.")
        void preparePayment_PendingWithCorrectAmount_BecomesPaymentInProgress() {
            // given
            Order order = Order.createPending(1L, 1L, 1L, 1, BigDecimal.valueOf(1000));
            Money amount = new Money(1000L);

            // when
            Order prepared = order.preparePayment(amount, LocalDateTime.now());

            // then
            assertThat(prepared.getStatus()).isEqualTo(OrderStatus.PAYMENT_IN_PROGRESS);
        }

        @ParameterizedTest
        @MethodSource("alreadyFinalizedOrders")
        @DisplayName("이미 처리 완료된 주문(PAID, CANCELLED)에 결제를 준비하면, IllegalStateException이 발생한다.")
        void preparePayment_AlreadyFinalized_ThrowsIllegalStateException(Order order) {
            // when & then
            assertThatThrownBy(() -> order.preparePayment(new Money(1000L), LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
```

### [유스케이스 테스트] — 사용자 여정 검증 (인수 테스트)

**목적:** 고객 관점에서 소프트웨어가 인수 조건을 충족하는지 검증. 사용자 여정 문서화.

**작성 방식:**
- **인수 테스트(Acceptance Test)** = 통합 테스트. HTTP 요청부터 DB까지 전 계층 관통.
- `@AcceptanceTest` 커스텀 어노테이션으로 테스트 컨텍스트 추상화
- **블랙박스 테스트** — API 명세만 알고 내부 구현을 모르는 관점으로 작성
- DTO 클래스 대신 `Map` 사용 (내부 구현 변경에 테스트가 깨지지 않도록)
- RestAssured로 fluent한 HTTP 호출

```java
@AcceptanceTest({"acceptance/stock.json", "acceptance/product.json"})
class OrderControllerTest {

    @Test
    @DisplayName("선착순 재고가 있는 상품을 정상적으로 주문하면, 주문번호·총액·재고·주문상태를 반환한다.")
    void createReservationOrder_ValidProduct_ReturnsOrderInfo() {
        // given
        Map<String, Object> body = Map.of("productNo", 1L, "count", 1);

        // when
        Map<String, Object> response = RestAssured
            .given().log().all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("memberNo", 1L)
            .body(body)
            .when()
            .post("/api/v1/orders")
            .then().log().all()
            .statusCode(HttpStatus.CREATED.value())
            .extract().jsonPath().getMap(".");

        // then
        assertAll(
            () -> assertThat(response).containsKey("orderNo"),
            () -> assertThat(response).containsEntry("orderStatus", "PENDING"),
            () -> assertThat(new BigDecimal(response.get("totalPrice").toString()))
                .isEqualByComparingTo(BigDecimal.valueOf(10000))
        );
    }
}
```

---

## 3. 테스트 구조 규칙

### 메서드 네이밍: `테스트대상_상태_기대결과`

```java
// GOOD
void createOrder_SoldOut_ThrowsException()
void preparePayment_ExpiredOrder_ThrowsIllegalStateException()
void pay_PaymentInProgress_ReturnsPaidOrder()

// BAD
void testCreateOrder()        // 정보 없음
void createOrderTest()        // 기대 결과 없음
void 주문생성_성공()           // 영문 메서드명 규칙 불일치
```

### @DisplayName: 비즈니스 명세 문장

완전한 한글 문장. "~할 때, ~하면, ~하다/된다" 형태.

```java
// GOOD
@DisplayName("선착순 재고가 소진된 상태에서 주문을 시도하면, SoldOutException이 발생한다.")
@DisplayName("결제 대기 상태의 주문에 올바른 금액으로 결제를 준비하면, 결제 진행 중 상태가 된다.")

// BAD
@DisplayName("주문 테스트")              // 너무 추상적
@DisplayName("예외 발생")               // 결과만, 조건 없음
@DisplayName("create order success")   // 영문 + 불완전
```

### @Nested: Describe-Context-It (BDD 스타일)

복잡한 도메인 로직은 `@Nested`로 계층화한다.

```java
class OrderTest {
    // Describe: 어떤 메서드/기능을 테스트하는가?
    @Nested
    @DisplayName("pay — 주문 결제 승인")
    class Pay {

        // Context: 어떤 조건이 주어졌을 때인가?
        @Nested
        @DisplayName("결제 진행 중(PAYMENT_IN_PROGRESS) 상태일 때")
        class WhenPaymentInProgress {

            // It: 결과가 어떠해야 하는가?
            @Test
            @DisplayName("결제 승인을 호출하면, 주문 상태가 PAID가 된다.")
            void pay_PaymentInProgress_ReturnsPaidOrder() { ... }
        }

        @Nested
        @DisplayName("이미 결제 완료된(PAID) 상태일 때")
        class WhenAlreadyPaid {

            @Test
            @DisplayName("결제 승인을 호출하면, IllegalStateException이 발생한다.")
            void pay_AlreadyPaid_ThrowsIllegalStateException() { ... }
        }
    }
}
```

### Given / When / Then 주석

테스트 메서드 내부를 3단계로 명시적으로 분리한다.

```java
@Test
void someTest() {
    // given
    Order order = OrderFixture.pendingOrder();
    Money amount = new Money(1000L);

    // when
    Order result = order.preparePayment(amount, LocalDateTime.now());

    // then
    assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_IN_PROGRESS);
}
```

---

## 4. 테스트 대역 (Test Double) 사용 기준

### 도메인 정책 테스트
- **실제 객체 우선** — Mockito 사용 최소화
- 협력 객체(Collaborator)가 단순한 Value Object라면 직접 생성

### 유스케이스 테스트 (인수 테스트)
가능한 한 실제 환경에 가깝게. 대역은 불가피한 경우에만.

| 상황 | 대역 종류 | 예시 |
|------|----------|------|
| 외부 서비스 (제어 불가) | **Fake** — 실제와 유사하게 동작하는 구현체 | `MockPaymentClient` |
| 비즈니스 외적 부수효과 (알림 등) | **Dummy** — 아무것도 안 하는 구현체 | `MockKafkaMessageProducer` |
| 데이터 확보 어려운 내부 서비스 | **Stub** — Mockito `given(...).willReturn(...)` | 특정 캐시 응답 |
| 그 외 대부분 | **실제 객체** | Repository, Service |

---

## 5. test-fixtures 패키지

도메인 엔티티 생성 코드는 `src/test/java/.../fixtures` 패키지에 분리한다.
여러 테스트 클래스에서 재사용 가능하도록 한다.

```
src/test/java/
  └── com/kopang/
       └── fixtures/           # test-fixtures 패키지
            ├── OrderFixture.java
            ├── PaymentFixture.java
            └── StockFixture.java
```

**Fixture 작성 규칙:**
- 상태 전이 순서를 반드시 따른다. (잘못된 전이로 생성하면 클래스 로딩 실패)
- `Order.of(...)` 같은 테스트 편의 팩토리를 활용한다.

```java
// GOOD — 상태 전이 순서 준수
public class OrderFixture {
    public static Order pendingOrder() {
        return Order.of(1L, 1L, OrderStatus.PENDING, List.of(OrderProductFixture.product()), LocalDateTime.now());
    }

    public static Order paymentInProgressOrder() {
        return pendingOrder().preparePayment(new Money(1000L), LocalDateTime.now());
    }

    public static Order paidOrder() {
        return paymentInProgressOrder().pay();
    }
}

// BAD — 상태 전이 무시 (클래스 로딩 시 예외 발생)
public static final Order PAID_ORDER = PENDING_ORDER.pay(); // PENDING에서 pay() 호출 불가
```

---

## 6. 인수 테스트 인프라 사용 규칙

### @AcceptanceTest 어노테이션
```java
@AcceptanceTest({"acceptance/stock.json", "acceptance/product.json"})
class OrderControllerTest { ... }
```
- JSON 파일 경로는 `src/test/resources/acceptance/` 하위
- 파일에는 테스트에 필요한 최소 데이터만 정의

### JSON Fixture 포맷
```json
{
  "테이블명": [
    {"컬럼1": "값1", "created_at": "now()", "deleted_at": null}
  ]
}
```
- `"now()"` 문자열은 SQL `now()`로 처리됨
- `null`은 `NULL`로 처리됨
- beforeTestMethod: INSERT → afterTestMethod: TRUNCATE (완전 격리)

### 금지 패턴
```java
// BAD — 포트 하드코딩 (AcceptanceTestExecutionListener가 자동 설정)
RestAssured.port = 8080;

// BAD — DTO 클래스 사용 (내부 구현 의존 → 블랙박스 위반)
OrderRequest request = new OrderRequest(1L, 1);

// BAD — Thread.sleep (타이밍 의존 → Repeatable 위반)
Thread.sleep(100);
```