package com.example.clinic.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Global presentation controller advice providing secure, structured HTTP status and template mapping
 * for security violations, invalid URL parameters, missing resources, and unexpected runtime failures (NFR-009, TC-045).
 */
@ControllerAdvice
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        log.warn("Yetkisiz erişim engellendi [uri={}]: {}", safeUri(request), ex.getMessage());
        if (expectsJson(request)) {
            return buildJsonErrorResponse(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz bulunmamaktadır.");
        }
        return buildErrorView(
                response,
                HttpStatus.FORBIDDEN,
                "error/403",
                "403 — Yetkisiz Erişim",
                "Bu işlem için yetkiniz bulunmamaktadır."
        );
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public Object handleNotFound(Exception ex,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        if (expectsJson(request)) {
            return buildJsonErrorResponse(HttpStatus.NOT_FOUND, "İstenen kaynak bulunamadı.");
        }
        return buildErrorView(
                response,
                HttpStatus.NOT_FOUND,
                "error/404",
                "404 — Sayfa Bulunamadı",
                "Aradığınız sayfa, kaynak veya işlem bulunamadı."
        );
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public Object handleBadRequest(Exception ex,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        log.debug("Geçersiz istek parametresi [uri={}]", safeUri(request));
        if (expectsJson(request)) {
            return buildJsonErrorResponse(HttpStatus.BAD_REQUEST, "Geçersiz veya eksik istek parametresi.");
        }
        return buildErrorView(
                response,
                HttpStatus.BAD_REQUEST,
                "error/400",
                "400 — Geçersiz İstek",
                "Gönderilen istek parametreleri geçersiz veya eksik. Lütfen geçerli bir seçim yaparak tekrar deneyiniz."
        );
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpectedException(Exception ex,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        log.error("Beklenmeyen sunucu hatası [uri={}]", safeUri(request), ex);
        if (expectsJson(request)) {
            return buildJsonErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "İşleminiz gerçekleştirilirken beklenmeyen bir teknik hata oluştu."
            );
        }
        return buildErrorView(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "error/500",
                "500 — Beklenmeyen Sistem Hatası",
                "İşleminiz gerçekleştirilirken beklenmeyen bir teknik hata oluştu. Lütfen daha sonra tekrar deneyiniz."
        );
    }

    private ResponseEntity<Map<String, Object>> buildJsonErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "status", status.value(),
                        "error", status.getReasonPhrase(),
                        "message", message
                ));
    }

    private ModelAndView buildErrorView(HttpServletResponse response,
                                      HttpStatus status,
                                      String viewName,
                                      String errorTitle,
                                      String errorMessage) {
        response.setStatus(status.value());
        ModelAndView mav = new ModelAndView(viewName, status);
        mav.addObject("statusCode", status.value());
        mav.addObject("errorTitle", errorTitle);
        mav.addObject("errorMessage", errorMessage);
        return mav;
    }

    private boolean expectsJson(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/patient/slots")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        String xRequestedWith = request.getHeader("X-Requested-With");
        return (accept != null && accept.contains("application/json") && !accept.contains("text/html"))
                || "XMLHttpRequest".equalsIgnoreCase(xRequestedWith);
    }

    private String safeUri(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "-";
    }
}
