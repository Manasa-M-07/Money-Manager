package com.moneymanager.repository;

import com.moneymanager.entity.Income;
import com.moneymanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByUserOrderByDateDesc(User user);
    
    @Query("SELECT COALESCE(SUM(i.amount), 0.0) FROM Income i WHERE i.user = :user")
    Double sumTotalByUser(@Param("user") User user);
    
    @Query("SELECT i FROM Income i WHERE i.user = :user AND (:desc is null OR i.description LIKE %:desc% OR i.source LIKE %:desc%) AND (:startDate is null OR i.date >= :startDate) AND (:endDate is null OR i.date <= :endDate) ORDER BY i.date DESC")
    List<Income> searchAndFilter(
        @Param("user") User user, 
        @Param("desc") String desc, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT COALESCE(SUM(i.amount), 0.0) FROM Income i WHERE i.user = :user AND i.date >= :startDate AND i.date <= :endDate")
    Double sumByUserAndDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
