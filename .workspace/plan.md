# 구현 계획서 — #57 이벤트 선착순 공정성 보장 대기열 아키텍처 도입 (v1 Lua Script → v2 Queue)

> 설계 근거·아키텍처 토론: `.workspace/plan-design.md` 에 별도 작성

---

## 1. 선행 컨텍스트

| 문서    | 경로                                                                 | 숙지 포인트                                                                                |
|:------|:-------------------------------------------------------------------|:--------------------------------------------------------------------------------------|
| 아키텍처  | `.claude/skills/kopang-harness-guide/references/ARCHITECTURE.md`   | `infra/` vs `storage/` 구별(임시 배관 vs 영속), Orchestrator @Transactional 금지, toDomain() 패턴 |
| 예외 처리 | `.claude/skills/kopang-harness-guide/references/ERROR_HANDLING.md` | 새 예외 시 GlobalExceptionController 등록 필수, Scheduler log.warn 허용                         |
| 테스트   | `.claude/skills/kopang-harness-guide/references/TEST_GUIDE.md`     | 도메인 단위(POJO), 인수(@AcceptanceTest + Map), 동시성(CountDownLatch)                          |
| 참조 코드 | `service/purchase/PurchaseOrchestrator.java`                       | @Component 패턴                                                                         |
| 참조 코드 | `storage/stock/RedisStockReservationRepositoryImpl.java`           | Redis 어댑터 구현 패턴 (StringRedisTemplate 직접 사용)                                           |
| 참조 코드 | `api/controller/v1/order/OrderController.java`                     | Controller record DTO 패턴                                                              |
| 참조 코드 | `api/controller/v1/order/OrderControllerTest.java`                 | @AcceptanceTest + Map 인수 테스트 패턴, @AfterEach Redis 정리                                  |

---

## 2. 완료 조건 (Acceptance Criteria)

- [ ] `POST /api/v2/events/{eventId}/queue?memberNo={memberNo}` 가 202를 반환하고 `token`, `position`, `estimatedWaitMs`를 포함한다
- [ ] `GET /api/v2/events/{eventId}/queue/{token}/status` 가 DB 조회 없이 Redis만으로 `WAITING`(+position) / `ACTIVE` /
  `EXPIRED` 상태를 반환한다
- [ ] Redis Sorted Set 기반 FIFO 대기열: score = 요청 도달 timestamp(ms), 먼저 진입한 요청이 먼저 ACTIVE로 승격된다
- [ ] `EventQueueWorker`가 대기열에서 순서대로 항목을 꺼내 `queue:active:{eventId}` Set으로 이동하는 역할만 수행한다 (주문 로직 없음)
- [ ] Worker는 eventId 단위로 SETNX 락을 사용해 중복 실행을 방지한다
- [ ] `POST /api/v1/orders` 요청에 `X-Queue-Token` 헤더가 있을 때, Interceptor가 해당 token의 ACTIVE 여부를 검증하고 비활성 토큰은 거부한다
- [ ] ACTIVE 토큰은 5분 TTL을 가지며, 만료 후 폴링하면 `EXPIRED` 상태가 반환된다
- [ ] 기존 `POST /api/v1/orders` v1 플로우(`X-Queue-Token` 헤더 없는 경우)에 영향을 주지 않는다

---

## 3. 설계

### 데이터 구조 (Redis만 사용 — DB 테이블 없음)

| 키                        | 타입               | TTL                    | 용도                                                              |
|:-------------------------|:-----------------|:-----------------------|:----------------------------------------------------------------|
| `queue:event:{eventId}`  | Redis Sorted Set | 없음 (ZPOPMIN으로 소비 시 소멸) | FIFO 대기열 (score = 진입 시각 ms, member = token)                     |
| `queue:entry:{token}`    | Redis Hash       | 24h                    | 항목 상세 (memberNo, count, eventId) — Interceptor가 eventId 역조회에 사용 |
| `queue:active_events`    | Redis Set        | 없음                     | 워커가 폴링할 활성 이벤트 ID 목록                                            |
| `queue:active:{eventId}` | Redis Set        | 마지막 SADD 후 5분          | ACTIVE 토큰 목록. Interceptor와 폴링 API가 SISMEMBER로 검증                |
| `queue:lock:{eventId}`   | Redis String     | 2s                     | Worker 중복 실행 방지용 SETNX 락                                        |

> **`queue_result` DB 테이블 없음**: 대기열 도메인은 Redis 상태(WAITING / ACTIVE)만 관리.
> 실제 주문 성공/실패 영속화는 기존 Order 도메인(`orders` 테이블)에 위임.

