package com.coursecraft.adapter.in.error;

import com.coursecraft.domain.error.DomainExceptions.ForbiddenException;
import com.coursecraft.domain.error.DomainExceptions.NotFoundException;
import com.coursecraft.domain.error.DomainExceptions.ValidationException;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Maps domain exceptions thrown by handlers to HTTP status codes, keeping the domain free of any
 * HTTP concerns. Applied as an inner functional filter on the secured routes.
 */
public final class ErrorMapping {

    private ErrorMapping() {
    }

    public static HandlerFilterFunction<ServerResponse, ServerResponse> filter() {
        return (request, next) -> {
            try {
                return next.handle(request);
            } catch (ValidationException | IllegalArgumentException e) {
                return error(400, "bad_request", e.getMessage());
            } catch (ForbiddenException e) {
                return error(403, "forbidden", e.getMessage());
            } catch (NotFoundException | NoSuchElementException e) {
                return error(404, "not_found", e.getMessage());
            }
        };
    }

    private static ServerResponse error(int status, String code, String message) {
        return ServerResponse.status(status).body(Map.of(
                "error", code,
                "message", message == null ? "" : message));
    }
}
