# Error Handling Rules

## 예외 발생 위치
| 레이어 | 예외 | 비고 |
|:---|:---|:---|
| domain | `IllegalStateException` / `IllegalArgumentException` | 비즈니스 규칙 위반 / 유효하지 않은 값 |
| service | Custom Exception (static factory) | 엔티티 없음, 품절, 결제 실패 등 |
| orchestrator | 보상 후 `throw e`로 재전파 | 예외를 직접 생성하지 않고 전파만 하세요 |
| api | `GlobalExceptionController` | HTTP 상태 코드 변환 |

## Custom Exception 패턴
- `service/exception/` 패키지에 생성하세요. `RuntimeException`을 상속하세요.
- static factory를 사용하세요: `NotFoundException.order(orderNo)` 형태.
- **새 예외 추가 시 `GlobalExceptionController`에 `@ExceptionHandler` 핸들러를 추가하세요.**

## 보상(Compensation) 패턴
- 보상 후 `throw e`로 원본 예외를 재전파하세요:
  ```java
  } catch (Exception e) { stockReservationService.increase(...); throw e; }
  ```
- **Scheduler 예외:** 개별 항목 실패는 허용됩니다. `log.warn`으로 기록한 뒤 다음 사이클을 진행하세요.

## 로깅 기준
| 상황 | 레벨 |
|:---|:---|
| 알 수 없는 외부 상태 (PG 미지원 상태 등) | `log.error` |
| 재시도 예정인 실패 (배치/스케줄러) | `log.warn` |
| 정상 비즈니스 예외 (400, 404 등) | 로그 없음 |
| 예상치 못한 서버 오류 | `log.error` |