### 전체 흐름

```
[진입] POST /api/v2/events/{eventId}/queue?memberNo={memberNo}
  → EventQueueController
  → EventQueueOrchestrator.enqueue(eventId, memberNo, count)
      ZADD queue:event:{eventId} {System.currentTimeMillis()} {token}
      HSET queue:entry:{token}  memberNo {v}  count {v}  eventId {v}
      EXPIRE queue:entry:{token} 86400
      SADD queue:active_events {eventId}
  ← 202 { token, position, estimatedWaitMs }

[워커] EventQueueWorker @Scheduled(fixedDelay = 500ms)
  for each eventId in SMEMBERS queue:active_events:
    SET queue:lock:{eventId} 1 NX EX 2          // SETNX 락 (2s TTL)
    if lock acquired:
      tokens = ZPOPMIN queue:event:{eventId} 10
      if tokens empty:
        SREM queue:active_events {eventId}
        continue
      SADD queue:active:{eventId} {tokens...}
      EXPIRE queue:active:{eventId} 300          // 5분 TTL

[폴링] GET /api/v2/events/{eventId}/queue/{token}/status
  → EventQueueController
  → EventQueueOrchestrator.getStatus(eventId, token)
      SISMEMBER queue:active:{eventId} {token}   → ACTIVE
      ZRANK queue:event:{eventId} {token}         → WAITING + position
      neither                                     → EXPIRED
  ← 200 { status: WAITING | ACTIVE | EXPIRED, position? }

[주문] POST /api/v1/orders  (기존 API + 새 Interceptor)
  → QueueTokenInterceptor.preHandle()
      if X-Queue-Token header absent → pass through (상시 주문)
      if present:
        eventId = HGET queue:entry:{token} eventId
        if eventId null → 401 (유효하지 않은 토큰)
        SISMEMBER queue:active:{eventId} {token}
        if false → 401 Unauthorized
  → OrderController (기존 로직 그대로)
```

**의존성 방향:**

```
api → service(Orchestrator) → domain(Port) ← infra(Adapter)
EventQueueWorker → EventQueueService(infra)
QueueTokenInterceptor → EventQueueService(infra)
```

**MVP 전제:** `eventId = productNo` — 별도 Event 엔티티 도입 없음

---

## 4. 구현 원칙

**공통:**

- domain interface(Port/Repository)를 통해서만 주입받으세요.
- Orchestrator/Scheduler는 `@Transactional` 없이 `@Component`를 사용하세요.
- Redis 전용 Service는 `@Transactional` 없음 (`StockReservationService` 패턴).
- `@Scheduled`는 `fixedDelay` 속성을 사용하세요.

**이번 작업:**

- 대기열 도메인은 **Redis 전용**. DB 접근 금지.
- 주문 성공/실패 결과는 Order 도메인에 위임 — `queue_result` 테이블 없음.
- Worker는 WAITING → ACTIVE 이동만 수행. `PurchaseOrchestrator` 의존 없음.
- Worker는 `SET ... NX EX 2` (SETNX + 2s TTL)으로 eventId 단위 락을 잡은 후 dequeue하세요.
- Interceptor는 `HandlerInterceptor` 구현체로 `WebMvcConfigurer`에 `/api/v1/orders`에만 등록하세요.
- token은 `UUID.randomUUID().toString()`으로 생성하세요.

---

## 5. 엣지 케이스

| # | 엣지 케이스                                       | 방어 전략                                                            | 적용 위치                                |
|:--|:---------------------------------------------|:-----------------------------------------------------------------|:-------------------------------------|
| 1 | 복수의 Worker 인스턴스가 동일 eventId를 동시에 처리          | `SET queue:lock:{eventId} 1 NX EX 2` — 락 획득 실패 시 해당 eventId skip | `EventQueueWorker`                   |
| 2 | ACTIVE 토큰 5분 만료 후 주문 시도                      | Interceptor가 `SISMEMBER` false → 401 반환                          | `QueueTokenInterceptor`              |
| 3 | 존재하지 않는 token으로 폴링                           | WAITING / ACTIVE 어느 곳에도 없음 → `{ status: EXPIRED }` 200 반환        | `EventQueueOrchestrator.getStatus()` |
| 4 | queue:entry:{token} 만료(24h) 후 Interceptor 검증 | `HGET eventId` null → 401                                        | `QueueTokenInterceptor`              |
| 5 | active_events가 비어있을 때 Worker 실행              | `SMEMBERS` 빈 Set → for loop 즉시 종료                                | `EventQueueWorker`                   |
| 6 | Worker dequeue 후 SADD 실패                     | log.warn + 해당 eventId 처리 중단(토큰은 이미 제거됨 — 허용 가능 손실)               | `EventQueueWorker`                   |
| 7 | 동일 사용자 중복 대기열 진입                             | `SET queue:member:{eventId}:{memberNo} {token} NX EX 86400` — 이미 존재 시 `DuplicateQueueEntryException` → 409 Conflict | `EventQueueRepositoryImpl.enqueue()` |

