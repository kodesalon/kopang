#!/usr/bin/env python3
"""
v1(Lua Script)과 v2(대기열) 공정성을 같은 기준으로 비교하는 분석 스크립트

사용법:
    python3 fairness-report.py <k6_output.txt> [<k6_output.txt> ...]

k6 출력에서 [RESULT](v1) 또는 [V2RESULT](v2) 줄을 읽어 아래 지표를 계산한다.

    send_time → orderNo  (v1·v2 공통, 끝에서 끝까지)
        보낸 시각이 엄격히 빠른 요청이 더 큰 주문 번호를 받은 쌍의 비율.
        v2의 보낸 시각은 대기열 진입 요청을 보낸 시각(entry_time)이다.
    send_time → position (v2만)
        보낸 시각이 엄격히 빠른 요청이 더 뒤 순번을 받은 쌍의 비율. 대기열 진입이 공정한지 본다.
    position → orderNo, 배치 간 (v2만)
        서로 다른 배치에서 앞 순번이 더 큰 주문 번호를 받은 쌍의 수. queue-analyze.py와 같은 기준이다.

비율의 분모는 전체 쌍(n × (n − 1) / 2)이다. 같은 ms에 보낸 쌍(동률)은 순서를 판단할 수 없으므로
역전으로 세지 않고, 동률 쌍 비율을 따로 적는다.
"""

import re
import sys
from itertools import combinations

BATCH_SIZE = 400  # EventQueueWorker 배치 크기와 같게 둔다

V1 = re.compile(r'\[RESULT\] vu=(\d+) send_time=(\d+) latency_ms=(\d+) order_no=(\d+)')
V2 = re.compile(
    r'\[V2RESULT\]\s+vu=(\d+)\s+position=(\d+)\s+entry_time=(\d+)'
    r'\s+activation_time=(\d+)\s+wait_ms=(\d+)\s+order_no=(\d+)'
)


def parse(path):
    rows, kind, errors = [], None, 0
    with open(path, encoding='utf-8') as f:
        for line in f:
            m = V1.search(line)
            if m:
                kind = 'v1'
                rows.append({'send': int(m.group(2)), 'order': int(m.group(4))})
                continue
            m = V2.search(line)
            if m:
                kind = 'v2'
                rows.append({'send': int(m.group(3)), 'position': int(m.group(2)), 'order': int(m.group(6))})
                continue
            if '[ERROR]' in line or '[V2ERROR]' in line:
                errors += 1
    return kind, rows, errors


def strict_inversions(rows, first, second):
    """first가 엄격히 앞선 쌍 중 second가 뒤집힌 쌍 수, 그리고 first가 같은 쌍 수."""
    inv = ties = 0
    for a, b in combinations(rows, 2):
        if a[first] == b[first]:
            ties += 1
            continue
        early, late = (a, b) if a[first] < b[first] else (b, a)
        if early[second] > late[second]:
            inv += 1
    return inv, ties


def cross_batch_inversions(rows):
    inv = 0
    for a, b in combinations(rows, 2):
        if a['position'] // BATCH_SIZE == b['position'] // BATCH_SIZE:
            continue
        early, late = (a, b) if a['position'] < b['position'] else (b, a)
        if early['order'] > late['order']:
            inv += 1
    return inv


def pct(x, total):
    return f"{x / total * 100:.1f}%" if total else "-"


def report(path):
    kind, rows, errors = parse(path)
    n = len(rows)
    pairs = n * (n - 1) // 2
    print(f"== {path}")
    print(f"   종류 {kind}, 성공 {n}건, 에러 {errors}건, 전체 쌍 {pairs:,}")
    if n < 2:
        return None
    span = max(r['send'] for r in rows) - min(r['send'] for r in rows)
    print(f"   보낸 시각 범위 {span}ms")
    inv, ties = strict_inversions(rows, 'send', 'order')
    print(f"   send_time → orderNo  역전 {inv:,}쌍 ({pct(inv, pairs)}), 동률 {ties:,}쌍 ({pct(ties, pairs)})")
    result = {'kind': kind, 'n': n, 'send_order': inv / pairs, 'ties': ties / pairs}
    if kind == 'v2':
        inv_p, ties_p = strict_inversions(rows, 'send', 'position')
        cross = cross_batch_inversions(rows)
        print(f"   send_time → position 역전 {inv_p:,}쌍 ({pct(inv_p, pairs)}), 동률 {ties_p:,}쌍")
        print(f"   position → orderNo 배치 간 역전 {cross:,}쌍")
        result.update({'send_position': inv_p / pairs, 'cross_batch': cross})
    return result


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    results = [r for r in (report(p) for p in sys.argv[1:]) if r]
    if len(results) > 1:
        print("== 요약")
        for kind in ('v1', 'v2'):
            group = [r for r in results if r['kind'] == kind]
            if not group:
                continue
            for key in ('send_order', 'send_position'):
                vals = [r[key] for r in group if key in r]
                if vals:
                    print(f"   {kind} {key}: 최소 {min(vals)*100:.1f}%, 최대 {max(vals)*100:.1f}% ({len(vals)}회)")


if __name__ == '__main__':
    main()
