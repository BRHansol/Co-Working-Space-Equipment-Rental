package com.example.roombooking.config;

import com.example.roombooking.controller.web.support.WebAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("local")
public class WebAssetsConfig implements WebMvcConfigurer {
    private final WebAccessInterceptor access;

    public WebAssetsConfig(WebAccessInterceptor access) {
        this.access = access;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/templates/assets/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(access).addPathPatterns("/**")
                .excludePathPatterns("/assets/**", "/error", "/favicon.ico");
    }
}
