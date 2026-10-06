package com.example.roombooking.controller.web.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Errors raised before a controller is selected need unscoped local advice. */
@ControllerAdvice
@Profile("local")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalHttpErrorAdvice {
    private final WebSessionSupport sessions;
    private final WebUi ui;
    public LocalHttpErrorAdvice(WebSessionSupport sessions,WebUi ui) { this.sessions=sessions; this.ui=ui; }
    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView missing(HttpServletRequest request,HttpServletResponse response) {
        return errorPage(404,"ไม่พบหน้าที่ต้องการ","หน้านี้อาจถูกย้าย หรือไม่มีอยู่ในเว็บไซต์",request,response);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView methodNotAllowed(HttpRequestMethodNotSupportedException exception,HttpServletRequest request,HttpServletResponse response) {
        if(exception.getSupportedMethods()!=null) response.setHeader("Allow",String.join(", ",exception.getSupportedMethods()));
        return errorPage(405,"ไม่รองรับคำขอนี้","กรุณาดำเนินการผ่านปุ่มหรือแบบฟอร์มในเว็บไซต์",request,response);
    }
    private ModelAndView errorPage(int status,String title,String message,HttpServletRequest request,HttpServletResponse response) {
        response.setStatus(status);
        ModelAndView page=new ModelAndView("common/error");
        page.addObject("status",status); page.addObject("title",title); page.addObject("message",message);
        User user=sessions.getCurrentUser(request);
        page.addObject("currentUser",user); page.addObject("isSignedIn",user!=null);
        page.addObject("isManager",sessions.isManager(user));
        page.addObject("isAdmin",user!=null && user.getRole()==Role.ADMIN);
        page.addObject("isStaff",user!=null && user.getRole()==Role.STAFF);
        page.addObject("csrfToken",sessions.csrfToken(request)); page.addObject("ui",ui);
        return page;
    }
}
