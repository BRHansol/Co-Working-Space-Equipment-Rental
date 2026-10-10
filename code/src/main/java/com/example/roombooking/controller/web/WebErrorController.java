package com.example.roombooking.controller.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Profile("web")
public class WebErrorController implements ErrorController {
    @RequestMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = code instanceof Integer value ? value : 500;
        model.addAttribute("status", status);
        model.addAttribute("title", status == 404 ? "ไม่พบหน้าที่ต้องการ" : "ดำเนินการไม่สำเร็จ");
        model.addAttribute("message", status == 404 ? "หน้านี้อาจถูกย้าย หรือไม่มีอยู่ในเว็บไซต์" : "กรุณาลองใหม่อีกครั้ง หรือกลับไปที่หน้าหลัก");
        return "common/error";
    }
}
