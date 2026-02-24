-- KEYS[1]: 중복 요청 방지 키
-- ARGV[1]: TTL (초)

local exists = redis.call('EXISTS', KEYS[1])

if exists == 1 then
    return 0
end

redis.call('SET', KEYS[1], '1', 'EX', ARGV[1])
return 1