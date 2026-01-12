# Yellow Tale & Pond - Hytale Platform

## Overview

Yellow Tale & Pond is a comprehensive platform designed for the Hytale gaming community. It aims to provide Hytale players and server operators with a robust set of tools and services. The platform consists of a high-performance desktop launcher (Yellow Tale), a central API server, a marketing and account management website, and a modular server-side platform for Hytale servers (Pond). The business vision is to be the leading third-party platform for Hytale, offering essential functionalities like seamless game launching, social features, server management, and performance optimization, thereby enhancing the overall Hytale experience for its users.

## User Preferences

I want iterative development and prefer detailed explanations of the code. I like functional programming paradigms where appropriate. Please ask before making any major architectural changes or introducing new external dependencies. Do not make changes to the `replit.md` file itself.

## System Architecture

The platform is composed of four main components:

1.  **Yellow Tale (Desktop Application)**: A native launcher built with Rust and Tauri for Windows, focusing on performance optimization and user experience. It includes features like game process lifecycle management, profile management, mod orchestration, content caching, and system diagnostics. The UI features a sidebar navigation for Home, Servers, Friends, Mods, and Settings, and supports login/logout, server browsing, and performance settings. It is designed to treat the game as a black-box executable, avoiding memory injection or patching, focusing on legal pre-launch optimizations.
2.  **Central API Server**: A backend service developed in Rust using the Axum framework. It handles core functionalities such as user authentication (using Argon2 for password hashing), friends system, premium feature management, server browsing, game statistics, mod profile management, and subscription services via Stripe. It uses PostgreSQL as its primary database.
3.  **Website**: A React 18 application with TypeScript and Vite, serving as the marketing front, download hub, and account management portal. It provides pages for home, downloads, features, server browser, premium subscriptions, login, and a user dashboard.
4.  **Pond (Server-side Platform)**: A modular Rust platform for Hytale servers, akin to Paper for Minecraft. It features a hot-reloadable plugin system, a priority-based scheduler, real-time performance monitoring, an asset registry, and deep integration with Yellow Tale for features like queue management, friend activity sync, and session transfer.

**Technical Implementations:**

*   **Desktop (Yellow Tale)**: Rust, Tokio for async, `reqwest` for HTTP, Serde for serialization, Tracing for logging, `sysinfo` for system info.
*   **Central Server**: Axum, PostgreSQL (via `sqlx`), Argon2 for hashing, Axum's built-in WebSocket support, `tower-http` for CORS.
*   **Website**: React 18, TypeScript, Vite, `react-router-dom`, `lucide-react` for icons. Styling is handled with custom CSS.
*   **Pond**: Rust.

**Core Design Principles:**

*   **Modular Architecture**: Designed for rapid integration with Hytale's official API when available, utilizing traits like `GameAdapter`, `GameProtocol`, `AssetLoader` for abstraction.
*   **Feature Toggling**: A runtime `FeatureManager` allows features to be toggled based on `premium` status or `game_api` availability, with a `when_feature!` macro for conditional code.
*   **Enhanced Relay System**: Supports NAT traversal (ICE, STUN, TURN), password-protected sessions, regional relay selection, and various connection methods (UDP, TCP, QUIC, WebRTC readiness).
*   **Security**: Session tokens are SHA-256 hashed, passwords are Argon2 hashed, and all authentication flows use hashed token comparison. CORS is configured for secure cross-origin requests.

## External Dependencies

*   **Database**: PostgreSQL
*   **Payment Gateway**: Stripe (for subscriptions)
*   **STUN Servers**: Google STUN servers (for NAT traversal in relay system)
*   **Hytale**: The game itself, which Yellow Tale interacts with as a black-box executable.
*   **Operating System**: Windows for the native desktop application.