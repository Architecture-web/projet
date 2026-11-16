package com.skillquest.gamification.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

import com.skillquest.gamification.dto.QuizDtos.AttemptRequest;
import com.skillquest.gamification.dto.QuizDtos.AttemptResult;
import com.skillquest.gamification.dto.QuizDtos.CertificationProgress;
import com.skillquest.gamification.dto.QuizDtos.QuizDetail;
import com.skillquest.gamification.dto.QuizDtos.QuizSummary;
import com.skillquest.gamification.service.QuizService;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Le mini-jeu de quiz : l'etudiant est identifie par son jeton JWT (champ {@code sub}). */
@Tag(name = "Quiz (etudiant)", description = "Mini-jeu quiz : necessite un JWT (bouton Authorize)")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({@ApiResponse(responseCode = "401", description = "Jeton absent, invalide ou expire")})
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public List<QuizSummary> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String courseId) {
        return quizService.list(UserIds.from(jwt), courseId);
    }

    @GetMapping("/{id}")
    public QuizDetail get(@PathVariable UUID id) {
        return quizService.get(id);
    }

    @Operation(summary = "Soumet les reponses : score, XP gagnee (progression uniquement), correction et progression vers le certificat")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Tentative corrigee"), @ApiResponse(responseCode = "400", description = "Question etrangere au quiz"), @ApiResponse(responseCode = "404", description = "Quiz inconnu")})
    @PostMapping("/{id}/attempts")
    public AttemptResult submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody AttemptRequest request) {
        return quizService.submit(UserIds.from(jwt), id, request.answers());
    }

    /** Ou en suis-je pour obtenir le certificat de ce cours ? */
    @Operation(summary = "Progression vers le certificat : cours termine ? moyenne des quiz >= 80 % ?")
    @GetMapping("/progress/{courseId}")
    public CertificationProgress progress(@AuthenticationPrincipal Jwt jwt, @PathVariable String courseId) {
        return quizService.progress(UserIds.from(jwt), courseId);
    }
}
