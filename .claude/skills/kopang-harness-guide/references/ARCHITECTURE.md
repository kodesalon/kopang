# Architecture Rules

## 레이어 의존성
```
api → service → domain ← storage / infra / external
```
- `storage`/`infra`/`external`이 `domain` interface를 implements하세요. (DIP)
- `domain/`은 Java 표준 라이브러리와 domain 내부 패키지만 import하세요.

## Domain 규칙
- **Pure POJO:** domain 클래스에서 허용되는 어노테이션은 Lombok과 Java 표준(`@Override` 등)만입니다. Spring/JPA 어노테이션(`@Entity`, `@Table`, `@Service`, `@Component`, `@Transactional`)은 adapter 레이어에서만 사용하세요.
- **불변 객체:** 상태 변경 메서드는 `return new Order(...)` 형태로 새 인스턴스를 반환하세요.
- **Static Factory 필수:** 생성자는 private/package-private. `Order.createPending(...)` 형태를 사용하세요.
- **Rich Domain Model:** 비즈니스 규칙은 Domain 객체 내부에 위치시키세요.

## Service 규칙
- `@Service` + 모든 public 메서드에 `@Transactional` (읽기 전용은 `readOnly = true`)
- **예외:** Redis 전용 Service는 `@Transactional` 없음 (`StockReservationService` 패턴)
- domain interface(Repository, Port)를 통해서만 주입받으세요.

## Orchestrator 규칙
- `@Component`를 사용하세요. `@Transactional`은 Service 레이어에 위임하세요.
- 보상 후 반드시 `throw e`로 재전파하세요:
  ```java
  } catch (Exception e) { stockService.increase(...); throw e; }
  ```

## Storage 규칙
- JPA Entity는 storage 패키지 내부에서만 사용하세요.
- 변환: `XxxJpaEntity.from(domain)` / `entity.toDomain()`
- 네이밍: `OrderRepository` (domain) / `OrderRepositoryImpl` (impl) / `OrderJpaRepository` (Spring Data) / `OrderJpaEntity` (entity)

## Scheduler 규칙
- `@Component`를 사용하세요. `@Transactional`은 Service 레이어에 위임하세요. (Orchestrator와 동일)
- `@Scheduled`는 반드시 `fixedDelay` 속성을 사용하세요. (`fixedRate` 사용 금지)
- Scheduler에서 DB 접근이 필요한 경우 domain interface 또는 Service를 경유하세요. JPA Repository 직접 의존 금지.

## storage/ vs infra/ 구별
- `storage/`: 비즈니스 상태 영속 (JPA Entity, Redis 재고 수량 등)
- `infra/`: 소비되면 소멸하는 임시 배관 (대기열, 이벤트 발행)

## 올바른 방향

| 상황 | 올바른 방향 |
|:---|:---|
| 도메인 객체 영속 필요 | storage에 JPA Entity 별도 생성 후 toDomain() 변환 |
| DB 접근 필요 | domain Repository interface를 주입받아 사용 |
| 여러 Service 조율 필요 | Orchestrator에 위임, 각 Service에 @Transactional |
| Scheduler에서 DB 접근 필요 | domain interface 또는 Service 경유 |
| storage Entity를 반환해야 하는 상황 | toDomain() 변환 후 domain 객체 반환 |