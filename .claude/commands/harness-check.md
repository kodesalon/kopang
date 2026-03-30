---
description: 작성된 코드가 Kopang 아키텍처·에러 핸들링·테스트 가이드라인을 준수하는지 검증합니다.
allowed-tools: Read, Glob, Grep
---

# /harness-check

작성된 코드가 Kopang Harness 가이드라인을 준수하는지 검증한다.

## 실행 순서

1. `.claude/skills/kopang-harness-guide/references/` 하위 ARCHITECTURE.md, ERROR_HANDLING.md, TEST_GUIDE.md 읽기 (현재 대화에서 이미 Read한 파일은 Read tool을 호출하지 말고 건너뛰세요)
2. 검토 대상 코드(또는 최근 변경 파일)에 대해 아래 체크리스트 실행

## 체크리스트

### Architecture
- [ ] 레이어 의존성 방향 준수 (api→service→domain, storage→domain)
- [ ] Orchestrator: @Transactional 없음, @Component
- [ ] Scheduler: @Transactional 없음, @Component, @Scheduled는 fixedDelay 속성 사용
- [ ] Service: 각 메서드 @Transactional (Redis 전용 Service는 제외)
- [ ] Domain: POJO, 불변, static factory
- [ ] Storage: JpaEntity 캡슐화, from/toDomain 패턴

### Error Handling
- [ ] Domain 예외: IllegalStateException / IllegalArgumentException
- [ ] Service 예외: static factory method 패턴
- [ ] 새 예외 추가 시 GlobalExceptionController 등록
- [ ] 보상 로직 후 예외 재전파

### Testing
- [ ] 경계값 테스트 포함
- [ ] Fixture 클래스 상태 전이 순서 올바름
- [ ] @AcceptanceTest 사용, Map 사용 (DTO 클래스 금지)

## 출력 형식

```
✅ PASS: [항목]
❌ FAIL: [항목] — [설명] → [수정 방향]
⚠️  WARN: [항목] — [권장사항]

총 PASS: N / FAIL: N / WARN: N
```

FAIL 항목은 보고 후 사용자의 확인을 받아 수정하세요.