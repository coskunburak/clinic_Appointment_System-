package com.example.clinic.identity.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller managing user authentication entry points and minimal protected dashboard placeholders
 * for S04 verification (WP-S04-04, Phase 8). Full UI design is scheduled for S08.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "denied", required = false) String denied,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Geçersiz e-posta veya şifre.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Oturumunuz başarıyla kapatıldı.");
        }
        if (denied != null) {
            model.addAttribute("errorMessage", "Bu sayfaya erişim yetkiniz bulunmamaktadır.");
        }
        return "auth/login";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
}
