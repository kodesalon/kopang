-- 대기열 앞쪽 토큰을 꺼내 활성 Set 에 넣는 일을 한 번에 실행한다.
-- 두 명령 사이에 틈이 있으면, 그 사이 상태 조회는 토큰을 어디에서도 찾지 못해 EXPIRED 를 돌려준다.
-- KEYS[1]: queue:event:{eventId} (ZSet), KEYS[2]: queue:active:{eventId} (Set), KEYS[3]: queue:active_events (Set)
-- ARGV[1]: 꺼낼 개수, ARGV[2]: 활성 Set TTL(초), ARGV[3]: eventId
local popped = redis.call('ZPOPMIN', KEYS[1], ARGV[1])
if #popped > 0 then
    local tokens = {}
    for i = 1, #popped, 2 do
        tokens[#tokens + 1] = popped[i]
    end
    redis.call('SADD', KEYS[2], unpack(tokens))
    redis.call('EXPIRE', KEYS[2], ARGV[2])
end

-- 대기열이 비었으면 활성 이벤트 목록에서 뺀다.
-- 같은 스크립트 안에서 확인해야, 비었다고 본 뒤 들어온 진입을 목록에서 지워 버리는 일이 없다.
if redis.call('ZCARD', KEYS[1]) == 0 then
    redis.call('SREM', KEYS[3], ARGV[3])
end

return popped
