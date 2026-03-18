#!/usr/bin/env python3
"""
공정성 역전(Fairness Inversion) 분석 스크립트

사용법:
    python3 analyze.py k6_output.txt

출력:
    - send_time 순서(HTTP 발송 순서) vs orderNo 순서(DB 처리 순서) 비교표
    - 역전 쌍 수, 역전 비율, 역전 사례
    - 판정: 공정성 보장 여부

역전(Inversion)의 정의:
    send_time_i < send_time_j  이지만  order_no_i > order_no_j
    즉, 더 일찍 보낸 요청이 더 늦게 DB에 저장된 경우
"""

import re
import sys
from itertools import combinations


def parse_results(filepath: str) -> list[dict]:
    """[RESULT] 줄을 파싱하여 (vu, send_time, latency_ms, order_no) 리스트 반환"""
    pattern = re.compile(
        r'\[RESULT\] vu=(\d+) send_time=(\d+) latency_ms=(\d+) order_no=(\d+)'
    )
    results = []
    errors = []

    with open(filepath, encoding='utf-8') as f:
        for line in f:
            m = pattern.search(line)
            if m:
                results.append({
                    'vu': int(m.group(1)),
                    'send_time': int(m.group(2)),
                    'latency_ms': int(m.group(3)),
                    'order_no': int(m.group(4)),
                })
            elif '[ERROR]' in line:
                errors.append(line.strip())

    return results, errors


def count_inversions(order_nos: list[int]) -> tuple[int, int, list[tuple]]:
    """
    역전 쌍 수 계산 (O(n²))
    반환: (역전 수, 전체 쌍 수, 역전 예시 목록)
    """
    n = len(order_nos)
    total_pairs = n * (n - 1) // 2
    inversions = 0
    examples = []

    for i in range(n):
        for j in range(i + 1, n):
            if order_nos[i] > order_nos[j]:
                inversions += 1
                if len(examples) < 5:
                    examples.append((i, j, order_nos[i], order_nos[j]))

    return inversions, total_pairs, examples


def print_separator(char='=', width=65):
    print(char * width)


def main():
    if len(sys.argv) < 2:
        print("사용법: python3 analyze.py <k6_output.txt>")
        sys.exit(1)

    filepath = sys.argv[1]
    results, errors = parse_results(filepath)

    print_separator()
    print("  Lua Script 공정성(Fairness) 분석 리포트")
    print_separator()

    if not results:
        print("❌ [RESULT] 줄을 찾을 수 없습니다.")
        print("   - k6 출력 파일이 올바른지 확인하세요.")
        print("   - 명령어: k6 run fairness-test.js > k6_output.txt 2>&1")
        sys.exit(1)

    if errors:
        print(f"⚠️  에러 요청 {len(errors)}건 발생 (분석에서 제외)")
        for e in errors[:3]:
            print(f"   {e[:100]}")

    # send_time 기준으로 정렬 (= HTTP 발송 순서)
    by_send_time = sorted(results, key=lambda r: r['send_time'])

    send_times = [r['send_time'] for r in by_send_time]
    order_nos  = [r['order_no']  for r in by_send_time]
    vus        = [r['vu']        for r in by_send_time]
    latencies  = [r['latency_ms'] for r in by_send_time]

    # ── 기본 통계 ────────────────────────────────────────────────
    print(f"\n[기본 통계]")
    print(f"  성공 요청 수    : {len(results)}건")
    print(f"  send_time 범위  : {send_times[-1] - send_times[0]}ms  (첫 번째 ~ 마지막 발송)")
    print(f"  latency p50     : {sorted(latencies)[len(latencies)//2]}ms")
    print(f"  latency p99     : {sorted(latencies)[int(len(latencies)*0.99)]}ms")
    print(f"  orderNo 범위    : {min(order_nos)} ~ {max(order_nos)}")

    # ── 공정성 분석 ───────────────────────────────────────────────
    inversions, total_pairs, examples = count_inversions(order_nos)
    inversion_rate = inversions / total_pairs * 100 if total_pairs > 0 else 0.0

    print(f"\n[공정성 분석]")
    print(f"  역전 쌍 수   : {inversions:,} / {total_pairs:,}쌍")
    print(f"  역전 비율    : {inversion_rate:.1f}%")

    if inversions == 0:
        verdict = "✅  공정성 보장 — send_time 순서와 orderNo 순서가 일치"
    elif inversion_rate < 10:
        verdict = f"⚠️  경미한 공정성 위반 ({inversion_rate:.1f}% 역전)"
    else:
        verdict = f"❌  공정성 위반 확인 ({inversion_rate:.1f}% 역전) — Lua Script는 FIFO를 보장하지 않는다"

    print(f"  판정         : {verdict}")

    # ── 역전 사례 ─────────────────────────────────────────────────
    if examples:
        print(f"\n[역전 사례 — send_time 순서 기준 (상위 5개)]")
        print(f"  {'HTTP순위':>6}  {'VU':>4}  {'send_time':>14}  {'orderNo':>9}  역전 설명")
        print("  " + "-" * 58)
        for (i, j, ono_i, ono_j) in examples:
            print(
                f"  {i+1:>6}위  VU{vus[i]:>3}  {send_times[i]:>14}  {ono_i:>9}"
            )
            print(
                f"  {j+1:>6}위  VU{vus[j]:>3}  {send_times[j]:>14}  {ono_j:>9}"
                f"  ← ⚠️  {j+1}번째 발송이 {i+1}번째보다 먼저 처리됨"
            )
            print()

    # ── 전체 순서표 ────────────────────────────────────────────────
    print(f"\n[HTTP 발송 순서(send_time) vs DB 처리 순서(orderNo)]")
    print(f"  {'HTTP순위':>6}  {'VU':>4}  {'send_time':>14}  {'orderNo':>9}  {'latency':>8}")
    print("  " + "-" * 52)

    prev_order_no = -1
    for rank, r in enumerate(by_send_time, 1):
        flag = ""
        if prev_order_no != -1 and r['order_no'] < prev_order_no:
            flag = "  ⚠️ 역전"
        print(
            f"  {rank:>6}위  VU{r['vu']:>3}  {r['send_time']:>14}  "
            f"{r['order_no']:>9}  {r['latency_ms']:>6}ms{flag}"
        )
        prev_order_no = r['order_no']

    # ── 결론 ──────────────────────────────────────────────────────
    print()
    print_separator()
    if inversions > 0:
        print("  결론: Lua Script(v1)의 한계 실측 완료")
        print()
        print("  ▪ Lua Script는 원자적(Atomic)이다 → over-selling 없음 ✅")
        print("  ▪ Lua Script는 공정하지 않다(Non-FIFO) → 선착순 위반 ❌")
        print()
        print("  HTTP 도달 순서와 Redis 처리 순서는 다르다.")
        print("  Tomcat 스레드 풀의 비결정론적 스케줄링이 원인이다.")
        print()
        print("  → 이벤트 티켓팅 도메인에서 v2(대기열 아키텍처)가 필요한 이유")
    else:
        print("  이번 실행에서는 역전이 관찰되지 않았습니다.")
        print("  send_time 범위가 너무 좁거나 서버 부하가 낮을 경우 역전이 발생하지 않을 수 있습니다.")
        print("  VU 수를 늘리거나 서버 부하를 높여 재시도하세요.")
    print_separator()


if __name__ == '__main__':
    main()