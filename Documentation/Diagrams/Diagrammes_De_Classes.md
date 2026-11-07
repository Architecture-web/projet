# Diagrammes de Classes UML – Microservices SkillQuest

Ce document détaille les modèles de classes internes pour chacun des microservices du projet, requis pour la présentation individuelle de la séance 4.

---

## 1. Service Commun : Auth & Identity Service (PostgreSQL)

```mermaid
classDiagram
    class User {
        +UUID id
        +String email
        +String passwordHash
        +String firstName
        +String lastName
        +Boolean isActive
        +DateTime createdAt
        +register()
        +validateCredentials()
    }

    class Role {
        +Long id
        +String name
        +String description
    }

    class RefreshToken {
        +UUID id
        +String token
        +DateTime expiryDate
        +Boolean isRevoked
        +isExpired() Boolean
    }

    class UserRole {
        +UUID userId
        +Long roleId
        +DateTime assignedAt
    }

    User "1" --> "*" RefreshToken : possède
    User "1" --> "*" UserRole : possède
    Role "1" --> "*" UserRole : définit
```

---

## 2. Microservice 1 : Course & Learning Content Service (MongoDB)

```mermaid
classDiagram
    class Course {
        +String id
        +String title
        +String slug
        +String description
        +DifficultyLevel level
        +String instructorId
        +Boolean isPublished
        +List~Module~ modules
    }

    class Module {
        +String moduleId
        +String title
        +Integer orderIndex
        +List~Lesson~ lessons
    }

    class Lesson {
        +String lessonId
        +String title
        +LessonType type
        +String markdownContent
        +String videoUrl
        +Integer durationMinutes
    }

    class UserEnrollment {
        +String id
        +String userId
        +String courseId
        +Integer completionPercentage
        +DateTime lastAccessedAt
        +List~String~ completedLessonIds
        +updateProgress(lessonId)
    }

    Course *-- Module : contient
    Module *-- Lesson : contient
    UserEnrollment ..> Course : référence
```

---

## 3. Microservice 2 : Code Execution & Sandbox Service (Redis + PostgreSQL)

```mermaid
classDiagram
    class Submission {
        +UUID id
        +String userId
        +String exerciseId
        +ProgrammingLanguage language
        +String sourceCode
        +SubmissionStatus status
        +Integer executionTimeMs
        +Integer memoryUsedKb
        +DateTime submittedAt
    }

    class TestCase {
        +UUID id
        +String exerciseId
        +String input
        +String expectedOutput
        +Boolean isHidden
        +Integer timeoutMs
    }

    class ExecutionResult {
        +UUID testCaseId
        +Boolean passed
        +String actualOutput
        +String errorMessage
    }

    Submission "1" *-- "*" ExecutionResult : produit
    TestCase ..> ExecutionResult : validé contre
```

---

## 4. Microservice 3 : Gamification & Certification Service (PostgreSQL)

```mermaid
classDiagram
    class UserProfileGamification {
        +UUID userId
        +Integer totalXP
        +Integer currentLevel
        +Integer rankPosition
        +addXP(amount)
        +checkLevelUp()
    }

    class Badge {
        +Long id
        +String codeName
        +String displayName
        +String iconUrl
        +Integer requiredXP
    }

    class UserBadge {
        +UUID userId
        +Long badgeId
        +DateTime unlockedAt
    }

    class Certificate {
        +UUID id
        +UUID userId
        +String courseId
        +String certificateHash
        +DateTime issuedAt
        +verifyHash(hash) Boolean
    }

    UserProfileGamification "1" --> "*" UserBadge : détient
    Badge "1" --> "*" UserBadge : attribué via
    UserProfileGamification "1" --> "*" Certificate : certifié par
```

---

## 5. Microservice 4 : Job Board & Recruitment Service (MongoDB)

```mermaid
classDiagram
    class JobPosting {
        +String id
        +String recruiterId
        +String companyName
        +String title
        +String location
        +EmploymentType type
        +List~String~ requiredSkills
        +Integer minimumRequiredXP
        +Boolean isOpen
    }

    class JobApplication {
        +String id
        +String jobPostingId
        +String applicantId
        +ApplicationStatus status
        +Integer verifiedScore
        +DateTime appliedAt
        +updateStatus(status)
    }

    JobPosting "1" --> "*" JobApplication : reçoit
```

---

## 6. Microservice 5 : Community & Peer-Review Service (MongoDB)

```mermaid
classDiagram
    class ForumThread {
        +String id
        +String exerciseId
        +String authorId
        +String title
        +String content
        +Integer upvotesCount
        +DateTime createdAt
    }

    class Comment {
        +String id
        +String threadId
        +String parentCommentId
        +String authorId
        +String content
        +Integer score
        +DateTime createdAt
    }

    class CodeReviewShare {
        +String id
        +String exerciseId
        +String authorId
        +String cleanCodeSnippet
        +String explanationMarkdown
        +Integer likesCount
    }

    ForumThread "1" *-- "*" Comment : fil de discussion
    Comment "1" *-- "*" Comment : réponses imbriquées
```
