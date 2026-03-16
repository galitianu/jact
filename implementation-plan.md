# JACT v1 Implementation Plan (Spring + Reactive JavaFX)

## Summary
Build `jact` as a Java 21, Spring Boot 3.x framework for reactive desktop UI in a single JVM.  
v1 prioritizes core runtime, direct JavaFX bindings (no VDOM), file-based routing conventions, and a reference Task Manager CRUD app.  
Thesis work is included from the start through measurable comparison against JavaFX MVC + FXML.

## Implementation Changes
1. Repository + module structure (Gradle multi-module)
- Create modules: `jact-annotations`, `jact-core`, `jact-compiler` (annotation processor), `jact-javafx`, `jact-spring-boot-starter`, `samples-task-manager`.
- Publish coordinates under `io.jact:*` (for example `io.jact:jact-core`).

2. Component/page model (method-based, Spring-managed)
- Use `@JactComponent` and `@JactPage` annotations (avoid collision with Spring `@Component`).
- Components/pages are annotated methods inside Spring beans.
- Component methods return JACT UI nodes; runtime owns lifecycle + hook context.
- Page methods are discoverable by convention under configured pages root package.

3. Reactive runtime + rendering (direct bindings)
- Implement hook-based local state (`useState`, `useEffect`, `useMemo`) in `jact-core`.
- Provide optional global store module/API integrated with hook usage.
- Implement JavaFX renderer with retained node graph and direct property/event bindings.
- State updates trigger targeted JavaFX updates via binding graph; no virtual DOM diffing step.

4. Routing (file/package-based + params)
- Derive route templates from package/class naming under pages root.
- Dynamic segments use `$param` in template form (example `/users/$id`).
- Navigation resolves templates by substitution (example template `/users/$id` + `id=42` -> `/users/42`).
- Include navigation service with back/forward stack and parameter extraction.

5. Spring integration
- Auto-configuration via `jact-spring-boot-starter`.
- Startup lifecycle: Spring context boots, JACT runtime starts JavaFX app thread, root page mounts.
- Dependency injection supported in page/component bean methods.

6. Validation app + thesis evaluation assets
- Build `samples-task-manager` to exercise forms, lists, filter/search, CRUD, and routed details page.
- Define thesis evaluation protocol and scripts: compare JACT vs JavaFX MVC + FXML on:
  - implementation effort (LOC/time),
  - update responsiveness,
  - architectural complexity (component coupling),
  - developer ergonomics (qualitative + repeatable checklist).

## Public APIs / Interfaces
- Annotations: `@JactComponent`, `@JactPage`.
- Core types: `JNode` (or equivalent UI node contract), `HookContext`, `State<T>`.
- Routing: `Navigator`, `RouteTemplate`, `RouteParams`.
- Store (optional module): `Store<T>`, selectors/subscriptions, hook bridge.
- Spring starter config: `JactProperties` (pages root package, initial route, window settings).

## Test Plan
1. Compiler/annotation processor tests
- Discovery and generated registry correctness for pages/components.
- Route template generation including `$param` segments.
- Compile-time diagnostics for invalid signatures/duplicate routes.

2. Core runtime tests
- Hook slot stability across re-renders.
- Correct scheduling/order for effects and cleanup.
- Local state updates trigger only affected binding updates.

3. JavaFX integration tests
- Render/mount/unmount behavior.
- Event-to-state roundtrip (button/input -> state -> UI refresh).
- Navigation and param substitution (`/users/$id` -> `/users/42`).

4. End-to-end sample tests
- Task CRUD flows.
- Route changes preserve expected state boundaries.
- Smoke tests for starter auto-configuration in Spring Boot app.

## Assumptions and Defaults
- Language/runtime baseline: Java 21, Spring Boot 3.x.
- Deployment model: single JVM desktop app (Spring + JavaFX).
- Initial renderer target: JavaFX only (no Compose/SWT in v1).
- URL template syntax in JACT route definitions: `/.../$param`.
- Non-goals for v1: hot reload, multi-process deployment mode, web rendering.
- Plan artifact to maintain in repo: `implementation-plan.md` (iterative revisions), thesis LaTeX added later.
