package com.skillquest.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Badge du catalogue. {@code requiredXP} est {@code null} pour un badge attribué sur événement (ex. cours terminé). */
@Entity
@Table(name = "badge")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_name", nullable = false, unique = true, length = 64)
    private String codeName;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "required_xp")
    private Integer requiredXP;

    protected Badge() {
    }

    public Badge(String codeName, String title, String description, String iconUrl, Integer requiredXP) {
        this.codeName = codeName;
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
        this.requiredXP = requiredXP;
    }

    public Long getId() {
        return id;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public Integer getRequiredXP() {
        return requiredXP;
    }
}
