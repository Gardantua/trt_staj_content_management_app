package com.trt.contentengagement.gameplay.application;

import com.trt.contentengagement.gameplay.domain.GameplayRuleViolationException;

public class AttemptExpiredException extends GameplayRuleViolationException {
    public AttemptExpiredException() { super("ATTEMPT_EXPIRED", "The attempt deadline has passed."); }
}
