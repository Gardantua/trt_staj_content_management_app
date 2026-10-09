package com.trt.contentengagement.shared.api;

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorMessageResolver {
    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final List<Locale> SUPPORTED_LOCALES = List.of(TURKISH, Locale.ENGLISH);
    private static final String MESSAGE_PREFIX = "api.error.";

    private final MessageSource messageSource;

    public ApiErrorMessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String resolve(String errorCode, String safeFallbackMessage) {
        return resolve(errorCode, safeFallbackMessage, LocaleContextHolder.getLocale());
    }

    public String resolve(
            HttpServletRequest request,
            String errorCode,
            String safeFallbackMessage
    ) {
        return resolve(errorCode, safeFallbackMessage, localeFrom(request));
    }

    String resolve(String errorCode, String safeFallbackMessage, Locale requestedLocale) {
        Locale supportedLocale = supportedLocale(requestedLocale);
        String localizedMessage = messageSource.getMessage(
                MESSAGE_PREFIX + errorCode,
                null,
                null,
                supportedLocale
        );
        if (localizedMessage != null) {
            return localizedMessage;
        }
        if (Locale.ENGLISH.getLanguage().equals(supportedLocale.getLanguage())) {
            return safeFallbackMessage;
        }
        return messageSource.getMessage(
                MESSAGE_PREFIX + "RULE_CONFLICT",
                null,
                "İstek mevcut iş kurallarıyla çelişiyor.",
                TURKISH
        );
    }

    private Locale localeFrom(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return TURKISH;
        }
        try {
            Locale matchedLocale = Locale.lookup(
                    Locale.LanguageRange.parse(acceptLanguage),
                    SUPPORTED_LOCALES
            );
            return matchedLocale == null ? TURKISH : matchedLocale;
        } catch (IllegalArgumentException invalidAcceptLanguage) {
            return TURKISH;
        }
    }

    private Locale supportedLocale(Locale requestedLocale) {
        return Locale.ENGLISH.getLanguage().equals(requestedLocale.getLanguage())
                ? Locale.ENGLISH
                : TURKISH;
    }
}
