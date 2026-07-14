package com.moneymanager.repository;

import com.moneymanager.entity.Expense;
import com.moneymanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserOrderByDateDesc(User user);
    
    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e WHERE e.user = :user")
    Double sumTotalByUser(@Param("user") User user);
    
    @Query("SELECT e FROM Expense e WHERE e.user = :user AND (:desc is null OR e.description LIKE %:desc%) AND (:category is null OR e.category = :category) AND (:startDate is null OR e.date >= :startDate) AND (:endDate is null OR e.date <= :endDate) ORDER BY e.date DESC")
    List<Expense> searchAndFilter(
        @Param("user") User user, 
        @Param("desc") String desc, 
        @Param("category") String category, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e WHERE e.user = :user AND e.date >= :startDate AND e.date <= :endDate")
    Double sumByUserAndDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0.0) FROM Expense e WHERE e.user = :user AND (:startDate is null OR e.date >= :startDate) AND (:endDate is null OR e.date <= :endDate) GROUP BY e.category")
    List<Object[]> sumByCategoryBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
