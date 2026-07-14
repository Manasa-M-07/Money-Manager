package com.moneymanager.service;

import com.moneymanager.entity.Expense;
import com.moneymanager.entity.User;
import com.moneymanager.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public List<Expense> getExpenseHistory(User user) {
        return expenseRepository.findByUserOrderByDateDesc(user);
    }

    public Double getTotalExpenses(User user) {
        return expenseRepository.sumTotalByUser(user);
    }

    public Double getExpensesBetween(User user, LocalDate start, LocalDate end) {
        return expenseRepository.sumByUserAndDateBetween(user, start, end);
    }

    public List<Object[]> getCategoryBreakdown(User user, LocalDate start, LocalDate end) {
        return expenseRepository.sumByCategoryBetween(user, start, end);
    }

    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }

    @Transactional
    public Expense addExpense(Expense expense, User user) {
        expense.setUser(user);
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense updateExpense(Long id, Expense updatedExpense, User user) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense record not found"));
        
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to record");
        }

        expense.setAmount(updatedExpense.getAmount());
        expense.setCategory(updatedExpense.getCategory());
        expense.setDate(updatedExpense.getDate());
        expense.setDescription(updatedExpense.getDescription());
        
        return expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(Long id, User user) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense record not found"));
        
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to record");
        }

        expenseRepository.delete(expense);
    }

    public List<Expense> searchAndFilter(User user, String search, String category, String startDateStr, String endDateStr) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        
        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            startDate = LocalDate.parse(startDateStr);
        }
        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            endDate = LocalDate.parse(endDateStr);
        }

        String searchPattern = (search == null || search.trim().isEmpty()) ? null : search.trim();
        String catPattern = (category == null || category.trim().isEmpty() || category.equalsIgnoreCase("ALL")) ? null : category.trim();
        
        return expenseRepository.searchAndFilter(user, searchPattern, catPattern, startDate, endDate);
    }
}
