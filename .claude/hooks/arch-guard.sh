#!/bin/bash
# arch-guard.sh — Kopang 아키텍처 규칙 실시간 감시
# PostToolUse(Write|Edit) 시 stdin으로 JSON을 수신하여 실행

INPUT=$(cat)

# tool_input.file_path 추출 (jq 우선, fallback: python3)
if command -v jq &>/dev/null; then
  FILE=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')
else
  FILE=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null)
fi

[ -z "$FILE" ] && exit 0
[ ! -f "$FILE" ] && exit 0

# Java 파일만 검사
if ! echo "$FILE" | grep -q "\.java$"; then
  exit 0
fi

VIOLATIONS=()

# Rule 1: domain/ — Spring/JPA 어노테이션 사용 금지
if echo "$FILE" | grep -q "/domain/"; then
  if grep -qE "@Entity|@Table|@Service|@Component|@Transactional|@Repository" "$FILE"; then
    VIOLATIONS+=("[ARCH] domain에 Spring/JPA 어노테이션 감지 → $(basename $FILE)")
    VIOLATIONS+=("       허용: Lombok, Java 표준(@Override)만. Spring/JPA는 storage/infra/api 레이어에서만 사용하세요.")
  fi
fi

# Rule 2: Orchestrator — @Transactional 금지
if echo "$FILE" | grep -qE "Orchestrator\.java$"; then
  if grep -q "@Transactional" "$FILE"; then
    VIOLATIONS+=("[ARCH] Orchestrator에 @Transactional 감지 → $(basename $FILE)")
    VIOLATIONS+=("       @Transactional은 Service 레이어에 위임하세요. Orchestrator에는 @Component만 사용하세요.")
  fi
fi

# Rule 3: storage/ 외부 — @Entity 금지
if ! echo "$FILE" | grep -q "/storage/"; then
  if grep -qE "^\s*@Entity\b" "$FILE"; then
    VIOLATIONS+=("[ARCH] storage 외부에 @Entity 감지 → $(basename $FILE)")
    VIOLATIONS+=("       JPA Entity는 storage 패키지 내부에서만 사용하세요.")
  fi
fi

# Rule 4: storage/ JpaEntity — toDomain() 없이 사용 금지
if echo "$FILE" | grep -qE "JpaEntity\.java$"; then
  if ! grep -q "toDomain()" "$FILE"; then
    VIOLATIONS+=("[ARCH] JpaEntity에 toDomain() 메서드 누락 → $(basename $FILE)")
    VIOLATIONS+=("       storage Entity는 반드시 toDomain()으로 domain 객체를 반환하세요.")
  fi
fi

# Rule 5: service/ — @Transactional 누락 감지 (Redis 전용 Service 제외)
# 파일명이 아닌 파일 내용으로 Redis 의존성 여부를 판별한다
if echo "$FILE" | grep -q "/service/"; then
  IS_REDIS_SERVICE=false
  if grep -qE "RedisTemplate|StringRedisTemplate|ReactiveRedisTemplate|LettuceConnectionFactory" "$FILE"; then
    IS_REDIS_SERVICE=true
  fi
  if ! echo "$FILE" | grep -qiE "Orchestrator"; then
    if [ "$IS_REDIS_SERVICE" = "false" ]; then
      if grep -q "public " "$FILE" && ! grep -q "@Transactional" "$FILE"; then
        VIOLATIONS+=("[ARCH] Service public 메서드에 @Transactional 누락 의심 → $(basename $FILE)")
        VIOLATIONS+=("       DB 작업 Service의 모든 public 메서드에 @Transactional을 추가하세요.")
      fi
    fi
  fi
fi

# Rule 6: GlobalExceptionController — 새 Custom Exception 등록 확인
# find로 프로젝트 루트 기준 GlobalExceptionController.java를 탐색해 경로 의존 없이 확인한다
if echo "$FILE" | grep -q "/exception/"; then
  if grep -q "extends RuntimeException" "$FILE"; then
    EXCEPTION_NAME=$(grep -oE "class \w+Exception" "$FILE" | awk '{print $2}')
    PROJECT_ROOT=$(git -C "$(dirname "$FILE")" rev-parse --show-toplevel 2>/dev/null)
    if [ -n "$PROJECT_ROOT" ]; then
      CONTROLLER=$(find "$PROJECT_ROOT" -name "GlobalExceptionController.java" -type f 2>/dev/null | head -1)
    fi
    if [ -n "$CONTROLLER" ] && [ -f "$CONTROLLER" ]; then
      if ! grep -q "$EXCEPTION_NAME" "$CONTROLLER"; then
        VIOLATIONS+=("[ARCH] 새 Exception '$EXCEPTION_NAME'이 GlobalExceptionController에 미등록 → $(basename $FILE)")
        VIOLATIONS+=("       GlobalExceptionController에 @ExceptionHandler를 추가하세요.")
      fi
    fi
  fi
fi

if [ ${#VIOLATIONS[@]} -gt 0 ]; then
  echo ""
  echo "========== 아키텍처 위반 감지 =========="
  printf '%s\n' "${VIOLATIONS[@]}"
  echo "========================================"
  echo "위 항목을 수정한 뒤 다음 작업을 진행하세요."
  exit 1
fi