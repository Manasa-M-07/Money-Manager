package com.moneymanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private Double amount;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructors
    public Expense() {
    }

    public Expense(Long id, Double amount, String category, LocalDate date, String description, User user) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Builder
    public static ExpenseBuilder builder() {
        return new ExpenseBuilder();
    }

    public static class ExpenseBuilder {
        private Long id;
        private Double amount;
        private String category;
        private LocalDate date;
        private String description;
        private User user;

        public ExpenseBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public ExpenseBuilder amount(Double amount) {
            this.amount = amount;
            return this;
        }
        public ExpenseBuilder category(String category) {
            this.category = category;
            return this;
        }
        public ExpenseBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }
        public ExpenseBuilder description(String description) {
            this.description = description;
            return this;
        }
        public ExpenseBuilder user(User user) {
            this.user = user;
            return this;
        }
        public Expense build() {
            return new Expense(id, amount, category, date, description, user);
        }
    }
}
