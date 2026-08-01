package com.trt.contentengagement.identity.api;

import com.trt.contentengagement.identity.application.CurrentActor;
import com.trt.contentengagement.identity.application.GetCurrentActorUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class CurrentActorController {

    private final GetCurrentActorUseCase getCurrentActorUseCase;

    public CurrentActorController(GetCurrentActorUseCase getCurrentActorUseCase) {
        this.getCurrentActorUseCase = getCurrentActorUseCase;
    }

    @GetMapping("/me")
    public CurrentActorResponse getCurrentActor() {
        CurrentActor currentActor = getCurrentActorUseCase.execute();
        var sortedRoleNames = currentActor.roles().stream()
                .map(Enum::name)
                .sorted()
                .toList();

        return new CurrentActorResponse(
                currentActor.actorId().toString(),
                sortedRoleNames
        );
    }
}
