# Xynerzy Studio API Server

[한국어로 보기](../README.md)

![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)
![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)
[![en](https://img.shields.io/badge/lang-en-red.svg)](../documents/README.en.md)
[![ko](https://img.shields.io/badge/lang-ko-blue.svg)](../README.md)

> Where Human and AI Minds Converge.

**Xynerzy Studio** (an anagram of *Synergy*) is an open-source platform for hosting multi-party chat conferences that include both human and multiple Large Language Model (LLM) participants. The primary goal is to create a seamless environment for collaborative intelligence, where AI agents can assist, contribute, and process information in real-time alongside human users.

## ✨ Core Features

- **Multi-Agent Conferencing**: Create chat rooms where multiple users and AI agents can interact in real-time.
- **Flexible LLM Integration**:
  - Connect to major cloud-based LLMs (e.g., Google Gemini, OpenAI's GPT) using multiple API keys.
  - Support for local LLMs, enabling fully offline or private network operation.
- **Information Processing**: Leverage AI agents to perform on-the-fly tasks such as summarization, translation, action item extraction, and data analysis directly within the conversation.
- **Scalable Architecture**: Designed with future scalability in mind, including plans for multi-server sharding to handle large-scale deployments.

## 🏛️ Architecture

Xynerzy Studio is composed of two main components:

1.  **`xynerzy-studio-api`**: The backend service responsible for:
  - Managing user sessions and authentication.
  - Handling real-time messaging via WebSockets.
  - Orchestrating interactions between users and various AI agents.
  - Processing and persisting conversation data.
  - (Technology: Java / Rust)

2.  **`xynerzy-studio-web`**: The frontend web client that provides:
  - A modern, intuitive user interface for chat rooms.
  - Tools for managing AI agents and their configurations.
  - Real-time rendering of conversations.
  - (Technology: Vue.js / React)

## 🛠️ Technology Stack

- **Backend**: Java (Spring Boot) / Rust (Actix/Rocket)
- **Frontend**: Vue.js / React
- **Real-time Communication**: WebSockets
- **Database**: PostgreSQL / MySQL (TBD)

## 🚀 Getting Started

*(This section will be updated as the project matures.)*

### Prerequisites

- Java JDK 17+ or Rust toolchain
- Node.js 18+
- Docker (optional)

### Backend (`xynerzy-studio-api`)

```bash
# Clone the repository
git clone https://github.com/your-username/xynerzy-studio-api.git
cd xynerzy-studio-api

# Build and run (example for Java/Maven)
./mvnw spring-boot:run
```

### Frontend (`xynerzy-studio-web`)

```bash
# Clone the repository
git clone https://github.com/your-username/xynerzy-studio-web.git
cd xynerzy-studio-web

# Install dependencies
npm install

# Start the development server
npm run dev
```

## 🗺️ Roadmap

- [x] Initial project setup and architecture design.
- [ ] Core: Real-time chat between human users.
- [ ] Integration: Connect the first cloud LLM (e.g., Gemini).
- [ ] Core: Enable multiple, swappable LLM agents in a single chat.
- [ ] Integration: Support for local LLMs via a standardized API.
- [ ] Feature: Advanced information processing commands (e.g., `/summarize`).
- [ ] Core: User authentication and authorization.
- [ ] **Future**: Implement multi-server sharding for high availability and scalability.

## ✨ Contributors

- Jaebaek Jeong (@lupfeliz) — Core maintainer
<!-- ALL-CONTRIBUTORS-LIST:START -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

We welcome contributions from everyone! If you're interested in helping, please feel free to:
- Open an issue to report a bug or suggest a new feature.
- Fork the repository and submit a pull request.
- Improve the documentation.

## 📄 License

This project is licensed under the **Apache License 2.0**. See the `LICENSE` file for more details.
