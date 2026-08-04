package com.trt.contentengagement.gamification.api;

import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.gamification.application.XpService;
import com.trt.contentengagement.gamification.application.XpSummary;
import com.trt.contentengagement.gamification.domain.XpTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class XpController {
    private final XpService xpService;

    public XpController(XpService xpService) {
        this.xpService = xpService;
    }

    @GetMapping("/me/xp")
    public XpSummary currentUserSummary() {
        return xpService.currentUserSummary();
    }

    @PostMapping("/admin/xp-transactions/{transactionId}/adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    public XpTransactionResponse adjust(
            @PathVariable UUID transactionId,
            @Valid @RequestBody AdjustmentRequest request
    ) {
        return XpTransactionResponse.from(xpService.adjust(
                transactionId, request.amount(), request.referenceKey(), request.note()
        ));
    }

    public record AdjustmentRequest(
            int amount,
            @NotBlank @Size(max = 150) String referenceKey,
            @NotBlank @Size(max = 500) String note
    ) {
    }

    public record XpTransactionResponse(
            UUID id,
            UUID userId,
            int amount,
            String reason,
            String policyVersion,
            String referenceKey,
            UUID relatedTransactionId,
            Instant occurredAt
    ) {
        static XpTransactionResponse from(XpTransaction transaction) {
            return new XpTransactionResponse(
                    transaction.id(), transaction.userId(), transaction.amount(),
                    transaction.reason().name(), transaction.policyVersion().name(),
                    transaction.referenceKey(), transaction.relatedTransactionId(),
                    transaction.occurredAt()
            );
        }
    }
}
