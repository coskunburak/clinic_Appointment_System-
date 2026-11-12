package com.example.clinic.web.error;

import com.example.clinic.common.security.ClinicAuthenticationSuccessHandler;
import com.example.clinic.common.security.SecurityConfig;
import com.example.clinic.common.security.SecurityContextFacade;
import com.example.clinic.identity.infrastructure.UserAccountRepository;
import com.example.clinic.web.common.GlobalModelAttributesAdvice;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Spring MVC slice tests for error pages and secure error responses (NFR-009, TC-045, Section 13.5).
 */
@WebMvcTest(controllers = CustomErrorController.class)
@Import({SecurityConfig.class, GlobalModelAttributesAdvice.class, WebExceptionHandler.class})
class CustomErrorControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityContextFacade securityContextFacade;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @MockBean
    private ClinicAuthenticationSuccessHandler authenticationSuccessHandler;

    @Test
    @DisplayName("NFR-009: 403 status dispatches to error/403 template with HTTP 403 status")
    void handles403ForbiddenSecurely() throws Exception {
        mockMvc.perform(get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(content().string(containsString("403 — Yetkisiz Erişim")))
                .andExpect(content().string(containsString("Bu işlem için yetkiniz bulunmamaktadır.")))
                .andExpect(content().string(not(containsString("SQLException"))))
                .andExpect(content().string(not(containsString("org.hibernate"))));
    }

    @Test
    @DisplayName("NFR-009: 404 status dispatches to error/404 template with HTTP 404 status")
    void handles404NotFoundSecurely() throws Exception {
        mockMvc.perform(get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("404 — Sayfa Bulunamadı")))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }

    @Test
    @DisplayName("NFR-009: 500 status dispatches to error/500 template with HTTP 500 status")
    void handles500InternalErrorWithoutStackTraceLeakage() throws Exception {
        mockMvc.perform(get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(content().string(containsString("500 — Beklenmeyen Sistem Hatası")))
                .andExpect(content().string(not(containsString("NullPointerException"))))
                .andExpect(content().string(not(containsString("SQLException"))))
                .andExpect(content().string(not(containsString("HibernateException"))));
    }
}
