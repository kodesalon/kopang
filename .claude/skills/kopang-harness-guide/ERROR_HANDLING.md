# Error Handling Rules

## 1. 예외 발생 위치 원칙

예외는 발생한 **원인에 가장 가까운 레이어**에서 던진다.
상위 레이어는 예외를 변환하거나 전파할 책임을 가진다.

| 레이어 | 던지는 예외 | 의미 |
|--------|------------|------|
| domain | `IllegalStateException` | 비즈니스 규칙 위반 (허용되지 않는 상태 전이 등) |
| domain | `IllegalArgumentException` | 유효하지 않은 값 (음수, null 등) |
| service | Custom Exception (static factory) | 엔티티 없음, 품절, 결제 실패 등 비즈니스 오류 |
| orchestrator | 예외 재전파 | 보상 처리 후 상위로 전파 |
| api | GlobalExceptionController | HTTP 상태 코드로 변환 |

---

## 2. Domain 예외 패턴

Domain 객체는 Spring에 의존하지 않으므로 Java 표준 예외만 사용한다.

```java
// 상태 전이 불가 → IllegalStateException
public Order pay() {
    if (!status.isPaymentInProgress()) {
        throw new IllegalStateException("결제 진행 중인 주문만 승인할 수 있습니다.");
    }
    return new Order(..., OrderStatus.PAID, ...);
}

// 유효하지 않은 값 → IllegalArgumentException
public static StockQuantity from(Integer value) {
    if (value < MIN_QUANTITY) {
        throw new IllegalArgumentException("재고 수량은 0보다 작을 수 없습니다.");
    }
    return new StockQuantity(value);
}
```

---

## 3. Custom Exception 패턴

### Static Factory Method 필수
예외 메시지는 컨텍스트(ID 등)를 포함해야 하며, 직접 `new`로 생성하지 않는다.

```java
// GOOD — 팩토리 메서드로 메시지 표준화
public class NotFoundException extends RuntimeException {
    public static NotFoundException order(Long orderNo) {
        return new NotFoundException(
            String.format("주문 %s 를 찾을 수 없습니다", orderNo)
        );
    }
}

// 호출
throw NotFoundException.order(orderNo);

// BAD — 메시지 직접 생성
throw new NotFoundException("주문 123 를 찾을 수 없습니다"); // 표준화 불가
```

### 새 예외 클래스 추가 시 체크리스트
1. `service/exception` 패키지에 클래스 생성
2. `RuntimeException` 상속
3. 컨텍스트 포함 static factory method 정의
4. **`GlobalExceptionController`에 `@ExceptionHandler` 핸들러 반드시 추가**

---

## 4. HTTP 매핑 (GlobalExceptionController)

모든 비즈니스 예외는 `GlobalExceptionController`에서 명시적으로 HTTP 상태로 변환해야 한다.
**catch-all(500)에 비즈니스 예외가 떨어지면 안 된다.**

```java
@RestControllerAdvice
public class GlobalExceptionController {

    // 비즈니스 규칙 위반 (도메인에서 발생)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<KopangExceptionResponse> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest()
            .body(new KopangExceptionResponse(e.getMessage(), 400));
    }

    // 리소스 없음
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<KopangExceptionResponse> notFound(RuntimeException e) {
        return ResponseEntity.status(404)
            .body(new KopangExceptionResponse(e.getMessage(), 404));
    }

    // 품절 (새로 추가 예시 — 현재 누락된 매핑)
    @ExceptionHandler(SoldOutException.class)
    public ResponseEntity<KopangExceptionResponse> soldOut(RuntimeException e) {
        return ResponseEntity.badRequest()
            .body(new KopangExceptionResponse(e.getMessage(), 400));
    }

    // 결제 실패 (새로 추가 예시 — 현재 누락된 매핑)
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<KopangExceptionResponse> paymentFailed(RuntimeException e) {
        return ResponseEntity.status(402)
            .body(new KopangExceptionResponse(e.getMessage(), 402));
    }

    // 예상치 못한 서버 오류 (비즈니스 예외는 여기 오면 안 됨)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<KopangExceptionResponse> internalServerError(Exception e) {
        return ResponseEntity.status(500)
            .body(new KopangExceptionResponse(e.getMessage(), 500));
    }
}
```

---

## 5. 보상(Compensation) 패턴

### 원칙
- 보상 처리 후 예외는 **반드시 재전파**한다. 삼키지 않는다.
- 보상이 실패해도 원래 예외가 전파되어야 한다.

```java
// GOOD — 보상 후 재전파
try {
    Order order = orderService.createOrderPending(...);
    return new ReservationOrderResult(stock, order);
} catch (Exception e) {
    stockReservationService.increase(...);  // 보상
    throw e;                                // 반드시 재전파
}

// BAD — 예외 삼킴 (원인 소실)
} catch (Exception e) {
    stockReservationService.increase(...);
    // throw 없음 → 호출자가 실패를 모름
}
```

### 예외 삼킴이 허용되는 유일한 문맥: Batch/Scheduler
스케줄러에서 개별 항목 처리 실패는 다음 사이클에 재시도되어야 하므로 허용된다.
단, **반드시 `log.warn` 이상으로 기록**해야 한다.

```java
// GOOD — 스케줄러 문맥에서만 허용
public void recover(Order order) {
    try {
        // 복구 로직
    } catch (Exception e) {
        log.warn("Reconcile order [{}] 실패. 다음 cycle 재시도. Error: {}",
            order.getNo(), e.getMessage());  // 로그 필수
        // 예외 삼킴 — 스케줄러 맥락에서만 허용
    }
}
```

---

## 6. 로깅 기준

| 상황 | 레벨 | 위치 |
|------|------|------|
| 알 수 없는 외부 상태 (PG 미지원 상태 등) | `log.error` | Orchestrator/Recovery |
| 재시도 예정인 실패 (배치, 스케줄러) | `log.warn` | Scheduler |
| 정상 비즈니스 예외 (404, 400 등) | 로그 없음 | GlobalExceptionController가 처리 |
| 예상치 못한 서버 오류 | `log.error` | GlobalExceptionController |