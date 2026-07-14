package com.moneymanager.service;

import com.moneymanager.entity.Budget;
import com.moneymanager.entity.User;
import com.moneymanager.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    public Optional<Budget> getBudget(User user, String month) {
        return budgetRepository.findByUserAndMonth(user, month);
    }

    public Double getBudgetAmount(User user, String month) {
        return budgetRepository.findByUserAndMonth(user, month)
                .map(Budget::getAmount)
                .orElse(0.0);
    }

    @Transactional
    public Budget setOrUpdateBudget(User user, String month, Double amount) {
        Optional<Budget> existing = budgetRepository.findByUserAndMonth(user, month);
        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setAmount(amount);
        } else {
            budget = Budget.builder()
                    .user(user)
                    .month(month)
                    .amount(amount)
                    .build();
        }
        return budgetRepository.save(budget);
    }
}
