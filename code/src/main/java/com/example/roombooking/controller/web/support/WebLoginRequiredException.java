package com.example.roombooking.controller.web.support;

public class WebLoginRequiredException extends RuntimeException {
    public WebLoginRequiredException() {
        super("เข้าสู่ระบบเพื่อดำเนินการต่อ");
    }
}
