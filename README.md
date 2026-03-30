# Kopang (코팡)

> 선착순 이벤트 주문 처리 플랫폼 서버 구현

---

## 프로젝트 개요

**Kopang**은 선착순 이벤트 상품을 다수의 사용자가 동시에 주문하는 고트래픽 상황에서 **공정성** 과 **일관성** 보장을 목표로 하는 커머스 서버입니다.

단순한 기능 구현을 넘어, 실제 대용량 트래픽에서 발생하는 문제를 직접 측정하고 아키텍처를 개선하는 과정을 기록합니다.

### 핵심 목표

- 단순한 기능 구현이 아니라 대용량 트래픽 처리까지 고려하는 것을 목표로 함
- 선착순 재고 차감 시스템의 **공정성 보장** (먼저 요청한 사용자가 먼저 처리)
- **원자적** 재고 예약으로 중복/초과 차감 방지
- 결제 실패/타임아웃 시 **자동 재고 복구**
- 장애 상황에서도 데이터 **일관성 유지** (스케줄러 기반 자가 복구)

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| **언어 / 프레임워크** | Java 21, Spring Boot 3.5.8 |
| **데이터베이스** | MySQL 8.0.41 (운영), H2 (개발) |
| **캐시 / 대기열** | Redis (Sorted Set, Lua Script) |
| **로컬 캐시** | Caffeine |
| **ORM** | Spring Data JPA / Hibernate |
| **메시징** | Kafka (구현 예정) |
| **모니터링** | Micrometer, Prometheus, Spring Actuator |
| **테스트** | JUnit 5, REST Assured |
| **빌드** | Gradle |

---

## 요청 흐름

### 주문 흐름

<img width="1057" height="584" alt="스크린샷 2026-03-31 오전 1 20 34" src="https://github.com/user-attachments/assets/4fe621d5-2c8e-4eea-91b6-e8ed2b26cf7d" />

### 결제 흐름

<img width="1170" height="708" alt="스크린샷 2026-03-31 오전 1 21 22" src="https://github.com/user-attachments/assets/61454519-038c-4488-bb19-8a7ba5786099" />
<img width="958" height="233" alt="스크린샷 2026-03-31 오전 1 22 07" src="https://github.com/user-attachments/assets/e7e68914-8382-4582-8d8f-672d87685c61" />


---

## 기술적 문제와 해결 과정

### 도메인 설계

* [상품, 재고, 주문 도메인 경계 설계 : 변경 주기와 생명주기를 기준으로](https://cozo-dev.notion.site/32f7eec275a780d6863dd7f82976649a)
* [도메인 엔티티 JPA 엔티티 분리](https://cozo-dev.notion.site/JPA-32f7eec275a7802a8563eb97c4c3f289)

### 동시성 제어

* [동시성 제어 전략 1 : 비관적 락에서 Redisson 분산 락으로](https://cozo-dev.notion.site/1-32f7eec275a78067bfbbf8d1c867c945)
* [동시성 제어 전략 2 : Lua Script 수렴과 Eventual Consistency의 시작](https://cozo-dev.notion.site/2-32f7eec275a7800698b1cd056adf501d)

### Eventual Consistency

* [Eventual Consistency 전략 1 : Redis-DB 불일치, 세 가지 동기화 전략 비교와 MQ 선택](https://cozo-dev.notion.site/Eventual-Consistency-1-32f7eec275a780f2b2d3e9b8747f7205)
* [Eventual Consistency 전략 2 : RabbitMQ, Kafka, Redis Streams — 그리고 Redis Streams 선택](https://cozo-dev.notion.site/Eventual-Consistency-2-32f7eec275a780979a84f3f0002cc7d2)
* [Eventual Consistency 전략 3 : Redis Lua Script의 원자성 원리와 Redis Streams 재확인](https://cozo-dev.notion.site/Eventual-Consistency-3-32f7eec275a7801b892be57f28da04c6)
* [Eventual Consistency 전략 4 (최종) : Redis Streams를 포기하고 Kafka + Outbox Pattern으로](https://cozo-dev.notion.site/Eventual-Consistency-4-32f7eec275a780008579f174b180b6bb)

### Redis 운용

* [Redis 서버가 뻗었을 때 : Replication](https://cozo-dev.notion.site/Redis-Replication-32f7eec275a7809aabb7ceca406bc014)
* [Redis 메모리 한계 돌파 : Sharding](https://cozo-dev.notion.site/Redis-Sharding-32f7eec275a780b9bacce1de9fa1323e)
* [Redis 서버가 뻗었을 때 : HA](https://cozo-dev.notion.site/Redis-HA-32f7eec275a780998ddff19c7a7c2c76)
* [Redis 컬렉션 활용 전략](https://cozo-dev.notion.site/Redis-32f7eec275a78033b01fc72be7620074)

### 결제 & 주문 취소

* [결제 & 주문 취소 설계 : PAYMENT_IN_PROGRESS 추가와 만료 시간 분리로 동시성 제거](https://cozo-dev.notion.site/32f7eec275a7809c9f0ef9eee7f35670)
* [만료 주문 자동 취소 스케줄러 페이지네이션 반환 타입 : Slice vs. List](https://cozo-dev.notion.site/32f7eec275a7800daf4dd3bfeb79f6e8?v=32f7eec275a780ec9b5c000cfb9c5a6e&p=32f7eec275a7805b8134d08f848639b8&pm=s)
* [결제 & 주문 취소 비즈니스 예외 상황과 해결](https://cozo-dev.notion.site/32f7eec275a7802ab26ecafea47eac31)

### 성능 테스트 & 튜닝

* [1차 성능 테스트 : CPU는 여유로운데 p95가 3초 — 스레드 덤프로 찾은 DB 커넥션 고갈](https://cozo-dev.notion.site/1-32f7eec275a780feb947ee6721fd827e)
* [2차 성능 테스트 : 캐시로 TPS 2배, 그러나 DB 커넥션은 여전히 막혔다](https://cozo-dev.notion.site/2-32f7eec275a7805190ccebb97d63c314)
* [2차 성능 테스트 : 코드 한 줄 안 바꿨는데 TPS 4배 — JIT C2 컴파일러로 직접 검증](https://cozo-dev.notion.site/2-JVM-32f7eec275a78092bef3f861e4f1469b)
* [3차 성능 테스트 : TPS를 올릴수록 시스템이 무너졌다 — 30/30이 최적인 이유](https://cozo-dev.notion.site/3-32f7eec275a7807182fbc2911f4e2244)
* [실전 운영을 위한 Cache Warm-Up : 1000만 명은 예열할 수 없다, 진짜 참여자만 캐싱하는 방법](https://cozo-dev.notion.site/Cache-Warm-Up-32f7eec275a780c89289cd8b42ffc8b6)
