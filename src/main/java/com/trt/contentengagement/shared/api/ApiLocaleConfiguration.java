package com.trt.contentengagement.shared.api;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class ApiLocaleConfiguration {
    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setSupportedLocales(List.of(TURKISH, Locale.ENGLISH));
        localeResolver.setDefaultLocale(TURKISH);
        return localeResolver;
    }
}
