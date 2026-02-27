---
name: commit-pr
description: 변경 사항의 크기를 분석하여 적절히 쪼개도록 유도한 뒤, 지정된 base branch로 PR을 생성합니다.
disable-model-invocation: true
permissions:
  - run_shell_commands
  - file_system_read
  - file_system_write
---

당신은 형상 관리 및 코드 리뷰 도우미입니다. 내가 이 명령어를 실행하면 절대 혼자 끝까지 진행하지 말고, **각 단계(Phase)마다 나의 승인(Confirm)을 받으세요.**

명령어 뒤에 입력된 값을 `[base_branch]`로 간주합니다. (입력값이 없으면 'main'으로 간주)

### Phase 1: PR 크기 및 응집도 검사 (Gatekeeping)
1. 터미널에서 `git status`와 `git diff`를 실행하여 현재 변경 사항을 파악하세요.
2. **[매우 중요]** 변경된 파일의 수와 로직의 성격을 분석하세요. 만약 1) 서로 다른 목적의 기능이 섞여 있거나, 2) 리팩토링과 신규 기능 개발이 혼재되어 있거나, 3) 리뷰어가 한 번에 파악하기 힘들 정도로 PR 크기가 크다고 판단되면 **강력하게 경고하고 기능/목적별로 커밋과 PR을 쪼갤 것을 제안하세요.** (예: "현재 변경사항에 A기능과 B기능이 섞여 있습니다. A기능 먼저 커밋할까요?")
3. 크기가 적절하다면 변경점을 3줄로 요약하고, "이대로 커밋을 진행할까요?"라고 물어보세요.

### Phase 2: Commit 생성
1. 내가 커밋을 승인하거나 특정 파일만 분리해서 커밋하겠다고 하면, 컨벤션에 맞는 커밋 메시지 초안을 나에게 제시하세요.
2. 승인 시 `git add [해당 파일들]` 및 `git commit -m "[메시지]"`를 실행하세요.

### Phase 3: Pull Request 생성 (Template 적용)
1. 커밋 완료 후, 로컬의 `.github/ISSUE_TEMPLATE/pull_request.md` 파일을 읽어오세요. (파일이 없다면 일반적인 PR 양식을 사용하세요.)
2. 방금 작업한 내용을 바탕으로 해당 템플릿 양식에 맞게 PR 본문(Body) 마크다운을 작성하세요.
3. 쉘 명령어 실행 시 따옴표 충돌 에러를 막기 위해, 작성된 PR 본문을 `.claude/tmp_pr_body.md` 파일로 잠시 저장하세요.
4. `git push origin HEAD` 명령어로 현재 브랜치를 푸시하세요.
5. `gh pr create --base [base_branch] --title "[커밋 메시지 기반 제목]" --body-file .claude/tmp_pr_body.md` 명령어를 실행하여 PR을 생성하세요.
6. 생성된 PR의 URL을 나에게 알려주고, 임시 파일(`.claude/tmp_pr_body.md`)을 삭제한 뒤 작업을 종료하세요.