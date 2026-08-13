package com.trt.contentengagement.identity.infrastructure.email;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.trt.contentengagement.identity.application.PasswordResetNotificationPort;
import com.trt.contentengagement.identity.application.PasswordResetDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
class SmtpPasswordResetNotificationAdapter implements PasswordResetNotificationPort {
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String fromAddress;

    SmtpPasswordResetNotificationAdapter(
            JavaMailSender mailSender,
            @Value("${app.identity.password-reset.frontend-url}") String frontendUrl,
            @Value("${app.identity.password-reset.from-address}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordResetLink(String recipientEmail, String rawToken) {
        String resetLink = frontendUrl + "?resetToken="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("Hikâye İzi şifre sıfırlama");
        message.setText("Şifreni yenilemek için bu bağlantıyı kullan:\n\n" + resetLink
                + "\n\nBağlantı 30 dakika geçerlidir ve yalnız bir kez kullanılabilir.");
        try {
            mailSender.send(message);
        } catch (MailException deliveryFailure) {
            throw new PasswordResetDeliveryException(deliveryFailure);
        }
    }
}
