package com.example.clinic.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller advice providing secure exception mapping for presentation tier (NFR-009).
 */
@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("statusCode", 403);
        model.addAttribute("errorTitle", "403 — Yetkisiz Erişim");
        model.addAttribute("errorMessage", "Bu işlem için yetkiniz bulunmamaktadır.");
        return "error/403";
    }
}
