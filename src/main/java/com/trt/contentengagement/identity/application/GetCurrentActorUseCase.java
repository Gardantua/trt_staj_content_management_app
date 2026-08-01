package com.trt.contentengagement.identity.application;

import org.springframework.stereotype.Service;

@Service
public class GetCurrentActorUseCase {

    private final CurrentActorProvider currentActorProvider;

    public GetCurrentActorUseCase(CurrentActorProvider currentActorProvider) {
        this.currentActorProvider = currentActorProvider;
    }

    public CurrentActor execute() {
        return currentActorProvider.getCurrentActor();
    }
}
