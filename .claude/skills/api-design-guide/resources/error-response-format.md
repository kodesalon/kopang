# Kopang 예외 처리 및 에러 응답 가이드 (error-response-format.md)

이 문서는 Kopang 시스템에서 REST API를 개발할 때 반드시 준수해야 하는 예외 처리 및 에러 응답 규격입니다. Controller, Service, Domain 객체 구현 시 이 지침을 100% 따르세요.

## 1. Domain Exception vs Business Exception 구분

예외가 발생하는 계층과 책임에 따라 예외를 명확히 분리하여 던집니다.

### 1.1 Domain Exception (도메인 예외)

* **의미:** 도메인 객체(Entity) 내부의 불변식(Invariant)이 깨지거나, 유효하지 않은 상태/값이 들어올 때 발생합니다.
* **특징:** 외부 인프라(DB, PG사 등)나 복잡한 비즈니스 흐름과 무관하게 도메인 객체 스스로 던지는 예외입니다.
* **사용 예외:** 가급적 Java 표준 예외(`IllegalArgumentException`, `IllegalStateException`)를 사용하거나, 매우 특수한 경우 도메인 전용 예외를 정의합니다.
* **HTTP Status:** 주로 `400 Bad Request` 또는 `422 Unprocessable Entity`로 매핑됩니다.
* **예시:** 결제 금액이 0원 이하인 경우, 이미 취소된 주문 상태에서 변경을 시도하는 경우.

### 1.2 Business Exception (비즈니스 예외)

* **의미:** 도메인 규칙은 통과했으나, 비즈니스 흐름(Use Case)상 처리가 불가능한 상태일 때 발생합니다.
* **특징:** Service 계층에서 주로 발생하며, DB 검증이나 외부 API 연동 결과에 따라 발생합니다. **반드시 커스텀 예외로 정의합니다.**
* **HTTP Status:** 상황에 따라 `404 Not Found`, `409 Conflict`, `500 Internal Server Error` 등으로 세분화하여 매핑합니다.
* **예시:** `SoldOutException`(재고 소진), `PaymentFailedException`(PG사 결제 실패), `NotFoundException`(엔티티 조회 실패).

---

## 2. 커스텀 예외(Custom Exception) 설계 컨벤션

비즈니스 예외를 정의할 때는 다음 규칙을 엄격히 준수합니다.

1. **상속:** 반드시 `RuntimeException`을 상속합니다.
2. **정적 팩토리 메서드 활용:** `new CustomException(...)`을 직접 호출하지 말고, 상황을 명확히 설명하는 **정적 팩토리 메서드**를 사용하여 예외 생성자를 감쌉니다. 발생 맥락(Context)을 파악할 수 있는 식별자(`id`, `key` 등)를 메시지에 포함하세요.

**[예시: PaymentFailedException 구현]**

```java
public class PaymentFailedException extends RuntimeException {
	public PaymentFailedException(String message) {
		super(message);
	}

	// 상황 1: PG사 결제 승인 실패
	public static PaymentFailedException aborted(String paymentKey, Long orderNo, String message) {
		return new PaymentFailedException(
			String.format("결제에 실패했습니다. payment key : %s , orderNo : %d , failure message : %s", paymentKey, orderNo,
				message));
	}

	// 상황 2: 결제 유효시간 만료
	public static PaymentFailedException expired(String paymentKey, Long orderNo, String message) {
		return new PaymentFailedException(
			String.format("결제 유효시간이 만료되었습니다. payment key : %s , orderNo : %d , failure message : %s", paymentKey,
				orderNo, message));
	}
}
```

## 3. 에러 응답 포맷 및 GlobalExceptionController 매핑

클라이언트에게 반환되는 모든 에러 응답은 KopangExceptionResponse 레코드 규격을 따릅니다.

### 3.1 응답 DTO 포맷

```java
public record KopangExceptionResponse(
	String message,
	int code
) {
}
```

### 3.2 HTTP Status Code 매핑 가이드 (@RestControllerAdvice)

GlobalExceptionController에 예외를 핸들링할 때, 다음 기준에 따라 HTTP Status를 부여합니다.

* **400 Bad Request**: IllegalArgumentException, IllegalStateException 등 클라이언트의 잘못된 요청이나 도메인 검증 실패.
* **404 Not Found**: NotFoundException 등 요청한 자원(엔티티)이 DB에 존재하지 않을 때.
* **409 Conflict**: SoldOutException 등 시스템의 현재 상태(재고 소진, 동시성 충돌)와 클라이언트의 요청이 충돌할 때.
* **500 Internal Server Error**: Exception 등 핸들링되지 않은 서버 내부의 모든 치명적 에러.