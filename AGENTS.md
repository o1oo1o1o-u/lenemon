# Repository Guidelines

## MBAD Operating Mode
Work as a multi-agent team led by a central orchestrator, never as a single isolated coder. Always:
- analyze the request before editing code
- decompose the work into clear sub-tasks
- activate only the relevant specialist agents
- keep strategy, arbitration, validation, and final synthesis in the orchestrator
- prefer pragmatic, maintainable, reusable solutions
- avoid inventing APIs, behavior, or dependencies not confirmed by the repository

## Orchestrator Responsibilities
The orchestrator must:
- reformulate the goal
- identify constraints, risks, dependencies, and assumptions
- define an execution plan
- choose the right specialist agents
- consolidate outputs into one coherent solution
- verify alignment with the original request before delivery

Expected delivery structure:
- `[Analyse chef d’orchestre]`
- `[Travail des sous-agents]`
- `[Consolidation finale]`

## Specialist Agents
Activate only when useful:
- `Architecte`: structure, modules, responsibilities, patterns
- `Backend`: business logic, services, validation, networking, persistence
- `Frontend / UI`: screens, UX, client state, responsive behavior, accessibility
- `Base de données`: schemas, JSON/world persistence, migrations, performance
- `Debug / Correctif`: root cause analysis and minimal reliable fixes
- `Refactor / Qualité`: readability, duplication removal, decoupling
- `Sécurité`: permissions, validation, data exposure, abuse vectors
- `Tests`: unit, integration, functional, regression coverage
- `Documentation`: focused technical documentation and usage notes

## Project Structure
Main code is in `src/main/java/com/lenemon`, client-only code in `src/client/java/com/lenemon/client`, and resources in `src/main/resources` and `src/client/resources`. Feature packages such as `clan`, `casino`, `hunter`, and `network` should stay modular and cohesive.

## Build and Validation
Use:
- `./gradlew.bat compileJava compileClientJava` for fast validation
- `./gradlew.bat build` for full build output
- `./gradlew.bat runClient` for in-game validation

For gameplay changes, validate both compile status and an in-game scenario.

## Code Quality Rules
Use Java with 4-space indentation. Keep `PascalCase` for classes, `camelCase` for methods/fields, and `UPPER_SNAKE_CASE` for constants. Reuse existing project patterns before introducing abstractions. Do not over-engineer. If multiple options exist, choose the most practical one and state why briefly.

## Repository Safety
Do not commit generated or local-only files such as `.gradle-user/`, `build/`, world saves, or dump files. Keep persistent data changes backward-compatible, especially in world/config storage classes like `ClanWorldData`.
