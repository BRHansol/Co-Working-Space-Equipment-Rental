package com.example.roombooking.controller.web;

import com.example.roombooking.controller.web.forms.LoginForm;
import com.example.roombooking.controller.web.forms.RegistrationForm;
import com.example.roombooking.controller.web.support.WebSessionSupport;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.dto.request.UserCreateRequest;
import com.example.roombooking.dto.response.UserResponse;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.repository.UserRepository;
import com.example.roombooking.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Controller
@Profile("web")
public class AuthViewController {
    private final UserRepository users;
    private final UserService userService;
    private final WebSessionSupport sessions;

    public AuthViewController(UserRepository users, UserService userService, WebSessionSupport sessions) {
        this.users = users;
        this.userService = userService;
        this.sessions = sessions;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String next,
                            HttpServletRequest request, Model model) {
        if (sessions.getCurrentUser(request) != null) {
            return "redirect:" + WebSessionSupport.safeNext(next);
        }
        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("next", WebSessionSupport.safeNext(next));
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form, BindingResult errors,
                        @RequestParam(required = false) String next, HttpServletRequest request,
                        Model model, RedirectAttributes redirect) {
        model.addAttribute("next", WebSessionSupport.safeNext(next));
        if (errors.hasErrors()) {
            return "auth/login";
        }
        User user = users.findByUsername(form.getUsername().strip()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || user.getRole() == null
                || !sessions.matchesPassword(form.getPassword(), user.getPassword())) {
            errors.reject("credentials", "ชื่อผู้ใช้หรือรหัสผ่านไม่ตรงกัน");
            return "auth/login";
        }
        sessions.login(request, user);
        redirect.addFlashAttribute("flashSuccess", "เข้าสู่ระบบแล้ว");
        String destination = WebSessionSupport.safeNext(next);
        if (sessions.isManager(user) && destination.equals("/account")) {
            destination = "/admin";
        }
        return "redirect:" + destination;
    }

    @GetMapping("/register")
    public String registrationPage(HttpServletRequest request, Model model) {
        if (sessions.getCurrentUser(request) != null) {
            return "redirect:/account";
        }
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public synchronized String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                                        BindingResult errors, HttpServletRequest request,
                                        RedirectAttributes redirect) {
        if (sessions.getCurrentUser(request) != null) {
            return "redirect:/account";
        }
        if (!errors.hasFieldErrors("password") && form.getPassword() != null
                && form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.rejectValue("password", "length", "รหัสผ่านต้องมีความยาวไม่เกิน 72 ไบต์");
        }
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "match", "รหัสผ่านทั้งสองช่องต้องตรงกัน");
        }
        if (errors.hasErrors()) {
            return "auth/register";
        }
        String username = form.getUsername().strip();
        String email = form.getEmail().strip().toLowerCase(Locale.ROOT);
        if (username.length() < 4) {
            errors.rejectValue("username", "length", "ชื่อผู้ใช้ต้องมี 4–50 ตัวอักษร");
        }
        List<User> existing = users.findAll();
        if (existing.stream().anyMatch(user -> username.equalsIgnoreCase(user.getUsername()))) {
            errors.rejectValue("username", "duplicate", "ชื่อผู้ใช้นี้มีแล้ว เลือกชื่ออื่น");
        }
        if (existing.stream().anyMatch(user -> email.equalsIgnoreCase(user.getEmail()))) {
            errors.rejectValue("email", "duplicate", "อีเมลนี้มีบัญชีแล้ว เข้าสู่ระบบด้วยบัญชีเดิม");
        }
        if (errors.hasErrors()) {
            return "auth/register";
        }
        UserCreateRequest payload = new UserCreateRequest();
        payload.setUsername(username);
        payload.setEmail(email);
        payload.setPassword(form.getPassword());
        payload.setRole(Role.USER);
        UserResponse saved = userService.createUser(payload);
        User savedUser = users.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบบัญชีที่สร้างแล้ว"));
        sessions.login(request, savedUser);
        redirect.addFlashAttribute("flashSuccess", "สร้างบัญชีแล้ว เริ่มเลือกพื้นที่ที่ต้องการได้เลย");
        return "redirect:/account";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        sessions.logout(request);
        return "redirect:/login?loggedOut";
    }
}
