package com.leetcode.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "function_parameters")
public class FunctionParameter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String type;
    @Column(name = "parameter_order", nullable = false) private int parameterOrder;
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getParameterOrder() { return parameterOrder; }
    public void setParameterOrder(int parameterOrder) { this.parameterOrder = parameterOrder; }
}
