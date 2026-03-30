---
description: >
  승인된 plan.md를 기반으로 Step별 구현을 시작합니다.
  사용법: /implement (기본: .workspace/plan.md 자동 로드)
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Agent
---

## 지시

1. `$ARGUMENTS` 경로의 plan을 읽으세요. 없으면 `.workspace/plan.md`.
2. plan 섹션 1(선행 컨텍스트)에 명시된 문서와 참조 코드를 읽으세요. **현재 대화에서 이미 Read한 파일은 Read tool을 호출하지 말고 건너뛰세요.**
3. plan 섹션 4(구현 원칙)를 숙지하세요. 위반 감지 시 즉시 중단하고 보고하세요.
4. plan 섹션 6의 **Step 블록을 하나씩 순차 처리**하세요:
   - 해당 Step 블록만 작업 기준으로 삼으세요. 다른 Step 블록은 현재 Step과 무관합니다.
   - **테스트 Step** (검증 명령이 `./gradlew test --tests`인 경우): `Agent(subagent_type="general-purpose")` 호출 — "`.claude/agents/strict-test-writer.md`와 plan Step N 블록을 읽고, 이전 Step의 구현 파일을 대상으로 테스트를 작성하세요." 에이전트 반환값으로 파일 목록 갱신 후 검증 명령을 실행하세요.
   - **일반 Step:** Step 작업 직접 수행 → 생성/수정 파일 목록 출력
   - Step에 명시된 검증 명령 실행 (`./gradlew compileJava` 또는 `./gradlew test --tests "클래스명"`)
   - 검증 통과 시 "Step N 완료" 출력 후 **사용자 입력 없이 즉시 다음 Step으로 진행하세요.**
   - 검증 실패 시 원인을 분석하여 수정 후 재검증하세요. 수정 방향이 명확하지 않으면 즉시 중단하고 원인과 함께 보고하세요.
5. 모든 Step 완료 후 plan 섹션 7(검증 체크리스트)를 항목별 자가 점검하세요.

## 최종 보고 형식

```
## 구현 완료 보고

### 생성/수정된 파일
- [신규] path/to/NewFile.java
- [수정] path/to/ExistingFile.java

### Step별 검증 결과
| Step | 작업 | 검증 | 결과 |
|:---|:---|:---|:---|
| 1 | ... | ... | PASS |

### 검증 체크리스트 결과
| # | 항목 | 결과 | 근거 |
|:---|:---|:---|:---|
| 1 | ... | PASS | ... |

`/review`로 최종 검증을 실행할 수 있습니다.
```

## 원칙
- plan 섹션 6에 명시된 작업만 수행하세요. 범위 밖 작업은 사용자에게 먼저 보고하세요.
- plan에서 "재사용"으로 명시된 파일은 읽기만 하세요.
- 각 Step 완료 후 즉시 다음 Step으로 진행하세요.