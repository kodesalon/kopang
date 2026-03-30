# Kopang — 선착순 재고 차감 커머스 서버

## 기술 스택
- Java 21, Spring Boot 3.5.8, JPA, Redis, Kafka, MySQL
- 테스트: JUnit5, RestAssured, TestContainers, Embedded Redis
- 빌드: `./gradlew build` | 테스트: `./gradlew test`

## 아키텍처 핵심 규칙
> 상세: `.claude/skills/kopang-harness-guide/references/ARCHITECTURE.md`

- Hexagonal Architecture: domain → port(interface) → adapter(구현)
- `domain/` 패키지에 Spring/JPA 어노테이션(`@Entity`, `@Service`, `@Component`, `@Transactional`) 금지 — 순수 POJO
- Application(Orchestrator)에 `@Transactional` 금지 — 트랜잭션은 Service 레이어에서 관리
- DB(JPA) 작업 Service = `@Transactional` 필수. Redis 전용 Service = `@Transactional` 없음 (`StockReservationService` 패턴)
- 외부 호출(Redis, PG, HTTP)은 `@Transactional` 밖에서 실행
- `storage/` → `domain/` 변환은 반드시 `toDomain()` 패턴
- `infra/` 는 소비되면 소멸하는 임시 인프라 배관 (대기열, 캐시 등). 영속 데이터(`storage/`)와 구별

## 예외 처리 핵심
> 상세: `.claude/skills/kopang-harness-guide/references/ERROR_HANDLING.md`

- 새 예외 추가 시 `GlobalExceptionController`에 핸들러 필수 등록
- Scheduler에서 예외 삼킴 허용 (단, `log.warn()` 필수)
- 보상 트랜잭션 후 원본 예외 재전파

## 테스트 핵심
> 상세: `.claude/skills/kopang-harness-guide/references/TEST_GUIDE.md`

- FIRST 원칙 (Fast, Isolated, Repeatable, Self-validating, Timely)
- 인수 테스트: `@AcceptanceTest` 어노테이션 활용
- 동시성 테스트: `CountDownLatch` + `ExecutorService` 패턴

## 작업 프로세스 (필수)
1. 새 기능 구현 전: `/plan` 으로 계획서 작성 → **사용자 승인 후** 코딩 시작
2. 구현 시: `/implement` 로 plan 기반 Step별 순차 구현
3. 구현 완료 후: `/review` 로 검증 체크리스트 자가 점검

## 주요 코드 위치
- 주문 핵심: `service/purchase/PurchaseOrchestrator.java`
- Redis 패턴: `storage/stock/RedisStockReservationRepositoryImpl.java`
- 예외 처리: `api/controller/GlobalExceptionController.java`
- Controller 패턴: `api/controller/v1/order/OrderController.java`