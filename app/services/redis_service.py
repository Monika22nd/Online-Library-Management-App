import redis
import json
import os
from typing import List, Optional, Any

REDIS_HOST = os.environ.get("REDIS_HOST", "localhost")
REDIS_PORT = int(os.environ.get("REDIS_PORT", "6379"))

redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0, decode_responses=True)

def get_cart(user_id: int) -> List[str]:
    key = f"cart:{user_id}"
    return redis_client.lrange(key, 0, -1)

def add_to_cart(user_id: int, book_id: str) -> bool:
    key = f"cart:{user_id}"
    existing = redis_client.lrange(key, 0, -1)
    if book_id in existing:
        return False
    redis_client.rpush(key, book_id)
    return True

def remove_from_cart(user_id: int, book_id: str) -> bool:
    key = f"cart:{user_id}"
    redis_client.lrem(key, 0, book_id)
    return True

def clear_cart(user_id: int) -> int:
    key = f"cart:{user_id}"
    count = redis_client.llen(key)
    redis_client.delete(key)
    return count

def get_cart_count(user_id: int) -> int:
    key = f"cart:{user_id}"
    return redis_client.llen(key)

def set_cache(key: str, value: Any, ttl: int = 3600):
    if isinstance(value, (dict, list)):
        value = json.dumps(value)
    redis_client.setex(key, ttl, value)

def get_cache(key: str) -> Optional[str]:
    value = redis_client.get(key)
    if value:
        try:
            return json.loads(value)
        except:
            return value
    return None

def delete_cache(key: str):
    redis_client.delete(key)

def acquire_lock(resource: str, owner: str, timeout: int = 10) -> bool:
    key = f"lock:{resource}"
    return redis_client.set(key, owner, nx=True, ex=timeout)

def release_lock(resource: str, owner: str) -> bool:
    key = f"lock:{resource}"
    current = redis_client.get(key)
    if current == owner:
        redis_client.delete(key)
        return True
    return False

def check_rate_limit(endpoint: str, user_id: int, max_requests: int = 100, window: int = 3600) -> bool:
    key = f"rate:{endpoint}:{user_id}"
    count = redis_client.incr(key)
    if count == 1:
        redis_client.expire(key, window)
    return count > max_requests

LUA_ADD_TO_CART = """
local items = redis.call('LRANGE', KEYS[1], 0, -1)
for i, v in ipairs(items) do
    if v == ARGV[1] then return 0 end
end
redis.call('RPUSH', KEYS[1], ARGV[1])
return 1
"""

LUA_CHECKOUT = """
local count = redis.call('LLEN', KEYS[1])
redis.call('DEL', KEYS[1])
return count
"""

LUA_ACQUIRE_LOCK = """
local result = redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX')
if result then return 1 else return 0 end
"""

LUA_RELEASE_LOCK = """
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 1
end
return 0
"""

def add_to_cart_atomic(user_id: int, book_id: str) -> bool:
    key = f"cart:{user_id}"
    result = redis_client.eval(LUA_ADD_TO_CART, 1, key, book_id)
    return result == 1

def checkout_cart_atomic(user_id: int) -> int:
    key = f"cart:{user_id}"
    result = redis_client.eval(LUA_CHECKOUT, 1, key)
    return result

def acquire_lock_atomic(resource: str, owner: str, timeout: int = 10) -> bool:
    key = f"lock:{resource}"
    result = redis_client.eval(LUA_ACQUIRE_LOCK, 1, key, owner, timeout)
    return result == 1

def release_lock_atomic(resource: str, owner: str) -> bool:
    key = f"lock:{resource}"
    result = redis_client.eval(LUA_RELEASE_LOCK, 1, key, owner)
    return result == 1