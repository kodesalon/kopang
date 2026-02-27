# /harness-check

작성된 코드가 Kopang Harness 가이드라인을 준수하는지 검증한다.

## 실행 순서

1. `.claude/skills/kopang-harness-guide/ARCHITECTURE.md` 읽기
2. `.claude/skills/kopang-harness-guide/ERROR_HANDLING.md` 읽기
3. `.claude/skills/kopang-harness-guide/TESTING.md` 읽기
4. 검토 대상 코드(또는 최근 변경 파일)에 대해 아래 체크리스트 실행

## 체크리스트

### Architecture
- [ ] 레이어 의존성 방향 준수 (api→service→domain, storage→domain)
- [ ] Orchestrator: @Transactional 없음, @Component
- [ ] Service: 각 메서드 @Transactional
- [ ] Domain: POJO, 불변, static factory
- [ ] Storage: JpaEntity 캡슐화, from/toDomain 패턴

### Error Handling
- [ ] Domain 예외: IllegalStateException / IllegalArgumentException
- [ ] Service 예외: static factory method 패턴
- [ ] 새 예외 추가 시 GlobalExceptionController 등록
- [ ] 보상 로직 후 예외 재전파

### Testing
- [ ] 상태 머신 전이 매트릭스 커버
- [ ] 경계값 테스트 포함
- [ ] Orchestrator 보상 검증 테스트 포함
- [ ] Fixture 클래스 상태 전이 순서 올바름

## 출력 형식

```
✅ PASS: [항목]
❌ FAIL: [항목] — [설명] → [수정 방향]
⚠️  WARN: [항목] — [권장사항]

총 PASS: N / FAIL: N / WARN: N
```

FAIL이 1개라도 있으면 코드 제출 전 반드시 수정한다.