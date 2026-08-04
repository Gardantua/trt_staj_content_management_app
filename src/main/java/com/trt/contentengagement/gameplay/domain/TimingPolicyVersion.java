package com.trt.contentengagement.gameplay.domain;

import java.time.Duration;

public enum TimingPolicyVersion {
    STANDARD_V1(Duration.ofMinutes(5)),
    EXTENDED_V1(Duration.ofMinutes(50));

    private final Duration duration;

    TimingPolicyVersion(Duration duration) {
        this.duration = duration;
    }

    public Duration duration() {
        return duration;
    }
}

