package com.leetcode.backend.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="editorials") public class Editorial {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(optional=false) @JoinColumn(name="problem_id",nullable=false,unique=true) private Problem problem;
 @Column(length=180) private String title; @Column(nullable=false) private boolean published=false;
 @Column(columnDefinition="TEXT") private String intuition; @Column(columnDefinition="TEXT") private String approach; @Column(columnDefinition="TEXT") private String algorithmExplanation; @Column(columnDefinition="TEXT") private String edgeCases;
 private String timeComplexity; private String spaceComplexity;
 @Column(columnDefinition="TEXT") private String javaSolution; @Column(columnDefinition="TEXT") private String cppSolution; @Column(columnDefinition="TEXT") private String pythonSolution;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;

 @Column(columnDefinition="TEXT") private String keyObservations;
 public String getKeyObservations(){return keyObservations;} public void setKeyObservations(String value){keyObservations=value;}

 @Column(columnDefinition="TEXT") private String correctness;
 public String getCorrectness(){return correctness;} public void setCorrectness(String value){correctness=value;}

 @Column(columnDefinition="TEXT") private String referenceGuidance;
 public String getReferenceGuidance(){return referenceGuidance;} public void setReferenceGuidance(String value){referenceGuidance=value;}

 @Column(columnDefinition="TEXT") private String contentSource;
 public String getContentSource(){return contentSource;} public void setContentSource(String value){contentSource=value;}
 @PrePersist void created(){createdAt=updatedAt=LocalDateTime.now();} @PreUpdate void updated(){updatedAt=LocalDateTime.now();}
 public Long getId(){return id;} public Problem getProblem(){return problem;} public void setProblem(Problem v){problem=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public boolean isPublished(){return published;} public void setPublished(boolean v){published=v;} public String getIntuition(){return intuition;} public void setIntuition(String v){intuition=v;} public String getApproach(){return approach;} public void setApproach(String v){approach=v;} public String getAlgorithmExplanation(){return algorithmExplanation;} public void setAlgorithmExplanation(String v){algorithmExplanation=v;} public String getEdgeCases(){return edgeCases;} public void setEdgeCases(String v){edgeCases=v;} public String getTimeComplexity(){return timeComplexity;} public void setTimeComplexity(String v){timeComplexity=v;} public String getSpaceComplexity(){return spaceComplexity;} public void setSpaceComplexity(String v){spaceComplexity=v;} public String getJavaSolution(){return javaSolution;} public void setJavaSolution(String v){javaSolution=v;} public String getCppSolution(){return cppSolution;} public void setCppSolution(String v){cppSolution=v;} public String getPythonSolution(){return pythonSolution;} public void setPythonSolution(String v){pythonSolution=v;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
