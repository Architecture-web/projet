package com.skillquest.gamification.web;

import java.util.UUID;

import com.skillquest.gamification.dto.ApiDtos.CertifiedProfileResponse;
import com.skillquest.gamification.dto.ApiDtos.VerificationResponse;
import com.skillquest.gamification.service.CertificateService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {

    private final CertificateService certificateService;

    public CertificationController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /** Profil certifié (XP, niveau, badges, certificats) : appelé par job-board-service. */
    @GetMapping("/{userId}")
    public CertifiedProfileResponse certifiedProfile(@PathVariable UUID userId) {
        return certificateService.getCertifiedProfile(userId);
    }

    /** Vérification publique d'un certificat, avec l'empreinte en paramètre optionnel. */
    @GetMapping("/{id}/verify")
    public VerificationResponse verify(@PathVariable UUID id, @RequestParam(required = false) String hash) {
        return certificateService.verify(id, hash);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] pdf = certificateService.renderPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("certificate-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
