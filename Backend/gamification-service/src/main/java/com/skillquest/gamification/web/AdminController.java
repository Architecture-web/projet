package com.skillquest.gamification.web;

import java.util.List;
import java.util.UUID;

import com.skillquest.gamification.dto.ApiDtos.BadgeResponse;
import com.skillquest.gamification.dto.ApiDtos.ProfileResponse;
import com.skillquest.gamification.dto.QuizDtos.AdminCertificateView;
import com.skillquest.gamification.dto.QuizDtos.AdminPlayerView;
import com.skillquest.gamification.dto.QuizDtos.AdminQuizView;
import com.skillquest.gamification.dto.QuizDtos.BadgeUpsert;
import com.skillquest.gamification.dto.QuizDtos.QuizUpsert;
import com.skillquest.gamification.dto.QuizDtos.XpAdjustRequest;
import com.skillquest.gamification.service.AdminService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API du dashboard administrateur. Reservee au role ROLE_ADMIN (voir SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ---- quiz

    @GetMapping("/quizzes")
    public List<AdminQuizView> listQuizzes() {
        return adminService.listQuizzes();
    }

    @GetMapping("/quizzes/{id}")
    public AdminQuizView getQuiz(@PathVariable UUID id) {
        return adminService.getQuiz(id);
    }

    @PostMapping("/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminQuizView createQuiz(@Valid @RequestBody QuizUpsert request) {
        return adminService.createQuiz(request);
    }

    @PutMapping("/quizzes/{id}")
    public AdminQuizView updateQuiz(@PathVariable UUID id, @Valid @RequestBody QuizUpsert request) {
        return adminService.updateQuiz(id, request);
    }

    @DeleteMapping("/quizzes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuiz(@PathVariable UUID id) {
        adminService.deleteQuiz(id);
    }

    // ---- badges

    @PostMapping("/badges")
    @ResponseStatus(HttpStatus.CREATED)
    public BadgeResponse createBadge(@Valid @RequestBody BadgeUpsert request) {
        return adminService.createBadge(request);
    }

    @PutMapping("/badges/{id}")
    public BadgeResponse updateBadge(@PathVariable Long id, @Valid @RequestBody BadgeUpsert request) {
        return adminService.updateBadge(id, request);
    }

    @DeleteMapping("/badges/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBadge(@PathVariable Long id) {
        adminService.deleteBadge(id);
    }

    // ---- certificats

    @GetMapping("/certificates")
    public List<AdminCertificateView> listCertificates() {
        return adminService.listCertificates();
    }

    @PostMapping("/certificates/{id}/revoke")
    public AdminCertificateView revokeCertificate(@PathVariable UUID id) {
        return adminService.revokeCertificate(id);
    }

    // ---- joueurs et XP

    @GetMapping("/players")
    public List<AdminPlayerView> listPlayers(@RequestParam(defaultValue = "100") int limit) {
        return adminService.listPlayers(limit);
    }

    @PostMapping("/players/{userId}/xp")
    public ProfileResponse adjustXP(@PathVariable UUID userId, @Valid @RequestBody XpAdjustRequest request) {
        return adminService.adjustXP(userId, request.amount());
    }
}
