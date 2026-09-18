package com.leetcode.backend.model;
import jakarta.persistence.*;
@Entity @Table(name="assessment_problems",uniqueConstraints=@UniqueConstraint(columnNames={"assessment_id","problem_id"}),indexes=@Index(name="idx_assessment_problem",columnList="assessment_id,display_order")) public class AssessmentProblem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assessment_id",nullable=false) private Assessment assessment; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="problem_id",nullable=false) private Problem problem; @Column(name="display_order",nullable=false) private int displayOrder; @Column(nullable=false) private int points=100;
 public Long getId(){return id;} public Assessment getAssessment(){return assessment;} public void setAssessment(Assessment v){assessment=v;} public Problem getProblem(){return problem;} public void setProblem(Problem v){problem=v;} public int getDisplayOrder(){return displayOrder;} public void setDisplayOrder(int v){displayOrder=v;} public int getPoints(){return points;} public void setPoints(int v){points=v;}
}
