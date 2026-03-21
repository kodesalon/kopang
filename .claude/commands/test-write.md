---
description: >
  대상 클래스의 누락된 테스트를 식별하고 엣지케이스를 포함한 완전한 테스트 코드를 작성합니다.
  사용법: /test-write OrderService 또는 /test-write src/main/java/.../Order.java
allowed-tools: Read, Glob, Grep, Agent
---

## 지시

1. `$ARGUMENTS`에서 대상 클래스명 또는 파일 경로를 추출하세요.
2. `Agent(subagent_type="general-purpose")`를 호출하세요. 프롬프트: "`.claude/agents/strict-test-writer.md`를 먼저 읽고 해당 역할을 따르세요. 대상: `$ARGUMENTS`. 기존 테스트 파일이 있다면 먼저 읽고 누락된 케이스만 추가하세요. 완료 후 생성/수정된 파일 경로와 작성한 케이스 목록을 반환하세요."
3. 결과를 아래 형식으로 보고하세요.

## 출력 형식

```
## 테스트 작성 완료

### 생성/수정된 파일
- [신규/수정] path/to/TestFile.java

### 작성된 케이스
- {TestClass}: {케이스명} × N개

### 의도적으로 제외한 케이스
- {케이스}: {이유}
```