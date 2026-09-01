package com.leetcode.backend.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="learning_path_sections",indexes=@Index(name="idx_path_section_path_position",columnList="path_id,position")) public class LearningPathSection {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @JsonIgnore @ManyToOne(optional=false) @JoinColumn(name="path_id") private LearningPath path;
 @Column(nullable=false) private String title; private int position;
 @OneToMany(mappedBy="section",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("position") private List<LearningPathItem>items=new ArrayList<>();
 public Long getId(){return id;} public LearningPath getPath(){return path;} public void setPath(LearningPath v){path=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public int getPosition(){return position;} public void setPosition(int v){position=v;} public List<LearningPathItem>getItems(){return items;}}
