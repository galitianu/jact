# M1 Foundations Plan

## Timeline
**March 16, 2026 to April 5, 2026**

## Summary
M1 establishes the minimum viable technical foundation for JACT:
- Gradle multi-module project structure
- Compile-time annotation processing with generated registries
- Minimal Spring-to-JavaFX bootstrap path mounting one page
- CI baseline and initial testing gates

At M1 completion, the repository must build in CI, generate registry classes from annotations, and launch a desktop window through Spring startup.

## Deliverables
1. Repository bootstrap and build conventions
- Gradle modules:
  - `jact-annotations`
  - `jact-core`
  - `jact-compiler`
  - `jact-javafx`
  - `jact-spring-boot-starter`
  - `samples-task-manager`
- Common coordinates: `io.jact:*`
- Java toolchain baseline: Java 21
- Module boundaries:
  - No JavaFX dependency in `jact-core`
  - No Spring dependency in `jact-core` or `jact-javafx`

2. `jact-annotations` initial API
- `@JactComponent` (method-level)
- `@JactPage` (method-level, optional `path` override)
- Minimal contracts required by M1 compile path:
  - `JNode`
  - `RenderContext` placeholder type

3. `jact-compiler` annotation processor
- Discover methods annotated with `@JactComponent` and `@JactPage`
- Generate deterministic registries:
  - `GeneratedComponentRegistry`
  - `GeneratedPageRegistry`
- Include M1 compile diagnostics:
  - duplicate route collision
  - invalid annotation target
  - non-public annotated method
  - invalid return type for annotated methods

4. `jact-core` runtime skeleton
- Runtime contracts:
  - `ComponentDescriptor`
  - `PageDescriptor`
  - `RuntimeRegistry`
  - `RendererBridge`
- Startup service:
  - `JactRuntime.start()`
  - `JactRuntime.mountInitialPage()`
- Basic startup error model for registry/bootstrap failures

5. `jact-javafx` minimal renderer bridge
- `JavaFxRendererBridge` mounts a single `JNode` tree into a JavaFX `Scene`
- Minimal node adapters for M1 demo (container + text)
- JavaFX thread safety for initial mount path

6. `jact-spring-boot-starter` M1 integration
- Auto-configure runtime, registry wiring, and renderer bridge
- `JactProperties` with:
  - `enabled`
  - `initialPage`
  - `windowTitle`
  - `windowWidth`
  - `windowHeight`
- Startup path:
  - Spring context initializes
  - JACT runtime initializes
  - Initial page mounts on JavaFX stage

7. `samples-task-manager` M1 demo
- One minimal `@JactPage` page
- Static layout with one DI-provided label to prove Spring injection path
- Startup profile/config for local smoke launch

8. CI baseline
- GitHub Actions workflow on Java 21
- Build/test gate:
  - `./gradlew clean build`
- Test stack baseline:
  - JUnit 5
  - AssertJ

## Week-by-Week Execution
1. Week 1 (March 16 to March 22)
- Create module structure and Gradle conventions
- Add annotations and minimal core contracts
- Add initial CI workflow

Exit gate:
- Multi-module build works locally and in CI

2. Week 2 (March 23 to March 29)
- Implement annotation processor discovery and registry generation
- Add compile-time diagnostics and processor tests

Exit gate:
- Registries are generated from annotated methods
- Negative processor tests fail with clear diagnostics

3. Week 3 (March 30 to April 5)
- Implement Spring starter bootstrap + JavaFX mount path
- Implement sample page with DI label
- Add integration smoke tests

Exit gate:
- Spring startup mounts initial JACT page in JavaFX window
- CI remains green with all M1 tests

## Public APIs and Generated Artifacts (M1)
- Public annotations:
  - `@JactComponent`
  - `@JactPage`
- Public runtime/config contracts:
  - `JNode`
  - `RenderContext`
  - `JactRuntime`
  - `JactProperties`
- Generated artifacts:
  - `GeneratedComponentRegistry`
  - `GeneratedPageRegistry`
- Configuration keys:
  - `jact.enabled`
  - `jact.initial-page`
  - `jact.window-title`
  - `jact.window-width`
  - `jact.window-height`

## Test Plan
1. Unit tests
- Annotation and descriptor mapping behavior in compiler internals
- Runtime registry loading and startup error handling

2. Compile-time processor tests
- Positive generation tests for valid pages/components
- Negative tests for invalid signatures, duplicate routes, and invalid targets

3. Spring/JavaFX integration tests
- Starter loads with `jact.enabled=false`
- Required runtime beans are created when enabled
- Manual/headed smoke launch validates first-page mount path

## Acceptance Criteria
- `clean build` passes in GitHub Actions (Java 21)
- Annotation processor generates page/component registries
- Sample app mounts a minimal page through Spring -> JACT -> JavaFX startup path

## Assumptions and Defaults
- M1 intentionally excludes hooks, mutable state updates, and route transition logic (planned for M2)
- Dynamic `$param` route behavior is documented in M1 but fully implemented and validated in M2
- M1 scope prioritizes foundation quality and deterministic startup over feature breadth
