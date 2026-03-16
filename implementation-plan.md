# JACT v1 Implementation Plan (Spring + Reactive JavaFX)

## Summary
`jact` is a Java framework/library for building reactive desktop UI in Spring applications using JavaFX.

The v1 target is a working MVP in 3 months, from March 16, 2026 to June 16, 2026, together with a thesis draft that documents architecture, implementation decisions, and evaluation results.

Core decisions:
- Java 21 and Spring Boot 3.x baseline.
- Single JVM deployment model (Spring + JavaFX in one process).
- Direct JavaFX binding updates (no virtual DOM).
- Method-based component/page definitions using `@JactComponent` and `@JactPage`.
- File/package-based routing with dynamic segments represented as `$param`.

## Milestone Timeline (12 Weeks)
### M1: Foundations (March 16, 2026 to April 5, 2026)
Detailed plan: [milestones/m1-foundations-plan.md](./milestones/m1-foundations-plan.md)

Deliverables:
- Gradle multi-module setup with initial modules:
  - `jact-annotations`
  - `jact-core`
  - `jact-compiler` (annotation processor)
  - `jact-javafx`
  - `jact-spring-boot-starter`
  - `samples-task-manager`
- Initial contracts and annotations: `@JactComponent`, `@JactPage`, `JNode`, hook context types.
- Minimal bootstrap path from Spring startup to JavaFX window.

Acceptance criteria:
- Full project builds in CI.
- Annotation processor runs and generates a basic registry.
- A minimal page can be mounted via Spring + JavaFX startup.

### M2: Runtime and Routing Core (April 6, 2026 to April 26, 2026)
Detailed plan: [milestones/m2-runtime-routing-plan.md](./milestones/m2-runtime-routing-plan.md)

Deliverables:
- Hook runtime: `useState`, `useEffect`, `useMemo`.
- Retained node graph with direct JavaFX property/event bindings.
- File/package-based route derivation with `$param` support.
- Navigation API with parameter extraction and path substitution.

Acceptance criteria:
- State changes update only affected UI regions.
- Route templates resolve correctly (example: `/users/$id` with `id=42` resolves to `/users/42`).
- Compiler diagnostics are available for invalid annotated method signatures and duplicate routes.

### M3: Integration and Validation App (April 27, 2026 to May 17, 2026)
Detailed plan: [milestones/m3-integration-validation-plan.md](./milestones/m3-integration-validation-plan.md)

Deliverables:
- Spring Boot starter auto-configuration and runtime integration.
- Optional global store API integrated with hooks.
- Reactive service-state bridge so independently-changing Spring service values can trigger UI updates automatically.
- `samples-task-manager` reference app with:
  - task list and detail pages,
  - create/edit/delete flows,
  - filtering/search,
  - routed detail navigation.

Acceptance criteria:
- End-to-end flows run through JACT runtime, router, and Spring integration.
- Integration test suite covers render lifecycle, event-to-state updates, and navigation.
- A UI component subscribed to independently-changing service state updates automatically without manual refresh.
- v1 API surface is frozen at milestone end.

### M4: Stabilization and Thesis Packaging (May 18, 2026 to June 16, 2026)
Detailed plan: [milestones/m4-stabilization-thesis-plan.md](./milestones/m4-stabilization-thesis-plan.md)

Deliverables:
- Stabilization phase with bug fixing and API/documentation hardening.
- Final 2-week buffer (June 2, 2026 to June 16, 2026) reserved for risk, regressions, and polish.
- Thesis draft package including architecture, implementation, evaluation, and limitations.

Acceptance criteria:
- No open critical bugs in MVP scope.
- Reproducible evaluation runs are complete.
- Thesis draft is submission-ready for supervisor review.

## Implementation Changes
1. Module and build structure
- Use Gradle multi-module organization with clear ownership:
  - `jact-annotations`: public annotations and lightweight metadata interfaces.
  - `jact-compiler`: annotation processor and generated registry/binding code.
  - `jact-core`: hook engine, component lifecycle, scheduler, and runtime contracts.
  - `jact-javafx`: JavaFX renderer, node adapters, and update/binding integration.
  - `jact-spring-boot-starter`: Spring Boot auto-configuration and bootstrap wiring.
  - `samples-task-manager`: validation application for integration and evaluation.
- Publish coordinates under `io.jact:*` (for example `io.jact:jact-core`).
- Keep module boundaries strict: no JavaFX dependency in `jact-core`, no Spring dependency in renderer/core modules.

2. Compile-time code generation (`jact-compiler`)
- Process `@JactPage` and `@JactComponent` declarations at compile time.
- Generate deterministic registries for:
  - discovered pages/components,
  - route templates derived from package/class naming,
  - method signatures and invocation metadata used by runtime wiring.
