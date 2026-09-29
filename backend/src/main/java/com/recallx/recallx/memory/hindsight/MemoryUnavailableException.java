package com.recallx.recallx.memory.hindsight;

/**
 * Thrown when a Hindsight call fails: bad key (401), out of credits (402), unknown bank (404), rate limit (429),
 * server error (5xx) or timeout. The API turns it into HTTP 503 "memory_unavailable"; it is never replaced by a
 * memory-free answer.
 *
 * @see #status() the HTTP status Hindsight returned, or 0 for a timeout or connection failure
 */
public class MemoryUnavailableException extends RuntimeException {

    private final int status;

    public MemoryUnavailableException(String message, Throwable cause) {
        this(message, 0, cause);
    }

    public MemoryUnavailableException(String message, int status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
