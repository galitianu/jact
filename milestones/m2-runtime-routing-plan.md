# M2 Runtime and Routing Core Plan

## Timeline
**April 6, 2026 to April 26, 2026**

## Summary
M2 introduces JACT's first real reactive runtime:
- local hook state via a static `Hooks` API
- dependency-based effects and memoization
- subtree re-rendering and targeted JavaFX updates
- convention-based routing with `$param` dynamic segments

M2 scope is intentionally limited to local component/page reactivity and navigation.
Independently-changing Spring service state subscriptions are explicitly deferred to M3.

## Deliverables
1. Hook runtime core (`jact-core`)
- Add static hook API entrypoints:
  - `Hooks.useState(...)`
  - `Hooks.useEffect(...)`
  - `Hooks.useMemo(...)`
- Define `State<T>` as first-class local state handle (`get()`, `set(T)`).
- Implement stable hook slot indexing per component instance identity.
- Implement scheduler that batches updates and re-renders only the affected component subtree.
- Implement `useEffect` dependency semantics:
  - effect runs after commit
  - cleanup runs before dependency-change re-run and unmount
- Implement `useMemo` dependency tracking and value caching.

2. Runtime invocation and lifecycle integration
- Wrap page/component method execution in a hook-aware runtime context.
- Keep method signatures unchanged (no required hook context parameter).
- Track mount/update/unmount lifecycle so hook state/effects are bound to instance identity.

3. JavaFX update-capable renderer (`jact-javafx`)
- Extend renderer from mount-only behavior to update-capable subtree patch behavior.
- Preserve retained node mapping and apply minimal node/property updates in affected scope.
- Keep JavaFX thread safety guarantees for all commit/update/unmount operations.

4. Routing and navigation core
- Keep compile-time generated route registry as source of truth.
- Support convention-derived dynamic segments:
  - Java package/class segments starting with `$` map to route params.
- Keep `@JactPage(path=...)` explicit override behavior.
- Expose M2 route params as strings.
- Add `Navigator` API with:
  - `push(...)`
  - `replace(...)`
  - `back()`
  - current route/params access

5. Compile-time generation updates (`jact-compiler`)
- Extend route generation rules for `$param` dynamic segment handling.
- Validate route collisions including dynamic templates.
- Keep existing method-signature diagnostics and add diagnostics for invalid dynamic segment usage.

6. Sample app M2 runtime validation
- Add M2 sample flows proving:
  - local state update causes automatic UI refresh
  - route transitions and param extraction
  - `push/replace/back` behavior
- Keep service-driven independent state updates out of M2 sample scope.

## Week-by-Week Execution
1. Week 1 (April 6 to April 12)
- Implement `Hooks` static API contracts and `State<T>` semantics.
- Add hook slot indexing and render-context scaffolding.
- Add core scheduler and update queue primitives.

Exit gate:
- Local state change can schedule re-render for a single component instance.

2. Week 2 (April 13 to April 19)
- Implement `useEffect` and `useMemo` dependency semantics.
- Add lifecycle cleanup behavior on dependency changes and unmount.
- Extend JavaFX renderer for update-capable subtree patching.

Exit gate:
- Effect/memo behavior matches defined dependency semantics.
- Subtree updates apply without full-page rebuild.

3. Week 3 (April 20 to April 26)
- Implement `$param` route generation/validation and navigation API.
- Wire route params and navigation state into runtime flow.
- Add sample flows and finalize M2 docs/tests.

Exit gate:
- `push/replace/back` works for static and dynamic routes.
- Route param extraction and substitution are correct for M2 string params.

## Public APIs and Behavior Commitments (M2)
- Hook API style:
  - static `Hooks.*` calls
  - no required hook context method parameter
- Local state API:
  - `State<T>` handle with read/write operations
- Navigation API:
  - imperative `push/replace/back`
  - current route and params access
- Route param type in M2:
  - string-only (typed converters deferred)

## Test Plan
1. Hook runtime tests
- Hook slot ordering stability across repeated renders.
- State batching and single-scope re-render behavior.
- `useEffect` run/cleanup order on dependency change and unmount.
- `useMemo` recomputation only when dependencies change.

2. Renderer integration tests
- Subtree patch updates only affect targeted scope.
- Event -> state update -> scheduled re-render -> updated UI cycle.

3. Routing/navigation tests
- Convention-generated `$param` routes are correct.
- String param extraction is correct for push/replace flows.
- Back navigation restores expected route state.
- Dynamic route collisions/invalid patterns fail at compile time with clear diagnostics.

4. End-to-end sample tests
- Local counter-style state updates re-render UI without manual direct node mutation.
- Two-route navigation flow validates params and back behavior.

## Acceptance Criteria
- M2 build and tests pass in CI on Java 21.
- Hook API (`useState`, `useEffect`, `useMemo`) behaves per M2 semantics.
- State changes trigger subtree-only re-render updates.
- Dynamic `$param` routing and `Navigator.push/replace/back` are functional and tested.
- Service-driven independent state subscription remains intentionally out of M2 scope and is tracked for M3.

## Assumptions and Defaults
- Static hooks API is preferred over method-injected hook context for M2.
- Route templates come from generated registry + conventions, not runtime reflection-only routing.
- M2 prioritizes deterministic runtime behavior and clear semantics over broad feature surface.