---

## 6. 구현 순서

의존성이 적은 방향 (domain → infra → service → scheduler → interceptor → api) 으로 순차 진행. 각 Step은 독립적으로 컴파일·검증 가능.

---

### Step 1 — Domain 모델: QueueStatus, QueueEntry

**패키지:** `com.kodesalon.kopang.domain.queue`. 순수 POJO, Spring/JPA 어노테이션 금지.

- `QueueStatus.java`: `enum { WAITING, ACTIVE, EXPIRED }`

- `QueueEntry.java`: record
  ```
  String token, Long eventId, Long memberNo, Integer count, long requestedAt

  static QueueEntry of(Long eventId, Long memberNo, Integer count)
    → token = UUID.randomUUID().toString()
    → requestedAt = System.currentTimeMillis()
  ```
  > `QueueResult` 없음 — 주문 결과는 Order 도메인에 위임.

**검증:** `./gradlew compileJava`

---

### Step 2 — Domain Port 인터페이스: EventQueueRepository

**패키지:** `com.kodesalon.kopang.domain.queue`

- `EventQueueRepository.java` (interface):
  ```java
  // 대기열 진입 (ZADD + HSET + SADD)
  QueueEntry enqueue(Long eventId, Long memberNo, Integer count);

  // FIFO dequeue (ZPOPMIN)
  List<QueueEntry> dequeueForProcessing(Long eventId, int batchSize);

  // 대기 순위 조회 (ZRANK, 0-based, 없으면 -1)
  long getPosition(Long eventId, String token);

  // 활성 이벤트 목록 (SMEMBERS queue:active_events)
  Set<Long> getActiveEventIds();

  // WAITING → ACTIVE 이동 (SADD + EXPIRE)
  void activateTokens(Long eventId, List<String> tokens);

  // ACTIVE 여부 확인 (SISMEMBER queue:active:{eventId})
  boolean isActive(Long eventId, String token);

  // token → eventId 역조회 (HGET queue:entry:{token} eventId)
  Optional<Long> findEventIdByToken(String token);

  // eventId 단위 락 획득 (SET NX EX)
  boolean acquireLock(Long eventId);
  ```

**검증:** `./gradlew compileJava`

---

### Step 3 — Infra Adapter: EventQueueRepositoryImpl (Redis)

**패키지:** `com.kodesalon.kopang.infra.queue`. 임시 배관. DB 접근 금지.

- `EventQueueRepositoryImpl.java`: `@Repository implements EventQueueRepository`
    - 주입: `StringRedisTemplate`
    - 상수:
      ```
      QUEUE_KEY        = "queue:event:%d"
      ENTRY_KEY        = "queue:entry:%s"
      ACTIVE_EVENTS    = "queue:active_events"
      ACTIVE_KEY       = "queue:active:%d"
      LOCK_KEY         = "queue:lock:%d"
      ENTRY_TTL_SEC    = 86400   // 24h
      ACTIVE_TTL_SEC   = 300     // 5min
      LOCK_TTL_SEC     = 2       // 2s
      ```
    - `enqueue()`:
        1. `token = UUID.randomUUID().toString()`
        2. `ZADD queue:event:{eventId} {requestedAt} {token}`
        3. `HSET queue:entry:{token} memberNo {v} count {v} eventId {v}`
        4. `EXPIRE queue:entry:{token} 86400`
        5. `SADD queue:active_events {eventId}`
        6. `ZRANK queue:event:{eventId} {token}` → position
        7. `return new QueueEntry(token, eventId, memberNo, count, requestedAt)` (position은 반환값 별도 처리)
    - `dequeueForProcessing()`:
        1. `ZPOPMIN queue:event:{eventId} {batchSize}` → `Set<ZSetOperations.TypedTuple<String>>`
        2. 각 token으로 `HGETALL queue:entry:{token}` → QueueEntry 조립
        3. 빈 HGETALL 결과는 skip (entry 만료 방어)
    - `activateTokens()`:
        1. `SADD queue:active:{eventId} {tokens...}`
        2. `EXPIRE queue:active:{eventId} 300`
        3. `ZCARD queue:event:{eventId} == 0` → `SREM queue:active_events {eventId}`
    - `isActive()`: `Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("queue:active:" + eventId, token))`
    - `findEventIdByToken()`: `HGET queue:entry:{token} eventId` → `Optional<Long>`
    - `acquireLock()`: `redisTemplate.opsForValue().setIfAbsent("queue:lock:" + eventId, "1", Duration.ofSeconds(2))`