- Validate compile-time constraints:
  - duplicate route collisions,
  - invalid component/page method signatures,
  - unsupported parameter types.
- Fail fast on invalid definitions with actionable diagnostics (file/method-level messages).

3. Component and page programming model
- Use `@JactComponent` and `@JactPage` to avoid naming collision with Spring `@Component`.
- Components/pages are annotated methods inside Spring-managed beans.
- Methods return `JNode` (or compatible UI node contract) and can receive supported contextual parameters (props, route params, runtime context).
- Runtime manages component instances, hook slot mapping, render-cycle identity, and cleanup semantics.
- Page discovery is convention-driven through configured root package(s), with generated metadata as the source of truth at runtime.

4. Reactive runtime internals (`jact-core`)
- Implement hook primitives for local state and side effects: `useState`, `useEffect`, `useMemo`.
- Preserve hook call-order stability per component render identity.
- Introduce a scheduler that batches state changes and dispatches render/update tasks on UI-safe execution context.
- Provide effect lifecycle guarantees:
  - setup after render commit,
  - cleanup before re-run/unmount.
- Add optional global store integration (`Store<T>`) with selector/subscription support and hook bridge APIs.
- Add service-state subscription bridge APIs so external state sources can notify the runtime and trigger re-render paths.

5. JavaFX renderer and update pipeline (`jact-javafx`)
- Maintain a retained UI representation linked to JavaFX nodes.
- Bind component output to JavaFX controls/properties/events without virtual DOM reconciliation.
- On state changes, compute targeted update scope from runtime metadata and apply minimal node/property updates.
- Guarantee JavaFX thread safety for mount/update/unmount operations.
- Support event-to-state flow (JavaFX event -> handler -> state update -> scheduled UI update).

6. Routing model and navigation behavior
- Derive route templates from package/class naming conventions.
- Represent dynamic segments with `$param` syntax in templates (example: `/users/$id`).
- Provide runtime substitution to concrete paths (example: template `/users/$id` + `id=42` -> `/users/42`).
- Expose parameter extraction/parsing and typed access helpers through routing APIs.
- Implement navigation service with push/replace/back-forward semantics and route transition lifecycle hooks.

7. Spring Boot integration (`jact-spring-boot-starter`)
- Auto-configure JACT runtime, generated registries, navigator, and renderer bridge.
- Bootstrap sequence:
  - Spring application context initialization,
  - JACT runtime initialization,
  - JavaFX stage creation and root page mount.
- Support dependency injection into component/page bean methods through standard Spring lifecycle.
- Provide integration helpers for Spring-managed services that publish independent state updates.
- Provide configurable startup properties (`JactProperties`): pages package roots, initial route, and window settings.

8. Validation app and implementation evidence
- Build `samples-task-manager` as canonical reference implementation for:
  - page routing, nested components, form/input handling, list rendering, and mutation flows.
- Use this app to validate API ergonomics and capture thesis evaluation evidence.
- Keep example scope aligned with MVP boundaries to avoid feature creep in milestone M3/M4.

## Public APIs / Interfaces
- Annotations: `@JactComponent`, `@JactPage`.
- Core types: `JNode`, `HookContext`, `State<T>`.
- Routing: `Navigator`, `RouteTemplate`, `RouteParams`.
- Optional store: `Store<T>`, selectors/subscriptions, hook integration utilities, and external service subscription adapters.
- Spring starter configuration: `JactProperties` (pages root package, initial route, window settings).

## Test Plan
1. Compiler and annotation processor tests
- Discovery and generated registry correctness.
- Route template generation with `$param` segments.
- Compile-time diagnostics for invalid signatures and duplicate routes.

2. Runtime tests
- Hook slot stability across re-renders.
- Effect scheduling and cleanup order.
- Targeted UI update behavior under repeated state transitions.

3. JavaFX integration tests
- Mount/unmount lifecycle correctness.
- Event -> state -> UI update cycle.
- Dynamic route substitution and parameter access.
- External service update -> subscribed component re-render cycle.

4. End-to-end sample tests
- Task Manager CRUD and navigation flows.
- Spring starter smoke tests for bootstrapping.

## Thesis Evaluation Plan
- Baseline for comparison: JavaFX MVC + FXML implementation of comparable Task Manager scope.
- Metrics:
  - implementation effort (time and code size),
  - UI update responsiveness,
  - architectural complexity and coupling,
  - developer ergonomics via structured rubric.
- Output artifacts:
  - experiment procedure,
  - result tables/figures,
  - interpretation and limitations.

## Assumptions and Defaults
- Public-facing project language remains professional and suitable for supervisor/reviewer audiences.
- v1 non-goals: hot reload, multi-process deployment mode, non-JavaFX renderer targets.
- Thesis LaTeX content is planned after MVP foundations are stable; this file remains the planning source of truth for implementation phases.
