package com.trt.contentengagement.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class ApiErrorMessageResolverTest {
    private final ApiErrorMessageResolver resolver = new ApiErrorMessageResolver(messageSource());

    @Test
    void resolvesStableErrorCodeInTurkishAndEnglish() {
        assertThat(resolver.resolve("VALIDATION_FAILED", "fallback", Locale.forLanguageTag("tr")))
                .isEqualTo("İstek geçersiz veya eksik veri içeriyor.");
        assertThat(resolver.resolve("VALIDATION_FAILED", "fallback", Locale.ENGLISH))
                .isEqualTo("The request contains invalid or missing data.");
    }

    @Test
    void unsupportedLanguageFallsBackToTurkishWithoutChangingTheErrorCode() {
        assertThat(resolver.resolve("AUTHENTICATION_REQUIRED", "fallback", Locale.FRENCH))
                .isEqualTo("Bu kaynağa erişmek için giriş yapmalısınız.");
    }

    @Test
    void unknownRuleKeepsSafeEnglishDetailAndUsesSafeTurkishFallback() {
        assertThat(resolver.resolve("FUTURE_RULE", "Safe English detail.", Locale.ENGLISH))
                .isEqualTo("Safe English detail.");
        assertThat(resolver.resolve("FUTURE_RULE", "Safe English detail.", Locale.forLanguageTag("tr")))
                .isEqualTo("İstek mevcut iş kurallarıyla çelişiyor.");
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
