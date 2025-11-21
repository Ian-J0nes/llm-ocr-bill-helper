# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Backend (Spring Boot)

The backend is a Spring Boot application built with Maven, using Java 17. It heavily features **Spring AI** for intelligent functionalities.

### Commands

- **Build:** `./mvnw clean install`
- **Run:** `./mvnw spring-boot:run`
- **Test:** `./mvnw test`
- **Run a single test:** `./mvnw test -Dtest=TestClassName#testMethodName`

### Architecture

The backend follows a standard layered architecture with a strong emphasis on AI integration:

- **`config`**: Contains application configuration. This includes core setup like `SecurityConfig` and `WebMvcConfig`, as well as type-safe properties classes in `config/properties` for managing settings for the application, AI prompts, and external APIs.
- **`controller`**: Handles incoming HTTP requests. Controllers orchestrate calls to the service layer. AI-related endpoints are managed by controllers like `AIOChatController` and `AiInsightController`.
- **`service`**: Contains the business logic. The project uses an interface-based design (`service/` and `service/impl/`). A key component is `AIOrchestrationService`, which centralizes the core AI logic, integrating with OpenAI and a Redis vector store.
- **`mapper` / `entity`**: The project employs a dual data access strategy. **JPA entities** in the `entity` package are used for object-relational mapping, while **MyBatis mappers** in the `mapper` package are used for more direct SQL control.
- **`common`**: Contains utility classes, constants, and base classes. Key components include a generic `Result` class for API responses and various enums.
- **`scheduler`**: Contains scheduled tasks, such as `ScheduledTasks` for periodic jobs like fetching exchange rates.

## Frontend (WeChat Mini Program)

The frontend is a WeChat Mini Program.

### Commands

- **Run in simulator:** Use WeChat DevTools to import and run the `ai-bill-front` directory.
- **Build for production:** Use WeChat DevTools to upload the code.

### Architecture

The frontend is structured as follows:

- **`pages`**: Contains all the pages of the mini-program.
- **`utils`**: Contains utility functions, such as API requests (`api.js`) and user authentication (`login-helper.js`).
- **`static`**: Contains static assets like icons.
- **`app.js`**: The entry point of the application, responsible for global configurations.
- **`app.json`**: Global configuration for the mini-program, including pages, window style, and tab bar.
- **`project.config.json`**: Project configuration for the WeChat DevTools.

### TODO

- [ ] Analyze `config` package for simplification opportunities.
- [x] **Refactor `AIOChatController`**: Completed. The controller has been refactored to use `AIOrchestrationService`, which encapsulates the core AI logic, making the controller leaner and more maintainable.
- [x] **Analyze `common` package for simplification**: Completed. The package is generally well-structured. Key suggestions for improvement include consistently using Enums instead of constant classes (good), introducing declarative validation (e.g., using `@Valid`), and externalizing AI prompts (done). Further standardization of the generic `Result` class usage across all controllers could enhance consistency.
- [ ] **Add state management for the frontend**: The frontend currently lacks a centralized state management solution. Consider introducing a library like MobX or Redux to manage global state, such as user information and theme settings.
