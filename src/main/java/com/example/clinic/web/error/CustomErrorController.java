package com.example.clinic.web.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Production-level custom error controller satisfying NFR-009 and TC-045.
 * Strictly prevents stack trace, SQL statement, and internal exception leakage while preserving
 * exact HTTP status codes (403 Forbidden, 404 Not Found, 500 Internal Server Error).
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpServletResponse response, Model model) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        if (statusObj != null) {
            try {
                statusCode = Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        response.setStatus(statusCode);
        model.addAttribute("statusCode", statusCode);

        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            model.addAttribute("errorTitle", "403 — Yetkisiz Erişim");
            model.addAttribute("errorMessage", "Bu işlem için yetkiniz bulunmamaktadır.");
            return "error/403";
        }

        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            model.addAttribute("errorTitle", "404 — Sayfa Bulunamadı");
            model.addAttribute("errorMessage", "Aradığınız sayfa, kaynak veya işlem bulunamadı.");
            return "error/404";
        }

        model.addAttribute("errorTitle", "500 — Beklenmeyen Sistem Hatası");
        model.addAttribute("errorMessage", "İşleminiz gerçekleştirilirken beklenmeyen bir teknik hata oluştu. Lütfen daha sonra tekrar deneyiniz.");
        return "error/500";
    }
}
