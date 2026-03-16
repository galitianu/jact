# Jact

**Jact** (read: **jay-act**) is an in-progress Java framework for building **reactive desktop UI** on top of **Spring** applications.

The goal is to make desktop UI development feel closer to a component-driven workflow, with automatic UI updates when state changes.

## Project Status

Jact is currently in the **planning and architecture phase** (pre-alpha).

- Repository website: [jact.io](https://jact.io)
- Organization: **146 Industries**
- Active planning document: [Implementation Plan](./implementation-plan.md)

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

The current v1 direction focuses on:

- Java 21 + Spring Boot 3.x baseline
- JavaFX renderer with direct binding updates (no virtual DOM)
- `@JactComponent` and `@JactPage` annotations
- Compile-time annotation processing for discovery and validation
- A Task Manager reference app for end-to-end validation

For full details and milestone dates, see [implementation-plan.md](./implementation-plan.md).

## 3-Month Roadmap Snapshot

- **M1**: Foundations (multi-module setup, annotations, bootstrap path)
- **M2**: Runtime and routing core (state hooks, direct updates, route handling)
- **M3**: Spring integration and validation app
- **M4**: Stabilization, evaluation runs, and thesis draft packaging

Target planning window: **March 16, 2026 - June 16, 2026**

## Thesis Context

This project is also the practical foundation for my master thesis.
The thesis will document architecture decisions, implementation, and comparative evaluation against a JavaFX MVC + FXML baseline.

## Contributing

As implementation starts, contribution guidelines and issue templates will be added.
Until then, feedback on architecture and API design is welcome.
