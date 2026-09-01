package com.leetcode.backend.dto;
import java.util.*;
/** Safe admin analytics projection: deliberately excludes credentials and private notes. */
public record AdminUserAnalyticsResponse(Long id,String username,String role,Object lastCodingActivity,long solvedTotal,long easySolved,long mediumSolved,long hardSolved,long totalSubmissions,long acceptedSubmissions,long acceptanceRate,int currentStreak,int longestStreak,long activeDays,Map<String,Long> languageUsage,List<Map<String,Object>> recentSubmissions) {}
