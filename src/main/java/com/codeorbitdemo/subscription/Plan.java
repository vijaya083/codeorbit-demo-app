package com.codeorbitdemo.subscription;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
@Table(name = "plans", uniqueConstraints = @UniqueConstraint(name = "uk_plan_code", columnNames = "code"))
public class Plan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 32) private String code;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal monthlyPrice;
    @Column(nullable = false) private boolean active;
    protected Plan() { }
    public Plan(String code, String name, BigDecimal monthlyPrice, boolean active) { this.code = code; this.name = name; this.monthlyPrice = monthlyPrice; this.active = active; }
    public Long getId() { return id; } public String getCode() { return code; } public String getName() { return name; } public BigDecimal getMonthlyPrice() { return monthlyPrice; } public boolean isActive() { return active; }
}
