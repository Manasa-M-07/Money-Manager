package com.moneymanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "income")
public class Income {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private Double amount;
    
    @NotBlank(message = "Source is required")
    private String source;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructors
    public Income() {
    }

    public Income(Long id, Double amount, String source, LocalDate date, String description, User user) {
        this.id = id;
        this.amount = amount;
        this.source = source;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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
    public static IncomeBuilder builder() {
        return new IncomeBuilder();
    }

    public static class IncomeBuilder {
        private Long id;
        private Double amount;
        private String source;
        private LocalDate date;
        private String description;
        private User user;

        public IncomeBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public IncomeBuilder amount(Double amount) {
            this.amount = amount;
            return this;
        }
        public IncomeBuilder source(String source) {
            this.source = source;
            return this;
        }
        public IncomeBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }
        public IncomeBuilder description(String description) {
            this.description = description;
            return this;
        }
        public IncomeBuilder user(User user) {
            this.user = user;
            return this;
        }
        public Income build() {
            return new Income(id, amount, source, date, description, user);
        }
    }
}
