package com.mphoYanga.scheduler.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class PageController {

    @GetMapping
    public String landing(){
        return "redirect:/index.html";
    }
    @GetMapping("/login")
    public String login(){
        return "redirect:/login.html";
    }
    @GetMapping("/registerAdmin")
    public String registerAdmin(){
        return "redirect:/register.html";
    }
    @GetMapping("/registerClient")
    public String registerClient(){
        return "redirect:/client-register.html";
    }
    @GetMapping("/client/verify")
    public String clientVerifyPage() {
        return "forward:/client-verify.html";
    }

    @GetMapping("/client/dashboard")
    public String clientDashboard() {
        return "redirect:/client-dashboard.html";
    }

    @GetMapping("/client/calendar")
    public String clientCalendar() {
        return "redirect:/client-calendar.html";
    }

    @GetMapping("/admin/quotations")
    public String adminQuotations() {
        return "redirect:/admin-quotations.html";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        // If using Thymeleaf: place forgot-password.html in src/main/resources/templates/
        // If serving as a static file: place it in src/main/resources/static/ and
        //   use forward: so Spring serves it directly.
        return "forward:/forgot-password.html";
        // OR for Thymeleaf:
        // return "forgot-password";
    }

}
