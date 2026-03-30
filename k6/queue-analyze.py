#!/usr/bin/env python3
"""
v2 Queue 공정성(Fairness) 분석 스크립트

사용법:
    python3 queue-analyze.py k6_v2_output.txt

분석 지표:
    position  : 서버가 대기열 진입 순서대로 부여한 FIFO 순위 (0-based)
    orderNo   : DB auto-increment — 낮은 번호 = 먼저 처리됨
    wait_ms   : 대기열 진입 ~ ACTIVE 전환까지 걸린 시간

역전(Inversion) 정의:
    position_i < position_j  이지만  order_no_i > order_no_j
    즉, 더 먼저 대기열에 진입한 사람이 더 나중에 주문된 경우

배치 간 역전 vs 배치 내 역전:
    - 배치 간 역전 (batch_i ≠ batch_j): 진짜 공정성 위반.
      서로 다른 배치에 속한 사람끼리 순서가 뒤바뀐 것.
    - 배치 내 역전 (batch_i == batch_j): 허용 범위.
      같은 배치 내에서 동시에 ACTIVE가 되므로 비결정론적 처리 순서가 생길 수 있음.

v1과의 비교:
    - v1 (Lua Script): send_time 순서 vs orderNo 역전율 ~33~49%
    - v2 (Queue)     : position 순서 vs orderNo 배치 간 역전율 ≈ 0%
"""

import re
import sys
from itertools import combinations


# ── 파서 ─────────────────────────────────────────────────────────────────────

def parse_results(filepath: str):
    pattern = re.compile(
        r'\[V2RESULT\]\s+vu=(\d+)\s+position=(\d+)\s+entry_time=(\d+)'
        r'\s+activation_time=(\d+)\s+wait_ms=(\d+)\s+order_no=(\d+)'
    )
    results = []
    errors  = []

    with open(filepath, encoding='utf-8') as f:
        for line in f:
            m = pattern.search(line)
            if m:
                results.append({
                    'vu':              int(m.group(1)),
                    'position':        int(m.group(2)),
                    'entry_time':      int(m.group(3)),
                    'activation_time': int(m.group(4)),
                    'wait_ms':         int(m.group(5)),
                    'order_no':        int(m.group(6)),
                })
            elif '[V2ERROR]' in line:
                errors.append(line.strip())

    return results, errors


# ── 역전 카운터 ───────────────────────────────────────────────────────────────

BATCH_SIZE = 400  # EventQueueWorker 배치 크기와 동일


def batch_of(position: int) -> int:
    return position // BATCH_SIZE


def count_all_inversions(data: list) -> tuple:
    """전체 역전 쌍 수 (O(n²))"""
    total_pairs = len(data) * (len(data) - 1) // 2
    inversions  = 0
    examples    = []

    for i in range(len(data)):
        for j in range(i + 1, len(data)):
            if data[i]['order_no'] > data[j]['order_no']:
                inversions += 1
                if len(examples) < 5:
                    examples.append((data[i], data[j]))

    return inversions, total_pairs, examples


def count_cross_batch_inversions(data: list) -> tuple:
    """배치 경계를 넘는 역전만 카운트 (진짜 공정성 위반)"""
    total_pairs = len(data) * (len(data) - 1) // 2
    cross       = 0
    examples    = []

    for i in range(len(data)):
        for j in range(i + 1, len(data)):
            bi = batch_of(data[i]['position'])
            bj = batch_of(data[j]['position'])
            if bi < bj and data[i]['order_no'] > data[j]['order_no']:
                cross += 1
                if len(examples) < 3:
                    examples.append((data[i], data[j]))

    return cross, total_pairs, examples


# ── 유틸 ─────────────────────────────────────────────────────────────────────

def sep(char='=', width=68):
    print(char * width)


def percentile(values: list, p: float) -> int:
    s = sorted(values)
    return s[max(0, int(len(s) * p) - 1)]


# ── 메인 ─────────────────────────────────────────────────────────────────────

