# Architecture Rules

## 1. 아키텍처 철학

**Pragmatic Clean Architecture + Lightweight DDD**

- Hexagonal의 port/adapter 패키지 강제, UseCase 레이어 의무 분리 같은 오버엔지니어링은 하지 않는다.
- 단, 아래 두 원칙은 **절대 타협하지 않는다:**
  1. **Domain POJO와 JPA Entity는 완전히 분리한다.**
  2. **외부 의존성(DB, 외부 API)과 통신하는 인터페이스는 반드시 `domain` 패키지 하위에 위치한다. (DIP)**

---

## 2. 레이어 의존성 방향

```
[api]          Controller — HTTP in/out 전담, 비즈니스 로직 없음
   ↓
[business]     Service / Orchestrator 등 — 비즈니스 로직 투영
   ↓
[domain]       Entity, Value Object, Repository Interface (Port)
   ↑
[storage]      JPA Entity, RepositoryImpl (Adapter) — domain 인터페이스 구현
[external]     외부 API Client Impl (Adapter)
[infra]        이벤트, 메시징 Adapter
```

**의존성 역전 원칙 (DIP):**
- `domain` 패키지는 `storage`, `external`, `infra`를 **절대 import하지 않는다.**
- `storage`, `external`, `infra`가 `domain`의 인터페이스를 구현한다.

```java
// GOOD — domain 하위에 인터페이스 정의
package com.kopang.domain.order;
public interface OrderRepository { ... }   // Port

// GOOD — storage가 구현
package com.kopang.storage.order;
public class OrderRepositoryImpl implements OrderRepository { ... }  // Adapter

// BAD — domain이 JPA를 알면 안 됨
package com.kopang.domain.order;
import com.kopang.storage.order.OrderJpaRepository;  // 금지
```

---

## 3. Domain 레이어 규칙

### 순수 POJO 유지
도메인 클래스에 아래 어노테이션은 **금지**:
- `@Entity`, `@Table`, `@Column`, `@Id` — JPA 어노테이션
- `@Service`, `@Component`, `@Repository` — Spring 어노테이션
- `@Transactional` — Spring 트랜잭션

### 불변 객체 (Immutable)
상태 변경 메서드는 **새 인스턴스를 반환**한다. `this`를 직접 수정하지 않는다.

```java
// GOOD — 새 인스턴스 반환
public Order preparePayment(Money amount, LocalDateTime now) {
    validate(...);
    return new Order(no, memberNo, OrderStatus.PAYMENT_IN_PROGRESS, totalPrice, products, orderedAt);
}

// BAD — 직접 상태 변경
public void preparePayment(...) {
    this.status = OrderStatus.PAYMENT_IN_PROGRESS; // 금지
}
```

### Static Factory Method 패턴
생성자는 `private` 또는 `package-private`. 외부에서는 의미 있는 이름의 static 팩토리 메서드만 사용한다.

```java
// GOOD
Order.createPending(memberNo, productNo, warehouseNo, count, price)
Payment.createSuccess(orderNo, paymentResult)
StockQuantity.from(value)
NotFoundException.order(orderNo)

// BAD
new Order(...)          // public 생성자 직접 호출
new NotFoundException("주문 1 없음")  // 팩토리 메서드 우회
```

### Lightweight DDD — Rich Domain Model
- 비즈니스 규칙은 Service가 아닌 **Domain 객체 내부**에 위치한다.
- Value Object를 적극 활용한다. (`Money`, `StockQuantity`, `Address`, `Coordinate` 등)
- Value Object는 `equals`/`hashCode`를 명시적으로 구현한다.

```java
// BAD — 비즈니스 규칙이 Service에 있음
public class OrderService {
    public void pay(Order order) {
        if (order.getStatus() != OrderStatus.PAYMENT_IN_PROGRESS) {
            throw new IllegalStateException("...");
        }
        order.setStatus(OrderStatus.PAID); // Domain 빈혈
    }
}

// GOOD — 비즈니스 규칙이 Domain에 있음
public class Order {
    public Order pay() {
        if (!status.isPaymentInProgress()) {
            throw new IllegalStateException("결제 진행 중인 주문만 승인할 수 있습니다.");
        }
        return new Order(..., OrderStatus.PAID, ...);
    }
}
```

