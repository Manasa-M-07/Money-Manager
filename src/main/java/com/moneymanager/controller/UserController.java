package com.moneymanager.controller;

import com.moneymanager.entity.User;
import com.moneymanager.entity.Income;
import com.moneymanager.entity.Expense;
import com.moneymanager.entity.Budget;
import com.moneymanager.dto.TransactionDTO;
import com.moneymanager.service.UserService;
import com.moneymanager.service.IncomeService;
import com.moneymanager.service.ExpenseService;
import com.moneymanager.service.BudgetService;
import com.moneymanager.config.SecurityUtil;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private IncomeService incomeService;
    
    @Autowired
    private ExpenseService expenseService;
    
    @Autowired
    private BudgetService budgetService;

    // Helper to get logged-in user from session
    private User getSessionUser(HttpSession session) {
        return (User) session.getAttribute("user");
    }

    // Refresh user object in session
    private void refreshSessionUser(HttpSession session, User user) {
        session.setAttribute("user", user);
    }

    @GetMapping
    public String showDashboard(HttpSession session, Model model) {
        User user = getSessionUser(session);
        
        // Calculations
        Double totalIncome = incomeService.getTotalIncome(user);
        Double totalExpenses = expenseService.getTotalExpenses(user);
        Double balance = totalIncome - totalExpenses;
        Double totalSavings = totalIncome - totalExpenses; // Savings = Income - Expenses

        // Monthly calculations (Current Month)
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        String currentMonthStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Double monthlyIncome = incomeService.getIncomeBetween(user, firstDayOfMonth, lastDayOfMonth);
        Double monthlyExpenses = expenseService.getExpensesBetween(user, firstDayOfMonth, lastDayOfMonth);
        
        Double monthlyBudget = budgetService.getBudgetAmount(user, currentMonthStr);
        Double remainingBudget = monthlyBudget - monthlyExpenses;

        // Warnings and Notifications
        boolean budgetExceeded = false;
        if (monthlyBudget > 0 && monthlyExpenses > monthlyBudget) {
            budgetExceeded = true;
        }

        // Recent merged transactions (top 5)
        List<Income> incomes = incomeService.getIncomeHistory(user);
        List<Expense> expenses = expenseService.getExpenseHistory(user);
        List<TransactionDTO> txList = new ArrayList<>();
        
        for (Income inc : incomes) {
            txList.add(TransactionDTO.builder()
                    .id(inc.getId())
                    .amount(inc.getAmount())
                    .type("INCOME")
                    .categoryOrSource(inc.getSource())
                    .date(inc.getDate())
                    .description(inc.getDescription())
                    .build());
        }
        for (Expense exp : expenses) {
            txList.add(TransactionDTO.builder()
                    .id(exp.getId())
                    .amount(exp.getAmount())
                    .type("EXPENSE")
                    .categoryOrSource(exp.getCategory())
                    .date(exp.getDate())
                    .description(exp.getDescription())
                    .build());
        }
        
        Collections.sort(txList);
        List<TransactionDTO> recentTransactions = txList.stream().limit(5).collect(Collectors.toList());

        // Model bindings
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("balance", balance);
        model.addAttribute("monthlyBudget", monthlyBudget);
        model.addAttribute("remainingBudget", remainingBudget);
        model.addAttribute("totalSavings", totalSavings);
        model.addAttribute("monthlyIncome", monthlyIncome);
        model.addAttribute("monthlyExpenses", monthlyExpenses);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("budgetExceeded", budgetExceeded);
        model.addAttribute("currentMonth", today.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        return "dashboard";
    }

    // --- Income Management ---
    @GetMapping("/income")
    public String showIncome(HttpSession session, Model model) {
        User user = getSessionUser(session);
        model.addAttribute("incomes", incomeService.getIncomeHistory(user));
        model.addAttribute("newIncome", new Income());
        return "income";
    }

    @PostMapping("/income/add")
    public String addIncome(@Valid @ModelAttribute("newIncome") Income income, 
                            BindingResult bindingResult, 
                            HttpSession session, 
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Failed to add income: Invalid details!");
            return "redirect:/dashboard/income";
        }
        User user = getSessionUser(session);
        incomeService.addIncome(income, user);
        redirectAttributes.addFlashAttribute("success", "Income added successfully!");
        return "redirect:/dashboard/income";
    }

    @PostMapping("/income/edit")
    public String editIncome(@RequestParam("id") Long id, 
                             @ModelAttribute Income income, 
                             HttpSession session, 
                             RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        try {
            incomeService.updateIncome(id, income, user);
            redirectAttributes.addFlashAttribute("success", "Record updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/income";
    }

    @GetMapping("/income/delete/{id}")
    public String deleteIncome(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        try {
            incomeService.deleteIncome(id, user);
            redirectAttributes.addFlashAttribute("success", "Record deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/income";
    }

    // --- Expense Management ---
    @GetMapping("/expense")
    public String showExpense(HttpSession session, Model model) {
        User user = getSessionUser(session);
        model.addAttribute("expenses", expenseService.getExpenseHistory(user));
        model.addAttribute("newExpense", new Expense());
        model.addAttribute("categories", Arrays.asList("Food", "Travel", "Shopping", "Bills", "Rent", "Health", "Education", "Entertainment", "Fuel", "Investment", "Other"));
        return "expense";
    }

    @PostMapping("/expense/add")
    public String addExpense(@Valid @ModelAttribute("newExpense") Expense expense, 
                             BindingResult bindingResult, 
                             HttpSession session, 
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Failed to add expense: Invalid details!");
            return "redirect:/dashboard/expense";
        }
        User user = getSessionUser(session);
        expenseService.addExpense(expense, user);

        // Budget Exceeded alert helper
        String currentMonthStr = expense.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Double monthlyBudget = budgetService.getBudgetAmount(user, currentMonthStr);
        if (monthlyBudget > 0) {
            LocalDate start = expense.getDate().withDayOfMonth(1);
            LocalDate end = expense.getDate().withDayOfMonth(expense.getDate().lengthOfMonth());
            Double totalExpForMonth = expenseService.getExpensesBetween(user, start, end);
            if (totalExpForMonth > monthlyBudget) {
                redirectAttributes.addFlashAttribute("warning", "Warning: Budget exceeded for " + currentMonthStr + "!");
            }
        }
        
        redirectAttributes.addFlashAttribute("success", "Expense added successfully!");
        return "redirect:/dashboard/expense";
    }

    @PostMapping("/expense/edit")
    public String editExpense(@RequestParam("id") Long id, 
                              @ModelAttribute Expense expense, 
                              HttpSession session, 
                              RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        try {
            expenseService.updateExpense(id, expense, user);
            redirectAttributes.addFlashAttribute("success", "Record updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/expense";
    }

    @GetMapping("/expense/delete/{id}")
    public String deleteExpense(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        try {
            expenseService.deleteExpense(id, user);
            redirectAttributes.addFlashAttribute("success", "Record deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/expense";
    }

    // --- Budget Module ---
    @GetMapping("/budget")
    public String showBudget(HttpSession session, Model model) {
        User user = getSessionUser(session);
        LocalDate today = LocalDate.now();
        String currentMonthStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Double monthlyBudget = budgetService.getBudgetAmount(user, currentMonthStr);
        Double monthlyExpenses = expenseService.getExpensesBetween(user, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
        Double remainingBudget = monthlyBudget - monthlyExpenses;

        model.addAttribute("monthlyBudget", monthlyBudget);
        model.addAttribute("monthlyExpenses", monthlyExpenses);
        model.addAttribute("remainingBudget", remainingBudget);
        model.addAttribute("currentMonth", currentMonthStr);
        return "budget";
    }

    @PostMapping("/budget/update")
    public String updateBudget(@RequestParam("amount") Double amount, 
                               @RequestParam("month") String month, 
                               HttpSession session, 
                               RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        budgetService.setOrUpdateBudget(user, month, amount);
        redirectAttributes.addFlashAttribute("success", "Budget updated successfully!");
        return "redirect:/dashboard/budget";
    }

    // --- Reports ---
    @GetMapping("/reports")
    public String showReports(
            @RequestParam(value = "type", defaultValue = "EXPENSE") String type,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            HttpSession session, Model model) {
        
        User user = getSessionUser(session);
        
        if ("INCOME".equalsIgnoreCase(type)) {
            List<Income> data = incomeService.searchAndFilter(user, search, startDate, endDate);
            model.addAttribute("reportData", data);
            model.addAttribute("totalReportAmount", data.stream().mapToDouble(Income::getAmount).sum());
        } else {
            List<Expense> data = expenseService.searchAndFilter(user, search, category, startDate, endDate);
            model.addAttribute("reportData", data);
            model.addAttribute("totalReportAmount", data.stream().mapToDouble(Expense::getAmount).sum());
        }

        // Monthly Summary Aggregation
        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        Double mIncome = incomeService.getIncomeBetween(user, start, end);
        Double mExpense = expenseService.getExpensesBetween(user, start, end);
        List<Object[]> catSums = expenseService.getCategoryBreakdown(user, start, end);

        model.addAttribute("monthlyIncome", mIncome);
        model.addAttribute("monthlyExpense", mExpense);
        model.addAttribute("categoryBreakdown", catSums);
        model.addAttribute("categories", Arrays.asList("Food", "Travel", "Shopping", "Bills", "Rent", "Health", "Education", "Entertainment", "Fuel", "Investment", "Other"));
        
        // Retain filters in view
        model.addAttribute("type", type);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "reports";
    }

    // --- User Profile ---
    @GetMapping("/profile")
    public String showProfile() {
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        try {
            User updated = userService.updateUserProfile(user.getId(), name, email, phone);
            refreshSessionUser(session, updated);
            redirectAttributes.addFlashAttribute("success", "Record updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/profile";
    }

    @PostMapping("/profile/picture")
    public String uploadProfilePicture(
            @RequestParam("profilePic") MultipartFile file,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
            return "redirect:/dashboard/profile";
        }
        try {
            String mimeType = file.getContentType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("error", "Only image files are allowed.");
                return "redirect:/dashboard/profile";
            }
            byte[] bytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;
            
            user.setProfilePicture(dataUrl);
            userService.saveUserDirectly(user);
            refreshSessionUser(session, user);
            
            redirectAttributes.addFlashAttribute("success", "Profile picture updated successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error uploading image: " + e.getMessage());
        }
        return "redirect:/dashboard/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        String hashedCurrent = SecurityUtil.hashPassword(currentPassword);
        
        if (!user.getPassword().equals(hashedCurrent)) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect!");
            return "redirect:/dashboard/profile";
        }

        try {
            userService.changePassword(user.getId(), newPassword);
            // update cached password in session object
            user.setPassword(SecurityUtil.hashPassword(newPassword));
            refreshSessionUser(session, user);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/profile";
    }
}
