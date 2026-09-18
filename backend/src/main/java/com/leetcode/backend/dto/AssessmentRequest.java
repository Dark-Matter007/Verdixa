package com.leetcode.backend.dto;

import com.leetcode.backend.model.AssessmentEnums;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public record AssessmentRequest(
 @NotBlank @Size(max=180) String title, @NotBlank @Size(max=180) String organization,
 @Size(max=5000) String description, @Size(max=5000) String instructions,
 @NotNull AssessmentEnums.Visibility visibility, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt,
 LocalDateTime registrationDeadline, @Min(1) @Max(10000) Integer maxParticipants,
 boolean fullscreenRequired, boolean microphoneRequired, boolean strictProctoring,
 @Size(max=100) List<ProblemSelection> problems, @Size(max=100) List<McqSelection> mcqs,
 @Size(max=100) List<@NotNull Long> inviteeIds) {
 @AssertTrue(message="Select at least one coding problem or multiple-choice question.")
 public boolean hasQuestions(){return (problems!=null&&!problems.isEmpty())||(mcqs!=null&&!mcqs.isEmpty());}
 public record ProblemSelection(@NotNull Long problemId,@Min(1) @Max(10000) Integer points){}
 public record McqSelection(@NotBlank @Size(max=2000) String question,@NotNull @Size(min=2,max=6) List<@NotBlank @Size(max=500) String> options,@Min(0) Integer correctOption,@Min(1) @Max(10000) Integer points){
  @AssertTrue(message="Choose the correct option.") public boolean correctOptionInRange(){return correctOption!=null&&options!=null&&correctOption<options.size();}
 }
}
