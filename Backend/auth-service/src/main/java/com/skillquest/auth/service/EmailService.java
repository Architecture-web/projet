package com.skillquest.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service d'envoi d'e-mails transactionnels via SMTP.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend-url:http://localhost:8081}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envoie le lien sécurisé de réinitialisation de mot de passe par e-mail.
     */
    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        if (mailUsername == null || mailUsername.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service SMTP non configuré (MAIL_USERNAME et MAIL_PASSWORD requis dans .env)."
            );
        }

        String resetLink = frontendUrl.replaceAll("/$", "") + "/index.html?reset_token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject("SkillQuest – Réinitialisation de votre mot de passe");
        message.setText(
            "Bonjour,\n\n"
            + "Vous avez demandé la réinitialisation de votre mot de passe SkillQuest.\n\n"
            + "Votre code de réinitialisation (valable 15 minutes) : " + rawToken + "\n\n"
            + "Ou cliquez directement sur ce lien :\n"
            + resetLink + "\n\n"
            + "Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail."
        );

        try {
            mailSender.send(message);
            log.info("E-mail de réinitialisation envoyé à {}", toEmail);
        } catch (Exception ex) {
            log.error("Échec d'envoi SMTP vers {} : {}", toEmail, ex.getMessage());
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Impossible d'envoyer l'e-mail de réinitialisation. Vérifiez la configuration SMTP."
            );
        }
    }
}
