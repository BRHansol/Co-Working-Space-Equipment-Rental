package com.example.roombooking.controller.web.support;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.example.roombooking.controller.web")
@Profile("local")
public class WebUiAdvice {
    private final WebUi ui;
    public WebUiAdvice(WebUi ui) { this.ui = ui; }
    @ModelAttribute("ui") public WebUi ui() { return ui; }
}
