---
description: >
  새 기능 구현 전 plan.md 계획서를 작성합니다.
  사용법: /plan #57
allowed-tools: Read, Glob, Grep, Write, Agent
---

## 지시

1. `$ARGUMENTS`에서 이슈 번호를 추출한 뒤, `gh issue view <번호>` 명령어로 해당 이슈를 조회하고 제목·본문·레이블을 분석하세요.
2. 아래를 **병렬로 즉시 실행**하세요:
   - **[병렬 A]** `Read`: 참조 문서 4개 동시 로드
     - `.claude/templates/plan-template.md`
     - `.claude/skills/kopang-harness-guide/references/ARCHITECTURE.md`
     - `.claude/skills/kopang-harness-guide/references/ERROR_HANDLING.md`
     - `.claude/skills/kopang-harness-guide/references/TEST_GUIDE.md`
   - **[병렬 B]** `Agent(Explore)`: 이슈 기능과 관련된 기존 구현 패턴 탐색 (유사 Orchestrator, Service, domain 클래스)
   - **[병렬 C]** `Agent(Explore)`: 관련 domain interface / Repository 현황 파악
   - **[병렬 D]** `Agent(Explore)`: `fixtures/` 디렉토리 기존 테스트 픽스처 패턴 파악
   - **[병렬 E]** `Agent(Explore)`: `GlobalExceptionController` 등록 현황 파악
3. 수집된 컨텍스트를 바탕으로 `plan-template.md` 구조에 따라 계획서를 작성하세요.
   - 섹션 6: 각 Step 블록을 자기완결형으로 작성하세요. Step 처리에 필요한 클래스 스펙·제약을 해당 Step 블록 안에 포함하세요.
4. `.workspace/plan.md`에 저장하세요.

## 원칙

- 계획서 텍스트만 작성하세요.
- 계획서 작성 완료 후 반드시 다음을 출력하세요:

```
구현 계획서 작성을 완료했습니다. 검토해 주세요:
1. 완료 조건(섹션 2)이 이번 작업의 목표와 일치합니까?
2. 구현 원칙(섹션 4)에 추가할 항목이 있습니까?
3. 엣지 케이스(섹션 5)에서 누락된 시나리오가 있습니까?
4. 구현 순서(섹션 6)를 승인합니까?

승인 시 `/implement` 로 구현을 시작하세요.
```