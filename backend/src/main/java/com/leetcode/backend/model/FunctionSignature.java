package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "function_signatures")
public class FunctionSignature {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "function_name", nullable = false) private String functionName;
    @Column(name = "return_type", nullable = false) private String returnType;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true) @JoinColumn(name = "function_signature_id")
    @OrderBy("parameterOrder ASC") private List<FunctionParameter> parameters = new ArrayList<>();
    public Long getId() { return id; }
    public String getFunctionName() { return functionName; }
    public void setFunctionName(String functionName) { this.functionName = functionName; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public List<FunctionParameter> getParameters() { return parameters; }
    public void setParameters(List<FunctionParameter> parameters) { this.parameters = parameters == null ? new ArrayList<>() : parameters; }
}
