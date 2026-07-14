package com.moneymanager.dto;

import java.time.LocalDate;

public class TransactionDTO implements Comparable<TransactionDTO> {
    private Long id;
    private Double amount;
    private String type; // INCOME or EXPENSE
    private String categoryOrSource;
    private LocalDate date;
    private String description;

    // Constructors
    public TransactionDTO() {
    }

    public TransactionDTO(Long id, Double amount, String type, String categoryOrSource, LocalDate date, String description) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.categoryOrSource = categoryOrSource;
        this.date = date;
        this.description = description;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategoryOrSource() {
        return categoryOrSource;
    }

    public void setCategoryOrSource(String categoryOrSource) {
        this.categoryOrSource = categoryOrSource;
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

    @Override
    public int compareTo(TransactionDTO other) {
        // Sort descending by date
        return other.getDate().compareTo(this.getDate());
    }

    // Builder
    public static TransactionDTOBuilder builder() {
        return new TransactionDTOBuilder();
    }

    public static class TransactionDTOBuilder {
        private Long id;
        private Double amount;
        private String type;
        private String categoryOrSource;
        private LocalDate date;
        private String description;

        public TransactionDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public TransactionDTOBuilder amount(Double amount) {
            this.amount = amount;
            return this;
        }
        public TransactionDTOBuilder type(String type) {
            this.type = type;
            return this;
        }
        public TransactionDTOBuilder categoryOrSource(String categoryOrSource) {
            this.categoryOrSource = categoryOrSource;
            return this;
        }
        public TransactionDTOBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }
        public TransactionDTOBuilder description(String description) {
            this.description = description;
            return this;
        }
        public TransactionDTO build() {
            return new TransactionDTO(id, amount, type, categoryOrSource, date, description);
        }
    }
}
