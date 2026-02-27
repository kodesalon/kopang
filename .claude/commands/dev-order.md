# /dev-order

Order/Payment 도메인 개발 시작 커맨드. 하네스 가이드를 로드하고 개발 준비 상태로 전환한다.

## 실행 순서

1. 다음 파일을 **순서대로** 읽어 컨텍스트에 로드한다:
   - `.claude/skills/kopang-harness-guide/ARCHITECTURE.md`
   - `.claude/skills/kopang-harness-guide/ERROR_HANDLING.md`
   - `.claude/skills/kopang-harness-guide/TESTING.md`

2. 작업 요청을 받으면 코드 작성 전에 다음을 명시한다:
   - 수정 대상 레이어 (domain / service / storage / api)
   - 영향받는 클래스 목록
   - 준수할 아키텍처 규칙 (관련 항목 명시)

3. 코드 작성 후 `.claude/commands/harness-check.md` 체크리스트를 자체 검증한다.

## 사용 예시
```
/dev-order
→ 주문 만료 처리 로직에 배치 사이즈 제한 추가
```
