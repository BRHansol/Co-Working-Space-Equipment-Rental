package com.example.roombooking.controller.web.support;

import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@ControllerAdvice(basePackages = "com.example.roombooking.controller.web")
@Profile("web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebViewAdvice {
    private static final Logger log = LoggerFactory.getLogger(WebViewAdvice.class);
    private final WebSessionSupport sessions;

    public WebViewAdvice(WebSessionSupport sessions) {
        this.sessions = sessions;
    }

    @ModelAttribute
    public void sharedModel(HttpServletRequest request, Model model) {
        User user = sessions.getCurrentUser(request);
        model.addAttribute("currentUser", user);
        model.addAttribute("isSignedIn", user != null);
        model.addAttribute("isManager", sessions.isManager(user));
        model.addAttribute("isAdmin", user != null && user.getRole() == Role.ADMIN);
        model.addAttribute("isStaff", user != null && user.getRole() == Role.STAFF);
        model.addAttribute("csrfToken", sessions.csrfToken(request));
    }

    @ExceptionHandler(WebLoginRequiredException.class)
    public String loginRequired(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "redirect:/login?next=" + URLEncoder.encode(WebSessionSupport.safeNext(path),
                StandardCharsets.UTF_8);
    }

    @ExceptionHandler({WebAccessDeniedException.class, ForbiddenException.class})
    public String forbidden(RuntimeException exception, HttpServletRequest request,
                            HttpServletResponse response, Model model) {
        return error(403, "ไม่มีสิทธิ์เข้าถึง", exception.getMessage(), request, response, model);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String notFound(ResourceNotFoundException exception, HttpServletRequest request,
                           HttpServletResponse response, Model model) {
        return error(404, "ไม่พบรายการ", "รายการที่ต้องการไม่มีอยู่ หรือถูกลบแล้ว", request, response, model);
    }

    @ExceptionHandler({RoomNotAvailableException.class, EquipmentNotAvailableException.class,
            ConflictException.class, InvalidStateTransitionException.class})
    public String conflict(RuntimeException exception, HttpServletRequest request,
                           HttpServletResponse response, Model model) {
        return error(409, "ดำเนินการไม่ได้", exception.getMessage(), request, response, model);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String invalidRequest(IllegalArgumentException exception, HttpServletRequest request,
                                 HttpServletResponse response, Model model) {
        return error(400, "ตรวจข้อมูลอีกครั้ง", exception.getMessage(), request, response, model);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String responseStatus(ResponseStatusException exception, HttpServletRequest request,
                                 HttpServletResponse response, Model model) {
        int status = exception.getStatusCode().value();
        String message = status >= 500 ? "ระบบยังดำเนินการไม่ได้ ลองอีกครั้งในภายหลัง"
                : (exception.getReason() != null ? exception.getReason() : "ไม่สามารถดำเนินการกับรายการนี้ได้");
        if (status >= 500) {
            log.error("Web request failed: {}", request.getRequestURI(), exception);
        }
        return error(status, "ดำเนินการไม่ได้", message, request, response, model);
    }

    @ExceptionHandler({TypeMismatchException.class, BindException.class,
            ServletRequestBindingException.class, HttpMessageNotReadableException.class})
    public String invalidFormat(Exception exception, HttpServletRequest request,
                                HttpServletResponse response, Model model) {
        return error(400, "ตรวจรูปแบบข้อมูล", "ตรวจวันที่ เวลา หรือหมายเลขรายการ แล้วลองอีกครั้ง",
                request, response, model);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String unsupportedMethod(HttpServletRequest request, HttpServletResponse response, Model model) {
        return error(405, "ใช้แบบฟอร์มเพื่อดำเนินการ", "กลับไปที่หน้าเว็บไซต์แล้วใช้ปุ่มหรือแบบฟอร์มของรายการ",
                request, response, model);
    }

    @ExceptionHandler(Exception.class)
    public String unexpected(Exception exception, HttpServletRequest request,
                             HttpServletResponse response, Model model) {
        log.error("Unexpected web error: {}", request.getRequestURI(), exception);
        return error(500, "ระบบยังดำเนินการไม่ได้", "ลองอีกครั้งในภายหลัง หรือกลับไปเลือกพื้นที่ใหม่",
                request, response, model);
    }

    private String error(int status, String title, String message, HttpServletRequest request,
                         HttpServletResponse response, Model model) {
        try {
            sharedModel(request, model);
        } catch (RuntimeException modelFailure) {
            model.addAttribute("currentUser", null);
            model.addAttribute("isSignedIn", false);
            model.addAttribute("isManager", false);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isStaff", false);
            model.addAttribute("csrfToken", sessions.csrfToken(request));
        }
        response.setStatus(status);
        model.addAttribute("status", status);
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        return "common/error";
    }
}
