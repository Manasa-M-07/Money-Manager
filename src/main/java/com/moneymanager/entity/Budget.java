package com.moneymanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "budgets", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "month"})})
public class Budget {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", message = "Amount must be 0 or greater")
    private Double amount;
    
    @NotBlank(message = "Month is required")
    private String month; // format: YYYY-MM
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructors
    public Budget() {
    }

    public Budget(Long id, Double amount, String month, User user) {
        this.id = id;
        this.amount = amount;
        this.month = month;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Builder
    public static BudgetBuilder builder() {
        return new BudgetBuilder();
    }

    public static class BudgetBuilder {
        private Long id;
        private Double amount;
        private String month;
        private User user;

        public BudgetBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public BudgetBuilder amount(Double amount) {
            this.amount = amount;
            return this;
        }
        public BudgetBuilder month(String month) {
            this.month = month;
            return this;
        }
        public BudgetBuilder user(User user) {
            this.user = user;
            return this;
        }
        public Budget build() {
            return new Budget(id, amount, month, user);
        }
    }
}
