import json
import os
from typing import List, Optional, Any

REDIS_HOST = os.environ.get("REDIS_HOST", "localhost")
REDIS_PORT = int(os.environ.get("REDIS_PORT", "6379"))

# Try to connect to Redis; fall back to in-memory if unavailable
redis_client = None
try:
    import redis
    _client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0, decode_responses=True)
    _client.ping()
    redis_client = _client
    print(f"[OK] Redis connected at {REDIS_HOST}:{REDIS_PORT}")
except Exception:
    print(f"[WARN] Redis unavailable at {REDIS_HOST}:{REDIS_PORT} -- using in-memory fallback")

# ─── In-memory fallback stores ────────────────────────────────
_mem_carts = {}   # user_id -> [book_id, ...]
_mem_cache = {}   # key -> (value, expiry_timestamp)
_mem_locks = {}   # key -> owner

import time

# ─── Cart ─────────────────────────────────────────────────────

def get_cart(user_id: int) -> List[str]:
    if redis_client:
        key = f"cart:{user_id}"
        return redis_client.lrange(key, 0, -1)
    return list(_mem_carts.get(user_id, []))

def add_to_cart(user_id: int, book_id: str) -> bool:
    if redis_client:
        key = f"cart:{user_id}"
        existing = redis_client.lrange(key, 0, -1)
        if book_id in existing:
            return False
        redis_client.rpush(key, book_id)
        return True
    cart = _mem_carts.setdefault(user_id, [])
    if book_id in cart:
        return False
    cart.append(book_id)
    return True

def remove_from_cart(user_id: int, book_id: str) -> bool:
    if redis_client:
        key = f"cart:{user_id}"
        redis_client.lrem(key, 0, book_id)
        return True
    cart = _mem_carts.get(user_id, [])
    if book_id in cart:
        cart.remove(book_id)
    return True

def clear_cart(user_id: int) -> int:
    if redis_client:
        key = f"cart:{user_id}"
        count = redis_client.llen(key)
        redis_client.delete(key)
        return count
    cart = _mem_carts.pop(user_id, [])
    return len(cart)

# ─── Cache ────────────────────────────────────────────────────

def get_cache(key: str) -> Optional[Any]:
    if redis_client:
        data = redis_client.get(key)
        if data:
            return json.loads(data)
        return None
    entry = _mem_cache.get(key)
    if entry:
        value, expiry = entry
        if expiry is None or time.time() < expiry:
            return value
        del _mem_cache[key]
    return None

def set_cache(key: str, value: Any, ttl: int = 300):
    if redis_client:
        redis_client.setex(key, ttl, json.dumps(value))
        return
    _mem_cache[key] = (value, time.time() + ttl)

def delete_cache(key: str):
    if redis_client:
        redis_client.delete(key)
        return
    _mem_cache.pop(key, None)

# ─── Rate Limiting ────────────────────────────────────────────

def is_rate_limited(key: str, max_requests: int = 10, window: int = 60) -> bool:
    if redis_client:
        count = redis_client.incr(key)
        if count == 1:
            redis_client.expire(key, window)
        return count > max_requests
    # In-memory: skip rate limiting
    return False

# ─── Lua-equivalent atomic operations (in-memory fallback) ───

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
    if redis_client:
        key = f"cart:{user_id}"
        result = redis_client.eval(LUA_ADD_TO_CART, 1, key, book_id)
        return result == 1
    return add_to_cart(user_id, book_id)

def checkout_cart_atomic(user_id: int) -> int:
    if redis_client:
        key = f"cart:{user_id}"
        result = redis_client.eval(LUA_CHECKOUT, 1, key)
        return result
    return clear_cart(user_id)

def acquire_lock_atomic(resource: str, owner: str, timeout: int = 10) -> bool:
    if redis_client:
        key = f"lock:{resource}"
        result = redis_client.eval(LUA_ACQUIRE_LOCK, 1, key, owner, timeout)
        return result == 1
    if resource not in _mem_locks:
        _mem_locks[resource] = owner
        return True
    return False

def release_lock_atomic(resource: str, owner: str) -> bool:
    if redis_client:
        key = f"lock:{resource}"
        result = redis_client.eval(LUA_RELEASE_LOCK, 1, key, owner)
        return result == 1
    if _mem_locks.get(resource) == owner:
        del _mem_locks[resource]
        return True
    return False