/**
 * Idempotency key utilities
 * Prevents duplicate requests for the same operation
 */

const IDEMPOTENCY_STORE = 'idempotencyKeys';
const MAX_AGE = 24 * 60 * 60 * 1000; // 24 hours

/**
 * Generate an idempotency key for a request
 */
export function generateIdempotencyKey(operation, params) {
  const key = `${operation}_${JSON.stringify(params)}`;
  return btoa(key).replace(/[^a-zA-Z0-9]/g, '').substring(0, 32);
}

/**
 * Store idempotency key in memory (for current session)
 */
const idempotencyCache = new Map();

/**
 * Check if an idempotency key exists (prevent duplicate requests)
 */
export function isIdempotent(key) {
  if (idempotencyCache.has(key)) {
    const { timestamp } = idempotencyCache.get(key);
    // Remove old entries
    if (Date.now() - timestamp > MAX_AGE) {
      idempotencyCache.delete(key);
      return false;
    }
    return true;
  }
  return false;
}

/**
 * Mark an idempotency key as used
 */
export function markIdempotent(key, response) {
  idempotencyCache.set(key, {
    timestamp: Date.now(),
    response,
  });
  
  // Cleanup old entries periodically
  if (idempotencyCache.size > 1000) {
    const now = Date.now();
    for (const [k, v] of idempotencyCache.entries()) {
      if (now - v.timestamp > MAX_AGE) {
        idempotencyCache.delete(k);
      }
    }
  }
}

/**
 * Get cached response for idempotency key
 */
export function getIdempotentResponse(key) {
  const entry = idempotencyCache.get(key);
  if (entry && Date.now() - entry.timestamp < MAX_AGE) {
    return entry.response;
  }
  return null;
}


