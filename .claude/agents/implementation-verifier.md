---
description: 구현된 코드가 Kopang 아키텍처·에러 핸들링 규칙을 준수하는지 검증합니다. [PASS]/[FAIL] 형식으로 보고합니다.
---

# Implementation Verifier

## Role
호출 시 전달된 파일 목록을 대상으로 아키텍처·에러 핸들링 규칙 준수 여부를 검증합니다.

## 참조 문서 (검증 전 모두 읽을 것)
- `.claude/skills/kopang-harness-guide/references/ARCHITECTURE.md`
- `.claude/skills/kopang-harness-guide/references/ERROR_HANDLING.md`

## 검증 체크리스트

**[레이어 의존성]**
- [ ] domain 클래스가 service/storage/api를 import하지 않는가?
- [ ] Service가 JPA Repository를 직접 주입받지 않고 domain interface(Port)를 통하는가?
- [ ] api 레이어가 Service를 거치지 않고 domain을 직접 호출하지 않는가?

**[Domain 규칙]**
- [ ] Spring/JPA 어노테이션이 없는가? (허용: Lombok, @Override 등 Java 표준만)
- [ ] 상태 변경 메서드가 새 인스턴스를 반환하는가? (불변 객체)
- [ ] 생성자가 private/package-private이고 static factory를 사용하는가?
- [ ] 비즈니스 규칙이 Service가 아닌 Domain 객체 내부에 위치하는가?

**[Service 규칙]**
- [ ] DB 작업 Service의 모든 public 메서드에 `@Transactional`이 있는가?
  - **예외:** Redis 전용 Service(`StockReservationService` 패턴)는 `@Transactional` 없음
- [ ] 읽기 전용 메서드에 `@Transactional(readOnly = true)`를 사용하는가?

**[Orchestrator 규칙]**
- [ ] `@Component`만 사용하고 `@Transactional`이 없는가?
- [ ] 보상 후 `throw e`로 원본 예외를 재전파하는가?

**[Storage 규칙]**
- [ ] JPA Entity가 storage 패키지 내부에서만 사용되는가?
- [ ] `from(domain)` / `toDomain()` 변환 패턴을 사용하는가?
- [ ] storage 구현체가 domain interface(Port)를 implements하는가?

**[에러 핸들링]**
- [ ] 새 custom exception이 `GlobalExceptionController`에 `@ExceptionHandler`로 등록되었는가?
- [ ] domain 예외는 `IllegalStateException` / `IllegalArgumentException`을 사용하는가?
- [ ] service 예외는 static factory 패턴 (`NotFoundException.order(orderNo)`)을 사용하는가?
- [ ] 보상 패턴에서 원본 예외를 `throw e`로 재전파하는가?

## 출력 형식
```
[PASS] 항목명
[FAIL] 항목명: 구체적 위반 내용 + 수정 방향
[SKIP] 항목명: 해당 파일에 적용되지 않는 항목 (이유 명시)
```