package com.leetcode.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="translation_cache", uniqueConstraints=@UniqueConstraint(columnNames={"source_language","target_language","source_hash"}), indexes=@Index(name="idx_translation_lookup",columnList="source_language,target_language,source_hash"))
public class TranslationCache {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="source_language",nullable=false,length=12) private String sourceLanguage;
 @Column(name="target_language",nullable=false,length=12) private String targetLanguage;
 @Column(name="source_hash",nullable=false,length=64) private String sourceHash;
 @Column(name="translated_text",nullable=false,columnDefinition="TEXT") private String translatedText;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
 @PrePersist void create(){createdAt=updatedAt=LocalDateTime.now();} @PreUpdate void update(){updatedAt=LocalDateTime.now();}
 public String getSourceLanguage(){return sourceLanguage;} public void setSourceLanguage(String v){sourceLanguage=v;} public String getTargetLanguage(){return targetLanguage;} public void setTargetLanguage(String v){targetLanguage=v;} public String getSourceHash(){return sourceHash;} public void setSourceHash(String v){sourceHash=v;} public String getTranslatedText(){return translatedText;} public void setTranslatedText(String v){translatedText=v;}
}
