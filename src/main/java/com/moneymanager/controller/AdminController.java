package com.moneymanager.controller;

import com.moneymanager.entity.User;
import com.moneymanager.entity.Income;
import com.moneymanager.entity.Expense;
import com.moneymanager.service.UserService;
import com.moneymanager.service.IncomeService;
import com.moneymanager.service.ExpenseService;
import com.moneymanager.repository.IncomeRepository;
import com.moneymanager.repository.ExpenseRepository;
import com.moneymanager.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private IncomeRepository incomeRepository;
    
    @Autowired
    private ExpenseRepository expenseRepository;

    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model) {
        // Calculate totals across all users
        long totalUsers = userRepository.count() - userService.getUsersByRole("ADMIN").size();
        
        Double totalIncome = incomeRepository.findAll().stream().mapToDouble(Income::getAmount).sum();
        Double totalExpense = expenseRepository.findAll().stream().mapToDouble(Expense::getAmount).sum();
        
        List<User> usersList = userService.getUsersByRole("USER");

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("usersList", usersList);
        model.addAttribute("newUser", new User());

        return "admin-dashboard";
    }

    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute("newUser") User user, 
                          BindingResult bindingResult, 
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Failed to add user: Invalid input fields!");
            return "redirect:/admin/dashboard";
        }
        try {
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("success", "User added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/users/edit")
    public String editUser(@RequestParam("id") Long id,
                           @RequestParam("name") String name,
                           @RequestParam("email") String email,
                           @RequestParam("phone") String phone,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserProfile(id, name, email, phone);
            redirectAttributes.addFlashAttribute("success", "Record updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/users/toggle/{id}")
    public String toggleUserStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserActiveStatus(id);
            redirectAttributes.addFlashAttribute("success", "Record updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Record deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
