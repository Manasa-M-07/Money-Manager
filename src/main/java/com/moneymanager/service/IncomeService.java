package com.moneymanager.service;

import com.moneymanager.entity.Income;
import com.moneymanager.entity.User;
import com.moneymanager.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class IncomeService {

    @Autowired
    private IncomeRepository incomeRepository;

    public List<Income> getIncomeHistory(User user) {
        return incomeRepository.findByUserOrderByDateDesc(user);
    }

    public Double getTotalIncome(User user) {
        return incomeRepository.sumTotalByUser(user);
    }

    public Double getIncomeBetween(User user, LocalDate start, LocalDate end) {
        return incomeRepository.sumByUserAndDateBetween(user, start, end);
    }

    public Optional<Income> getIncomeById(Long id) {
        return incomeRepository.findById(id);
    }

    @Transactional
    public Income addIncome(Income income, User user) {
        income.setUser(user);
        return incomeRepository.save(income);
    }

    @Transactional
    public Income updateIncome(Long id, Income updatedIncome, User user) {
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Income record not found"));
        
        if (!income.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to record");
        }

        income.setAmount(updatedIncome.getAmount());
        income.setSource(updatedIncome.getSource());
        income.setDate(updatedIncome.getDate());
        income.setDescription(updatedIncome.getDescription());
        
        return incomeRepository.save(income);
    }

    @Transactional
    public void deleteIncome(Long id, User user) {
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Income record not found"));
        
        if (!income.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to record");
        }

        incomeRepository.delete(income);
    }

    public List<Income> searchAndFilter(User user, String search, String startDateStr, String endDateStr) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        
        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            startDate = LocalDate.parse(startDateStr);
        }
        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            endDate = LocalDate.parse(endDateStr);
        }

        String searchPattern = (search == null || search.trim().isEmpty()) ? null : search.trim();
        return incomeRepository.searchAndFilter(user, searchPattern, startDate, endDate);
    }
}
