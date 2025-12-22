-- KEYS[1]: 재고 Key
-- ARGV[1]: 차감할 수량

local current_stock = redis.call('get', KEYS[1])
local current_stock_val = tonumber(current_stock)

-- 재고가 없거나 0보다 작거나 같으면 실패(-1) 반환
if not current_stock or current_stock_val <= 0 then
    return -1
end

if current_stock_val - tonumber(ARGV[1]) < 0 then
    return -1
end

-- 재고가 있으면 차감하고 남은 재고 반환
return redis.call('DECRBY', KEYS[1], ARGV[1])