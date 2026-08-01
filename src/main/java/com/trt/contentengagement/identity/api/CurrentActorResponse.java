package com.trt.contentengagement.identity.api;

import java.util.List;

public record CurrentActorResponse(String actorId, List<String> roles) {
}