**검증:** `./gradlew compileJava`

---

### Step 4 — Service: EventQueueService (Redis 전용)

**패키지:** `com.kodesalon.kopang.service.queue`. `@Service` (NO @Transactional — Redis 전용)

- `EventQueueService.java`:
    - 주입: `EventQueueRepository`
  ```java
  QueueEntry enqueue(Long eventId, Long memberNo, Integer count)
  List<QueueEntry> dequeueForProcessing(Long eventId, int batchSize)
  long getPosition(Long eventId, String token)
  Set<Long> getActiveEventIds()
  void activateTokens(Long eventId, List<String> tokens)
  boolean isTokenActive(Long eventId, String token)
  Optional<Long> findEventIdByToken(String token)
  boolean acquireLock(Long eventId)
  ```

**검증:** `./gradlew compileJava`

---

### Step 5 — Orchestrator: EventQueueOrchestrator

**패키지:** `com.kodesalon.kopang.service.queue`. `@Component`, `@Transactional` 금지.

- `EnterQueueResult.java`: record `(String token, long position, long estimatedWaitMs)` — service 패키지
- `QueueStatusResult.java`: record `(QueueStatus status, Long position)` — service 패키지

- `EventQueueOrchestrator.java`:
    - 주입: `EventQueueService`
  ```java
  EnterQueueResult enqueue(Long eventId, Long memberNo, Integer count) {
      QueueEntry entry = eventQueueService.enqueue(eventId, memberNo, count);
      long position = eventQueueService.getPosition(eventId, entry.token());
      long estimatedWaitMs = position * 500L;  // 항목당 500ms 추정 (fixedDelay 기준)
      return new EnterQueueResult(entry.token(), position, estimatedWaitMs);
  }

  QueueStatusResult getStatus(Long eventId, String token) {
      if (eventQueueService.isTokenActive(eventId, token)) {
          return new QueueStatusResult(QueueStatus.ACTIVE, null);
      }
      long position = eventQueueService.getPosition(eventId, token);
      if (position >= 0) {
          return new QueueStatusResult(QueueStatus.WAITING, position);
      }
      return new QueueStatusResult(QueueStatus.EXPIRED, null);
  }
  ```

**검증:** `./gradlew compileJava`

---

### Step 6 — Scheduler: EventQueueWorker

**패키지:** `com.kodesalon.kopang.scheduler`. `@Component`, `@Transactional` 금지. `PurchaseOrchestrator` 의존 없음.

- `EventQueueWorker.java`:
    - 주입: `EventQueueService`
  ```java
  @Scheduled(fixedDelay = 500)
  void processQueue() {
      Set<Long> activeEventIds = eventQueueService.getActiveEventIds();
      for (Long eventId : activeEventIds) {
          if (!eventQueueService.acquireLock(eventId)) {
              continue;  // 다른 인스턴스가 처리 중
          }
          try {
              List<QueueEntry> entries = eventQueueService.dequeueForProcessing(eventId, 10);
              if (entries.isEmpty()) continue;
              List<String> tokens = entries.stream().map(QueueEntry::token).toList();
              eventQueueService.activateTokens(eventId, tokens);
          } catch (Exception e) {
              log.warn("대기열 활성화 실패: eventId={}, reason={}", eventId, e.getMessage());
          }
      }
  }
  ```
  > 락 TTL(2s)이 만료되면 자동 해제. 명시적 DEL 불필요.

**검증:** `./gradlew compileJava`

---

### Step 7 — Interceptor: QueueTokenInterceptor

**패키지:** `com.kodesalon.kopang.api.interceptor`.

- `QueueTokenInterceptor.java`: `implements HandlerInterceptor`
    - 주입: `EventQueueService`
  ```java
  @Override
  boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
      String token = request.getHeader("X-Queue-Token");
      if (token == null) return true;  // 헤더 없으면 pass (상시 주문)

      Optional<Long> eventId = eventQueueService.findEventIdByToken(token);
      if (eventId.isEmpty()) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return false;
      }
      if (!eventQueueService.isTokenActive(eventId.get(), token)) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return false;
      }
      return true;
  }
  ```

