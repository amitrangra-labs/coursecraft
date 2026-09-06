package com.coursecraft.domain.error;

/**
 * Domain-level failures the inbound layer maps to HTTP status codes. Keeping them here lets the
 * domain stay framework-free while still signalling intent (the adapter does the HTTP mapping).
 */
public final class DomainExceptions {

    private DomainExceptions() {
    }

    /** Requested entity does not exist → 404. */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** Caller is authenticated but not allowed to perform the action → 403. */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }

    /** Input failed a business rule → 400. */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
