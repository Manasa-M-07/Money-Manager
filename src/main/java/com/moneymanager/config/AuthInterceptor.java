package com.moneymanager.config;

import com.moneymanager.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // Allow static resources and auth endpoints
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") || uri.startsWith("/webjars/")) {
            return true;
        }
        
        if (uri.equals("/") || uri.equals("/login") || uri.equals("/register") || uri.equals("/forgot-password")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("/login?error=Please log in to access this page");
            return false;
        }

        User user = (User) session.getAttribute("user");
        
        // Admin path restrictions
        if (uri.startsWith("/admin")) {
            if (!"ADMIN".equals(user.getRole())) {
                response.sendRedirect("/dashboard?error=Access denied: Admin role required");
                return false;
            }
        }
        
        // Deactivated users should not be allowed
        if (!user.isActive()) {
            session.invalidate();
            response.sendRedirect("/login?error=Your account is deactivated. Please contact Admin.");
            return false;
        }

        return true;
    }
}
