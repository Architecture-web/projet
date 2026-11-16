package com.skillquest.gamification.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Certifications", description = "Profil certifie, verification publique et PDF (routes publiques)")
@RestController
@RequestMapping("/api/certifications")
public class CertificationController {

    private final CertificateService certificateService;

    public CertificationController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /** Profil certifié (XP, niveau, badges, certificats) : appelé par job-board-service. */
    @Operation(summary = "Profil certifie : XP, niveau, badges et certificats (appele par job-board-service)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Profil certifie"), @ApiResponse(responseCode = "404", description = "Aucun profil pour cet utilisateur")})
    @GetMapping("/{userId}")
    public CertifiedProfileResponse certifiedProfile(@PathVariable UUID userId) {
        return certificateService.getCertifiedProfile(userId);
    }

    /** Vérification publique d'un certificat, avec l'empreinte en paramètre optionnel. */
    @Operation(summary = "Verification publique d'un certificat (hash optionnel) : valid=false si altere, faux hash ou revoque")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Resultat de la verification"), @ApiResponse(responseCode = "404", description = "Certificat inconnu")})
    @GetMapping("/{id}/verify")
    public VerificationResponse verify(@PathVariable UUID id, @RequestParam(required = false) String hash) {
        return certificateService.verify(id, hash);
    }

    @Operation(summary = "Telecharge le certificat au format PDF")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Fichier PDF"), @ApiResponse(responseCode = "404", description = "Certificat inconnu ou revoque")})
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