def main():
    if len(sys.argv) < 2:
        print("사용법: python3 queue-analyze.py <k6_v2_output.txt>")
        sys.exit(1)

    results, errors = parse_results(sys.argv[1])

    sep()
    print("  v2 Queue 공정성(Fairness) 분석 리포트")
    sep()

    if not results:
        print("❌ [V2RESULT] 줄을 찾을 수 없습니다.")
        print("   queue-fairness-test.js 출력 파일이 올바른지 확인하세요.")
        print("   명령어: k6 run queue-fairness-test.js > k6_v2_output.txt 2>&1")
        sys.exit(1)

    if errors:
        print(f"\n⚠️  에러 요청 {len(errors)}건 발생 (분석에서 제외):")
        for e in errors[:5]:
            print(f"   {e[:120]}")

    # position 기준 정렬 = 대기열 FIFO 순서
    by_position = sorted(results, key=lambda r: r['position'])

    positions   = [r['position']   for r in by_position]
    order_nos   = [r['order_no']   for r in by_position]
    wait_mss    = [r['wait_ms']    for r in by_position]

    # ── 기본 통계 ─────────────────────────────────────────────────────────────
    print(f"\n[기본 통계]")
    print(f"  성공 요청 수     : {len(results)}건  (에러: {len(errors)}건)")
    print(f"  position 범위    : {min(positions)} ~ {max(positions)}")
    print(f"  orderNo 범위     : {min(order_nos)} ~ {max(order_nos)}")
    print(f"  wait_ms (ACTIVE 대기)")
    print(f"    p50  : {percentile(wait_mss, 0.50):,}ms")
    print(f"    p90  : {percentile(wait_mss, 0.90):,}ms")
    print(f"    p99  : {percentile(wait_mss, 0.99):,}ms")
    print(f"    max  : {max(wait_mss):,}ms")

    # ── 역전 분석 ─────────────────────────────────────────────────────────────
    all_inv, total_pairs, all_examples       = count_all_inversions(by_position)
    cross_inv, _, cross_examples             = count_cross_batch_inversions(by_position)
    within_inv                               = all_inv - cross_inv

    all_rate   = all_inv   / total_pairs * 100 if total_pairs > 0 else 0.0
    cross_rate = cross_inv / total_pairs * 100 if total_pairs > 0 else 0.0

    print(f"\n[공정성 분석]")
    print(f"  전체 역전         : {all_inv:,} / {total_pairs:,}쌍  ({all_rate:.1f}%)")
    print(f"  ├─ 배치 간 역전  : {cross_inv:,}쌍  ({cross_rate:.1f}%)  ← 진짜 공정성 위반")
    print(f"  └─ 배치 내 역전  : {within_inv:,}쌍              ← 허용 (동일 배치 동시 활성화)")

    if cross_inv == 0:
        verdict = "✅  FIFO 공정성 보장 — 배치 간 역전 없음"
    elif cross_rate < 5:
        verdict = f"⚠️  경미한 배치 간 공정성 위반 ({cross_rate:.1f}%)"
    else:
        verdict = f"❌  배치 간 공정성 위반 ({cross_rate:.1f}%)"

    print(f"\n  판정  : {verdict}")

    # ── 배치 간 역전 사례 ─────────────────────────────────────────────────────
    if cross_examples:
        print(f"\n[배치 간 역전 사례 (상위 3개)]")
        for ri, rj in cross_examples:
            bi = batch_of(ri['position']) + 1
            bj = batch_of(rj['position']) + 1
            print(
                f"  position={ri['position']:>3} (배치{bi}) → orderNo={ri['order_no']}  "
                f"| position={rj['position']:>3} (배치{bj}) → orderNo={rj['order_no']}"
                f"  ← ⚠️ 역전"
            )

    # ── 배치별 처리 현황 ──────────────────────────────────────────────────────
    print(f"\n[배치별 처리 현황 (워커 배치 크기={BATCH_SIZE})]")
    print(f"  {'배치':>4}  {'position':>10}  {'ACTIVE대기 avg':>15}  {'orderNo 범위':>14}")
    print("  " + "-" * 52)

    batch_map: dict = {}
    for r in by_position:
        b = batch_of(r['position'])
        batch_map.setdefault(b, []).append(r)

    for b_idx in sorted(batch_map.keys()):
        items      = batch_map[b_idx]
        pos_range  = f"{min(r['position'] for r in items)}~{max(r['position'] for r in items)}"
        wait_avg   = sum(r['wait_ms'] for r in items) // len(items)
        ono_range  = f"{min(r['order_no'] for r in items)}~{max(r['order_no'] for r in items)}"
        print(f"  {b_idx+1:>4}배치  {pos_range:>10}  {wait_avg:>12}ms  {ono_range:>14}")

    # ── 전체 순서표 ───────────────────────────────────────────────────────────
    print(f"\n[대기열 순서(position) vs DB 처리 순서(orderNo)]")
    print(f"  {'position':>8}  {'배치':>4}  {'VU':>4}  {'wait_ms':>8}  {'orderNo':>9}")
    print("  " + "-" * 46)

    prev_ono = -1
    for r in by_position:
        b    = batch_of(r['position']) + 1
        flag = ""
        if prev_ono != -1 and r['order_no'] < prev_ono:
            bi_prev = batch_of(by_position[by_position.index(r) - 1]['position']) + 1
            if bi_prev != b:
                flag = "  ⚠️ 역전(배치 간)"
            else:
                flag = "  (배치 내)"
        print(
            f"  {r['position']:>8}  {b:>4}배치  VU{r['vu']:>3}  "
            f"{r['wait_ms']:>6}ms  {r['order_no']:>9}{flag}"
        )
        prev_ono = r['order_no']

    # ── v1 vs v2 비교 ─────────────────────────────────────────────────────────
    print()
    sep()
    print("  v1(Lua Script) vs v2(Queue) 공정성 비교")
    sep()
    print(f"  {'구분':<10}  {'역전 기준':<24}  {'역전율':<10}  판정")
    print("  " + "-" * 58)
    print(f"  {'v1':<10}  {'send_time → orderNo':<24}  {'~33~49%':<10}  ❌ 공정성 미보장")
    print(f"  {'v2':<10}  {'position → orderNo (배치 간)':<24}  {cross_rate:.1f}%{'':>7}  ", end="")
    print("✅ 보장" if cross_inv == 0 else f"⚠️ {cross_rate:.1f}% 위반")
    print()
    print("  v1 한계:")
    print("    Tomcat 스레드 풀의 비결정론적 스케줄링으로 인해")
    print("    HTTP 도달 순서 ≠ Redis Lua Script 실행 순서")
    print()
    print("  v2 개선:")
    print(f"    Redis Sorted Set FIFO → 배치 크기({BATCH_SIZE}) 단위로 공정성 보장")
    print(f"    배치 내 비결정론은 {within_inv}쌍 존재하나 동일 활성화 타이밍으로 허용")
    sep()


if __name__ == '__main__':
    main()