- `WebConfig.java` (신규 또는 기존 WebMvcConfigurer 확인): `@Configuration implements WebMvcConfigurer`
  ```java
  @Override
  void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(queueTokenInterceptor)
          .addPathPatterns("/api/v1/orders");
  }
  ```

**검증:** `./gradlew compileJava`

---

### Step 8 — API: EventQueueController

**패키지:** `com.kodesalon.kopang.api.controller.v2.queue`

- `EnterQueueRequest.java`: record `(Integer count)`

- `EnterQueueResponse.java`: record `(String token, long position, long estimatedWaitMs)`
  ```java
  static EnterQueueResponse of(EnterQueueResult result)
  ```

- `QueueStatusResponse.java`: record `(String status, Long position)`
  ```java
  static QueueStatusResponse of(QueueStatusResult result)
    → status = result.status().name()
  ```

- `EventQueueController.java`:
  ```java
  @RestController
  @RequestMapping("/api/v2/events/{eventId}/queue")
  // 주입: EventQueueOrchestrator

  // POST /api/v2/events/{eventId}/queue?memberNo={memberNo}
  @PostMapping
  ResponseEntity<EnterQueueResponse> enterQueue(
      @PathVariable Long eventId,
      @RequestParam Long memberNo,
      @RequestBody EnterQueueRequest request
  ) → 202 Accepted

  // GET /api/v2/events/{eventId}/queue/{token}/status
  @GetMapping("/{token}/status")
  ResponseEntity<QueueStatusResponse> getQueueStatus(
      @PathVariable Long eventId,
      @PathVariable String token
  ) → 200 OK
  ```

**검증:** `./gradlew compileJava`

---

### Step 9 — 테스트

아래 표에서 이 기능에 **해당하는 유형만** Y로 표시하고 구현하세요.

| 유형     | 해당 | 대상                                                                                             | 핵심 케이스                                                                                                     |
|:-------|:--:|:-----------------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------|
| 도메인 단위 | Y  | `QueueEntry`                                                                                   | `of()` 정적 팩토리 token UUID 생성, requestedAt 설정, QueueStatus enum 값                                            |
| 인수     | Y  | `POST /api/v2/events/{eventId}/queue`, `GET .../status`, `POST /api/v1/orders + X-Queue-Token` | 정상 진입 → 202, ACTIVE 상태 폴링 → ACTIVE, EXPIRED 폴링 → EXPIRED, 유효한 ACTIVE token으로 주문 → 201, 만료 token으로 주문 → 401 |
| 동시성    | Y  | `EventQueueWorker` FIFO 순서                                                                     | 50개 동시 진입 시, Worker 실행 후 ZPOPMIN 순서(timestamp 오름차순)대로 ACTIVE Set에 들어감을 SISMEMBER로 검증                       |

**공통 규칙:** `@Nested` 계층 구조, 메서드명 `테스트대상_상태_기대결과` (영문), Fixture는 `static final` 금지·메서드 호출 방식 사용

**인수 테스트 fixture:** `src/test/resources/acceptance/queue/` (product, warehouse, member_address, stock JSON)

**검증:**

```
./gradlew test --tests "com.kodesalon.kopang.domain.queue.QueueEntryTest"
./gradlew test --tests "com.kodesalon.kopang.api.controller.v2.queue.EventQueueControllerTest"
```

---

## 7. 검증 체크리스트

**구조:**

- [ ] Service가 Port interface만 주입받는가?
- [ ] `domain/` 패키지에 Spring/JPA 어노테이션이 없는가?
- [ ] Orchestrator/Scheduler에 `@Transactional`이 없는가?
- [ ] `EventQueueWorker`에 `PurchaseOrchestrator` 의존이 없는가?
- [ ] `infra/queue/`에 DB 접근 코드가 없는가?
- [ ] 섹션 4(구현 원칙)를 모두 준수하는가?

**비즈니스:**

- [ ] 섹션 5(엣지 케이스)의 방어 전략이 코드에 반영되었는가?
- [ ] Interceptor가 `WebMvcConfigurer`에 `/api/v1/orders` 경로로만 등록되었는가?
- [ ] `X-Queue-Token` 헤더 없는 요청은 Interceptor를 통과하는가?

**테스트:**

- [ ] Step 9에서 Y로 표시한 테스트 유형이 모두 작성되었는가?
- [ ] 인수: `@AcceptanceTest` + Map 타입을 사용했는가?
- [ ] 인수: `@AfterEach`에서 Redis 키 정리가 포함되었는가?
- [ ] 동시성: `CountDownLatch` + `ExecutorService` 패턴을 사용했는가?
- [ ] Fixture 상태 전이 순서가 올바른가?
