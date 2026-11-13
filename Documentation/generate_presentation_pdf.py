#!/usr/bin/env python3
"""
SkillQuest: System Architecture, Vision & UI/UX Specification Generator.
Generates an executive, publication-grade PDF without vendor/technology names in the architecture.
"""

import os
import sys
from builder import DocumentBuilder

def generate_pdf():
    doc = DocumentBuilder(filename="/tmp/SkillQuest_Spec.ps")
    output_pdf = "/home/sabriii/WEB_dist/Documentation/SkillQuest_Project_Architecture_UIUX.pdf"

    # =========================================================================
    # PAGE 1: EXECUTIVE SUMMARY, CORE MISSION & ECOSYSTEM PERSONAS
    # =========================================================================
    doc.new_page()

    doc.add_title_banner(
        title="SkillQuest: Next-Gen Technical Upskilling & Sourcing",
        subtitle="Comprehensive Architectural Model, UI/UX Philosophy & Interactive Templates",
        badge="ENTERPRISE PLATFORM SPECIFICATION"
    )

    doc.add_section_header("1. Project Vision & Problem Statement", icon="*")

    doc.add_paragraph(
        "SkillQuest is an enterprise-grade technical learning and talent assessment platform designed to bridge the structural divide between theoretical education and real-world software engineering practice. Modern engineering ecosystems require verifiable, hands-on problem-solving capabilities, yet conventional e-learning systems remain passive, reliant on pre-recorded lectures and unverified multiple-choice quizzes.",
        size=8.5, line_height=12
    )
    doc.add_paragraph(
        "Simultaneously, technology recruiters and hiring organizations face high costs and friction attempting to validate candidate claims on static resumes. SkillQuest resolves this asymmetry by combining an interactive, sandboxed coding environment with a gamified progression model, generating verifiable algorithmic and architectural proof-of-work profiles directly consumable by enterprise talent acquisition teams.",
        size=8.5, line_height=12
    )

    doc.add_callout(
        title="CORE VALUE PROPOSITION: DUAL-SIDED TALENT ENGINE",
        text="For Learners: Active, gamified mission tracks with instant execution feedback, verifiable skill badges, and algorithmic XP. For Recruiters: An objective talent sourcing portal providing tamper-proof code execution metrics, benchmark percentiles, and validated candidate competencies.",
        r_bg=0.94, g_bg=0.96, b_bg=1.0, r_border=0.15, g_border=0.4, b_border=0.9
    )

    doc.add_subsection_header("Primary Ecosystem Personas & Stakeholder Value")

    doc.add_card_grid([
        {
            "tag": "LEARNER / STUDENT",
            "title": "Practical Skill Mastery",
            "desc": "Hands-on coding challenges executed in isolated ephemeral sandboxes. Progression measured via dynamic XP curves, streak bonuses, verified milestone badges, and skill matrix portfolios.",
            "color": (0.1, 0.5, 0.9)
        },
        {
            "tag": "RECRUITER / ENTERPRISE",
            "title": "Objective Talent Discovery",
            "desc": "Searchable candidate pool filtered by empirically verified capabilities, sandbox test scores, and algorithmic complexity metrics rather than unverified CV credentials.",
            "color": (0.05, 0.65, 0.45)
        },
        {
            "tag": "INSTRUCTOR / MENTOR",
            "title": "Curriculum Authoring",
            "desc": "Syllabus design studio with automated unit test harnesses, tiered difficulty constraints, dynamic evaluation fixtures, and real-time student cohort telemetry.",
            "color": (0.6, 0.3, 0.85)
        }
    ])

    doc.add_subsection_header("Key Architectural Differentiators")
    doc.add_bullet("Zero-Installation Execution:", "Learners solve complex programming tasks directly in the browser through secure, server-side sandboxed execution runtimes.")
    doc.add_bullet("Behavioral Gamification Engine:", "Intrinsic motivation reinforced through level tiers, achievement unlock notifications, quest milestones, and skill radar charting.")
    doc.add_bullet("Verified Talent Certification:", "Candidate profiles export cryptographic certificates with immutable audit trails of solved problems and benchmark percentiles.")

    # =========================================================================
    # PAGE 2: TECHNOLOGY-NEUTRAL SYSTEM ARCHITECTURE
    # =========================================================================
    doc.new_page()

    doc.add_section_header("2. Technology-Agnostic System Architecture", icon="*")

    doc.add_paragraph(
        "The SkillQuest platform is designed around a distributed, modular microservices topology prioritizing high fault isolation, independent scaling, and zero-trust service boundaries. To maintain strict architectural purity and vendor independence, the system decomposes responsibilities into dedicated domain-driven bounded contexts.",
        size=8.5, line_height=12
    )

    doc.add_callout(
        title="ARCHITECTURAL DESIGN MANDATE: DECOUPLED SCALABILITY",
        text="The system strictly isolates interactive web traffic, CPU-intensive code execution, transactional identity flows, and asynchronous grading pipelines to ensure that burst code compilation spikes never degrade student browsing or recruiter search responsiveness.",
        r_bg=0.95, g_bg=0.98, b_bg=0.95, r_border=0.1, g_border=0.6, b_border=0.3
    )

    doc.add_subsection_header("Functional Decomposition & Domain Microservices")

    doc.add_card_grid([
        {
            "tag": "SECURITY & ACCESS",
            "title": "Identity & Access Gateway",
            "desc": "Centralized ingress proxy handling SSL termination, rate-limiting, edge CORS, token verification, and federated external identity provider handshakes.",
            "color": (0.15, 0.45, 0.95)
        },
        {
            "tag": "DOMAIN SERVICE",
            "title": "User & Profile Service",
            "desc": "Authoritative master for account records, role hierarchies, public developer portfolios, recruiter company profiles, and audit log histories.",
            "color": (0.2, 0.55, 0.8)
        },
        {
            "tag": "KNOWLEDGE DOMAIN",
            "title": "Catalog & Curriculum Service",
            "desc": "Hierarchical taxonomy of learning tracks, missions, theory documents, starter templates, and verified test fixture definitions.",
            "color": (0.55, 0.25, 0.8)
        }
    ])

    doc.add_card_grid([
        {
            "tag": "EXECUTION ENGINE",
            "title": "Isolated Sandbox Runner",
            "desc": "Stateless worker fleet orchestrating ephemeral, isolated runtime environments with strict kernel-level resource caps, CPU limits, memory quotas, and air-gapped networking.",
            "color": (0.9, 0.35, 0.1)
        },
        {
            "tag": "EVALUATION DOMAIN",
            "title": "Assessment & Scoring Service",
            "desc": "Automated test harness evaluator analyzing standard output, execution duration, algorithmic complexity, memory footprints, and unit test pass rates.",
            "color": (0.05, 0.65, 0.45)
        },
        {
            "tag": "ENGAGEMENT DOMAIN",
            "title": "Gamification Engine",
            "desc": "Event-driven state machine computing experience points (XP), dynamic streak tracking, milestone badge awards, and real-time cohort leaderboards.",
            "color": (0.85, 0.6, 0.1)
        }
    ])

    doc.add_subsection_header("Inter-Service Communication Protocols & Patterns")
    doc.add_bullet("Unified Edge API Gateway:", "Acts as the singular reverse proxy entry point for all client requests, performing ingress validation, claims verification, and dynamic upstream routing.")
    doc.add_bullet("Synchronous Request-Reply RPC Contracts:", "High-frequency operational reads (e.g. catalog queries, profile lookups, auth verification) utilize low-latency synchronous request-response contracts.")
    doc.add_bullet("Decoupled Asynchronous Event Bus:", "State mutations trigger asynchronous domain events (e.g., CodeExecutedEvent, MissionCompletedEvent, BadgeEarnedEvent). Downstream consumers (Gamification, Notifications, Recruiter Sourcing) react independently without coupling.")

    # =========================================================================
    # PAGE 3: PERSISTENCE, RUNTIME SANDBOX & SECURITY MODEL
    # =========================================================================
    doc.new_page()

    doc.add_section_header("3. Data Topology, Sandbox Execution & Security", icon="*")

    doc.add_paragraph(
        "A robust technical platform requires specialized storage models tailored to varying access patterns and write semantics. SkillQuest implements a Polyglot Persistence strategy to optimize ACID transactional guarantees alongside unstructured document flexibility and high-throughput cache lookups.",
        size=8.5, line_height=12
    )

    doc.add_subsection_header("Polyglot Persistence Layer Topology")

    doc.add_bullet("Relational Transaction Store:", "Guarantees strict ACID integrity for identity records, role assignments, financial billing, enterprise subscriptions, and institutional access authorizations.")
    doc.add_bullet("Document-Oriented Knowledge Store:", "Houses semi-structured, polymorphic curriculum schemas, interactive challenge descriptions, code boilerplates, multi-file unit tests, and candidate submission archives.")
    doc.add_bullet("In-Memory Distributed State Cache:", "Provides sub-millisecond retrieval of distributed session tokens, real-time rate limit tracking windows, active streak counters, and live global leaderboards.")

    doc.add_subsection_header("Isolated Code Execution Pipeline Architecture")
    doc.add_paragraph(
        "Allowing untrusted, arbitrary user code execution introduces severe security vectors. The platform's execution architecture adheres to military-grade defense-in-depth isolation principles:",
        size=8.5, line_height=12
    )

    doc.add_callout(
        title="EPHEMERAL EXECUTION FLOW & SECURITY GATES",
        text="Client Submission -> Edge Gateway -> Job Queue -> Ephemeral Sandbox Worker -> Strict Resource Quarantine (CPU Throttling, Memory Limit, No Network Ingress/Egress, Restricted Syscall Profile) -> Execution & Unit Test Verification -> Scored Output -> Event Bus -> State Update.",
        r_bg=0.98, g_bg=0.95, b_bg=0.95, r_border=0.9, g_border=0.2, b_border=0.2
    )

    doc.add_subsection_header("Comprehensive Security & Federated Trust Model")
    doc.add_bullet("Federated Single Sign-On (SSO):", "Standards-compliant authorization code exchange with major social and enterprise identity providers (e.g. Google Workspace, GitHub Enterprise). Simplifies developer friction while delegating credential confidentiality.")
    doc.add_bullet("Stateless Cryptographic Token Exchange:", "Short-lived signed authentication tokens carry user context and scope permissions, enabling horizontal gateway scaling without sticky server affinity.")
    doc.add_bullet("Zero-Trust Internal Network Boundary:", "All intra-cluster communication enforces mutual authentication and strict service-to-service role authorizations.")
    doc.add_bullet("Granular Role-Based Access Control (RBAC):", "Strict segregation of duties across Student, Recruiter, Instructor, and Platform Administrator personas.")

    # =========================================================================
    # PAGE 4: UI / UX DESIGN SYSTEM & PHILOSOPHY
    # =========================================================================
    doc.new_page()

    doc.add_section_header("4. UI/UX Design System & Ergonomic Philosophy", icon="*")

    doc.add_paragraph(
        "SkillQuest's user interface is purposefully engineered for software practitioners and enterprise talent scouts. The design system balances intense developer immersion with crisp executive clarity, minimizing cognitive friction across deep coding sessions and high-throughput recruitment searches.",
        size=8.5, line_height=12
    )

    doc.add_subsection_header("Core Design System Pillars")

    doc.add_card_grid([
        {
            "tag": "VISUAL SYSTEM",
            "title": "Modern Dark-First Canvas",
            "desc": "Ultra-low eyestrain dark slate backdrop (#0B0F19, #1E293B) paired with high-contrast neon accents (Electric Cyan, Violet, Emerald Green) for immediate focus hierarchy.",
            "color": (0.2, 0.45, 0.95)
        },
        {
            "tag": "INFORMATION ARCHITECTURE",
            "title": "Cognitive Load Minimization",
            "desc": "Split-view workspaces, collapsible sidebars, and contextual hotkeys (Run Code, Submit Solution, Clear Console) maximize active editor screen real estate.",
            "color": (0.05, 0.65, 0.45)
        },
        {
            "tag": "BEHAVIORAL LOOPS",
            "title": "Real-Time Telemetry & XP",
            "desc": "Subtle micro-interactions: pulsing execution spinners, immediate test case outcome chips (Passed / Failed), XP gain toasts, and streak flame counters.",
            "color": (0.85, 0.5, 0.1)
        }
    ])

    doc.add_subsection_header("Design Tokens & Typography Standards")
    doc.add_bullet("Typographic Scale:", "Clean modern sans-serif for UI chrome, headings, and metrics; paired with tabular monospaced typography for code blocks, terminal outputs, and execution telemetry.")
    doc.add_bullet("Semantic Color Harmony:", "Emerald (#10B981) for successful test executions; Amber (#F59E0B) for warnings and runtime timeouts; Rose (#EF4444) for syntax errors and test failures; Indigo (#6366F1) for active focus and primary action triggers.")
    doc.add_bullet("Tactile Micro-Interactions:", "Hover state depth shifts, smooth progress bar easing, zero-layout-shift skeleton loaders, and non-blocking notification drawers.")

    doc.add_subsection_header("Responsive Multi-Device Layout Strategy")
    doc.add_bullet("Desktop Workspace (>= 1200px):", "Full multi-column split view (Challenge Spec | Syntax Editor | Test Console), enabling simultaneous reading, coding, and debugging.")
    doc.add_bullet("Tablet Adaptation (768px - 1199px):", "Tabbed studio interface allowing quick gestures between problem instructions and editor workspace.")
    doc.add_bullet("Mobile Companion (< 768px):", "Card-based review mode, syllabus tracking, interview preparation flashcards, and community discussions with touch-optimized controls.")

    # =========================================================================
    # PAGE 5: DETAILED TEMPLATE SPECIFICATIONS
    # =========================================================================
    doc.new_page()

    doc.add_section_header("5. Key Platform Templates & User Journeys", icon="*")

    doc.add_paragraph(
        "The SkillQuest experience is realized through five core structural templates, each engineered for specialized user intents and high conversion efficiency.",
        size=8.5, line_height=12
    )

    doc.add_subsection_header("Template 1: Public Landing Page & Dynamic Course Catalog")
    doc.add_bullet("Hero Section & Real-Time Demo:", "Engaging hero banner communicating the core dual value proposition with dual CTAs ('Start Questing' vs 'Hire Verified Talent') alongside an interactive mini-sandbox simulator.")
    doc.add_bullet("Track Discovery Matrix:", "Filterable grid of engineering domains (Fullstack, Cloud Architecture, Algorithms, Security) displaying difficulty badges, estimated hours, and XP bounties.")
    doc.add_bullet("Ecosystem Trust & Social Proof:", "Live counter displaying certified learners, problems solved, and partner hiring organizations.")

    doc.add_subsection_header("Template 2: Federated Identity & Onboarding Modal")
    doc.add_bullet("Unified Single Sign-On:", "One-click authentication via verified identity providers (Google, GitHub) eliminating password friction, alongside standard email fallback.")
    doc.add_bullet("Persona Role Selector:", "Interactive onboarding split configuring the profile as either an 'Ambitious Engineer' or 'Tech Recruiter', dynamically initializing respective dashboard modules.")

    doc.add_subsection_header("Template 3: Interactive Sandbox Studio (Tri-Pane Layout)")
    doc.add_bullet("Left Pane (Problem Specification):", "Markdown-rendered problem description, structural inputs/outputs, algorithmic constraints, edge-case hints, and difficulty ratings.")
    doc.add_bullet("Center Pane (Editor Workspace):", "Multi-tab code editor with syntax highlighting, line numbers, automatic indentation, reset boilerplate, and keyboard shortcuts.")
    doc.add_bullet("Right / Bottom Pane (Execution Terminal):", "Live console displaying standard stdout, execution runtime in milliseconds, peak memory usage, and unit test pass/fail breakdown.")

    doc.add_subsection_header("Template 4: Student Mastery & Progression Dashboard")
    doc.add_bullet("Hero Metric Banner:", "Current Level rank badge, total accumulated XP, next-level progress bar, and consecutive day streak flame counter.")
    doc.add_bullet("Interactive Competency Radar:", "Multilateral radar chart quantifying proficiency across Algorithms, Backend Systems, Database Optimization, and System Design.")
    doc.add_bullet("Recruiter Visibility Tracker:", "Analytics widget alerting students when enterprise recruiters inspect their verified challenge solutions.")

    doc.add_subsection_header("Template 5: Recruiter Candidate Intelligence Portal")
    doc.add_bullet("Talent Sourcing Filter Matrix:", "Multi-faceted search querying candidate availability, verified sandbox scores, primary languages, and algorithmic percentiles.")
    doc.add_bullet("Comprehensive Candidate Dossier:", "Drill-down view of candidate proof-of-work: side-by-side code submission replay, test pass efficiency, and code cleanliness score.")
    doc.add_bullet("Custom Challenge Composer:", "Interface for recruiters to deploy custom coding missions and benchmark applicant pools under identical evaluation parameters.")

    # Final conclusion banner
    doc.add_callout(
        title="SUMMARY: A UNIFIED PARADIGM FOR TECHNICAL EDUCATION & HIRING",
        text="By harmonizing technology-agnostic microservice resilience, kernel-isolated sandbox execution, and an ergonomic developer-first design system, SkillQuest elevates computer science education from passive absorption into verifiable, merit-based career advancement.",
        r_bg=0.94, g_bg=0.97, b_bg=1.0, r_border=0.2, g_border=0.5, b_border=0.95
    )

    doc.compile(output_pdf)
    return output_pdf

if __name__ == "__main__":
    generate_pdf()

