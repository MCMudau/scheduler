package com.mphoYanga.scheduler.controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CustomErrorController
 *
 * Intercepts all Spring Boot error routes (/error) and returns the
 * appropriate custom HTML page instead of the default white-label error page.
 *
 * Place 404.html (and optionally 500.html) in:
 *   src/main/resources/static/
 *
 * Spring Boot will serve them as static files — this controller just
 * forwards to the right one based on the HTTP status code.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {

        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (statusObj != null) {
            int status = Integer.parseInt(statusObj.toString());

            if (status == HttpStatus.NOT_FOUND.value()) {           // 404
                return "forward:/404.html";
            }

            if (status == HttpStatus.FORBIDDEN.value()) {           // 403
                return "forward:/404.html";  // treat forbidden same as not found
            }

            if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) { // 500
                return "forward:/404.html";  // swap for 500.html if you create one
            }
        }

        // Fallback for any other error code
        return "forward:/404.html";
    }
}
