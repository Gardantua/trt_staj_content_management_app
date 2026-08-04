package com.trt.contentengagement.gameplay.application;

import com.trt.contentengagement.gameplay.domain.QuizAttemptCompleted;

public interface QuizCompletionEventOutbox {
    void stage(QuizAttemptCompleted completedAttempt);
}
