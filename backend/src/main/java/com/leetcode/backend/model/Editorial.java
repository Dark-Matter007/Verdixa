package com.leetcode.backend.model;
import jakarta.persistence.*;
@Entity @Table(name="editorials") public class Editorial {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(optional=false) @JoinColumn(name="problem_id",nullable=false,unique=true) private Problem problem;
 @Column(columnDefinition="TEXT") private String intuition; @Column(columnDefinition="TEXT") private String approach; @Column(columnDefinition="TEXT") private String algorithmExplanation; @Column(columnDefinition="TEXT") private String edgeCases;
 private String timeComplexity; private String spaceComplexity;
 @Column(columnDefinition="TEXT") private String javaSolution; @Column(columnDefinition="TEXT") private String cppSolution; @Column(columnDefinition="TEXT") private String pythonSolution;
 public Long getId(){return id;} public Problem getProblem(){return problem;} public void setProblem(Problem v){problem=v;} public String getIntuition(){return intuition;} public void setIntuition(String v){intuition=v;} public String getApproach(){return approach;} public void setApproach(String v){approach=v;} public String getAlgorithmExplanation(){return algorithmExplanation;} public void setAlgorithmExplanation(String v){algorithmExplanation=v;} public String getEdgeCases(){return edgeCases;} public void setEdgeCases(String v){edgeCases=v;} public String getTimeComplexity(){return timeComplexity;} public void setTimeComplexity(String v){timeComplexity=v;} public String getSpaceComplexity(){return spaceComplexity;} public void setSpaceComplexity(String v){spaceComplexity=v;} public String getJavaSolution(){return javaSolution;} public void setJavaSolution(String v){javaSolution=v;} public String getCppSolution(){return cppSolution;} public void setCppSolution(String v){cppSolution=v;} public String getPythonSolution(){return pythonSolution;} public void setPythonSolution(String v){pythonSolution=v;}
}
