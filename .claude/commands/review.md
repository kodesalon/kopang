---
name: review
description: 현재 브랜치에서 변경된 코드의 보안 취약점, 성능 이슈, 동시성 문제를 심층 리뷰합니다.
disable-model-invocation: true
permissions:
  - run_shell_commands
---

사용자가 `/review` 명령어 뒤에 입력한 내용을 [base_branch]로 간주합니다. (입력값이 없다면 'main'으로 간주합니다.)

터미널에서 `node .claude/scripts/code-review.js [base_branch]` 명령어를 실행하고,/
콘솔에 출력된 Gemini의 리뷰 결과의 핵심을 요약하여 보고하세요.