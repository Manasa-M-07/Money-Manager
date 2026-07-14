package com.moneymanager.controller;

import com.moneymanager.entity.User;
import com.moneymanager.service.UserService;
import com.moneymanager.config.SecurityUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            return "ADMIN".equals(user.getRole()) ? "redirect:/admin/dashboard" : "redirect:/dashboard";
        }
        return "home";
    }

    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            return "ADMIN".equals(user.getRole()) ? "redirect:/admin/dashboard" : "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(SecurityUtil.hashPassword(password))) {
                if (!user.isActive()) {
                    redirectAttributes.addFlashAttribute("error", "Your account is deactivated. Please contact Admin.");
                    return "redirect:/login";
                }
                session.setAttribute("user", user);
                redirectAttributes.addFlashAttribute("success", "Welcome back, " + user.getName() + "!");
                return "ADMIN".equals(user.getRole()) ? "redirect:/admin/dashboard" : "redirect:/dashboard";
            }
        }
        redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, HttpSession session) {
        if (session != null && session.getAttribute("user") != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (phone.equals(user.getPhone())) {
                userService.changePassword(user.getId(), newPassword);
                redirectAttributes.addFlashAttribute("success", "Password reset successful! Please log in with your new password.");
                return "redirect:/login";
            }
        }
        model.addAttribute("error", "Invalid Email or Phone Number combination!");
        return "forgot-password";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("success", "You have been logged out successfully.");
        return "redirect:/login";
    }
}
