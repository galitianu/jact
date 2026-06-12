# Jact

**Jact** (read: **jay-act**) is a thesis-oriented Java framework for building **reactive desktop UI** on top of **Spring** applications.

The goal is to make desktop UI development feel closer to a component-driven workflow, with automatic UI updates when state changes.

## Project Status

Jact is currently a **thesis MVP**. It includes the core reactive runtime, annotation processing, Spring Boot starter integration, JavaFX rendering, a JACT Task Manager sample, and a JavaFX MVC/FXML baseline for evaluation.

- Repository website: [jact.io](https://jact.io)
- Organization: **146 Industries**
- Implementation notes: [Thesis Implementation Notes](./docs/thesis-implementation-notes.md)
- Evaluation package: [evaluation](./evaluation)

## Vision

Jact aims to provide:

- A Spring-friendly desktop runtime (single JVM model)
- Declarative, method-based UI components and pages
- Reactive local state primitives (hooks-style API)
- Optional global store integration
- File/package-based routing with dynamic segments

## Why Jact

Desktop Java UI development is still often split between imperative view logic and backend code.
Jact is designed to reduce that split by introducing a reactive component model that integrates naturally with Spring dependency injection and application structure.

## Current Scope (v1)

The current thesis MVP focuses on:

- Java 21 + Spring Boot 3.x baseline
- JavaFX renderer with direct binding updates (no virtual DOM)
- `@JactComponent` and `@JactPage` annotations
- Compile-time annotation processing for discovery and validation
- A Task Manager reference app for end-to-end validation
- A JavaFX MVC/FXML baseline for comparison

For the original milestone plan, see [implementation-plan.md](./implementation-plan.md).

## 3-Month Roadmap Snapshot

- **M1**: Foundations (multi-module setup, annotations, bootstrap path)
- **M2**: Runtime and routing core (state hooks, direct updates, route handling)
- **M3**: Spring integration and validation app
- **M4**: Stabilization, evaluation runs, and thesis draft packaging

Target planning window: **March 16, 2026 - June 16, 2026**

## Thesis Context

This project is the practical foundation for the master thesis **PROIECTAREA SI IMPLEMENTAREA UNUI FRAMEWORK REACTIV PENTRU INTERFETE DESKTOP JAVA**.
The thesis documents architecture decisions, implementation, limitations, and comparative evaluation against a JavaFX MVC/FXML baseline.

## License

Licensed under the Apache License, Version 2.0.  
See [LICENSE](./LICENSE).

## Contributing

As implementation starts, contribution guidelines and issue templates will be added.
Until then, feedback on architecture and API design is welcome.