---

## 4. Business 레이어 규칙

### Service — 원자적 단위 작업 집합

Service는 도메인 객체 간 협력을 이끌어내는 원자적 단위 작업의 집합이다.

- `@Service` 어노테이션 사용
- **모든 public 메서드에 `@Transactional` 필수**
- 읽기 전용 메서드는 `@Transactional(readOnly = true)`
- domain 인터페이스(Repository, Port)만 주입받는다. JPA Repository 직접 주입 금지.

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;  // domain 인터페이스 주입

    @Transactional
    public Order createOrderPending(...) { ... }

    @Transactional(readOnly = true)
    public Orders findExpiredPendingOrders(LocalDateTime now) { ... }
}
```

### 상위 레이어 — 필요에 의해 쌓는다

복잡한 비즈니스 플로우가 단일 Service 트랜잭션 경계로 처리하기 어려울 때, 상위 레이어를 추가한다.
상위 레이어의 형태는 Orchestrator만이 아니다. 비즈니스 로직의 성격에 따라 다를 수 있다.

**Orchestrator 패턴 (대표적 상위 레이어):**
- 트랜잭션 없이 전체 비즈니스 흐름을 조율하는 관리자 역할
- `@Component` 사용, **`@Transactional` 금지** (의도적)
- 보상(Compensation) 로직을 담당
- 외부 API 호출 + Service 호출 조합 시 적합

```java
// GOOD — Orchestrator: 보상 후 예외 재전파
@Component
public class PurchaseOrchestrator {
    // @Transactional 없음 — 의도적

    public ReservationOrderResult reserve(...) {
        // 각 단계는 Service가 트랜잭션 관리
        Optional<StockQuantity> sq = stockReservationService.decrease(...);
        try {
            Order order = orderService.createOrderPending(...);
            return new ReservationOrderResult(sq.get(), order);
        } catch (Exception e) {
            stockReservationService.increase(...);  // 보상
            throw e;                                // 반드시 재전파
        }
    }
}
```

---

## 5. Storage 레이어 규칙

JPA Entity는 **storage 패키지 내부에만 존재**한다. domain/service/api로 노출하지 않는다.

- domain → storage 변환: `XxxJpaEntity.from(domainObject)`
- storage → domain 변환: `entity.toDomain()`

```java
// GOOD — RepositoryImpl이 변환 책임
@Override
public Order register(Order order) {
    return orderJpaRepository.save(OrderJpaEntity.from(order)).toDomain();
}

// BAD — Service가 JPA Entity를 직접 다룸
public class OrderService {
    @Autowired OrderJpaRepository jpaRepo;  // storage 직접 주입 금지
    public void register(OrderJpaEntity entity) { ... }  // JPA Entity 노출 금지
}
```

**네이밍 규칙:**
| 종류 | 예시 |
|------|------|
| domain 인터페이스 | `OrderRepository` |
| storage 구현체 | `OrderRepositoryImpl` |
| Spring Data JPA | `OrderJpaRepository` |
| JPA Entity | `OrderJpaEntity` |

---

## 6. 아키텍처 원칙 위반 사례 (리팩토링 타겟)

새 코드 작성 시 아래 패턴이 나타나면 즉시 중단하고 수정한다.

| 위반 패턴 | 올바른 방향 |
|-----------|------------|
| domain 클래스에 `@Entity` 추가 | JPA Entity 클래스를 storage에 별도 생성 |
| Service에 `OrderJpaRepository` 직접 주입 | domain `OrderRepository` 인터페이스 주입 |
| Orchestrator에 `@Transactional` 추가 | Service 레이어에 원자적 메서드 위임 |
| storage 패키지의 Entity가 api/service로 반환 | toDomain() 변환 후 domain 객체 반환 |
| scheduler가 JPA Repository 직접 주입 | domain 인터페이스 또는 Service 경유 |
