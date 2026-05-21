# M3 Integration and Validation Plan

## Timeline
**April 27, 2026 to May 17, 2026**

## Summary
M3 finalizes Spring/runtime integration and validates JACT using a database-backed sample application.
The milestone introduces shared store primitives, an external observable bridge, and end-to-end CRUD/navigation flows backed by Spring Data JPA + H2.

## Deliverables
1. Global store and external observable APIs (`jact-core`)
- Add public contracts:
  - `Subscription`
  - `ObservableValue<T>`
  - `Store<T>` with `get`, `set`, `update`, and `subscribe`
- Add `SimpleStore<T>` as default store implementation.
- Extend `Hooks`:
  - `useStore(Store<T>)`
  - `useStore(Store<T>, Function<T, R>)`
  - `useExternal(ObservableValue<T>)`
- Guarantee selector-based rerender suppression when selected values remain equal.
- Guarantee subscription cleanup on source switch and unmount.

2. Hook runtime lifecycle and reactivity updates
- Track store/external hook slots in `HookRuntime`.
- Maintain subscription handles across rerenders.
- Unsubscribe deterministically on unmount and source replacement.
- Preserve existing `useState`/`useEffect`/`useMemo` semantics.

3. Spring starter integration finalization (`jact-spring-boot-starter`)
- Keep existing bootstrap path unchanged.
- Extend page-argument resolution to inject Spring beans by type for non-framework parameters.
- Keep explicit support for `RouteParams` and `Navigator` parameters.
- Add `ServiceStateBridge<T>` helper for Spring-managed services to expose `ObservableValue<T>`.

4. Validation app expansion (`samples-task-manager`)
- Move sample to Spring Data JPA + embedded H2.
- Add task entity/repository/service with CRUD and completion toggle.
- Expose service revision updates through `ObservableValue` to trigger automatic UI rerenders.
- Add shared UI state stores for search query, filter, and sort mode.
- Implement pages and navigation flows:
  - list view with search/filter/sort
  - create task
  - detail page route `/tasks/$id`
  - edit title
  - toggle complete/open
  - delete task

5. Documentation and milestone lock
- Align `implementation-plan.md` M3 section with concrete API and validation commitments.
- Document focus-stable rendering and key semantics for dynamic sibling collections.
- Freeze public M3 API at milestone end.

6. Focus-stable rendering and React-style keys (`jact-javafx` + `jact-core`)
- Replace full subtree replacement updates with retained-node reconciliation so controls are reused.
- Preserve active focus and text input caret/selection when element identity is preserved.
- Add `KeyedNode` / `Nodes.key(...)` for explicit identity in dynamic collections.
- Apply React-style key policy:
  - keys are required for dynamic sibling groups (insert/remove/reorder)
  - keys are optional for static/non-reordered sibling groups
  - sibling keys must be unique when provided
- Add diagnostics for ambiguous unkeyed dynamic sibling updates with guidance to use `Nodes.key(...)`.

## Week-by-Week Execution
1. Week 1 (April 27 to May 3)
- Implement store and observable public APIs.
- Add hook runtime support and cleanup semantics.
- Add targeted runtime unit tests for selector suppression and unmount cleanup.

Exit gate:
- `useStore` and `useExternal` work with deterministic rerender behavior.

2. Week 2 (May 4 to May 10)
- Finalize starter argument resolution with Spring bean injection.
- Add service bridge helper and integration tests.
- Add sample persistence model and service-level CRUD flows.

Exit gate:
- Starter can invoke page methods with injected Spring service arguments.
- Service bridge notifies subscribed UI paths.

3. Week 3 (May 11 to May 17)
- Complete sample UI flows and route detail page behavior.
- Implement and validate focus-stable renderer reconciliation.
- Add end-to-end validation tests for CRUD + search/filter + navigation.
- Finalize M3 documentation and acceptance checks.

Exit gate:
- M3 acceptance suite is green.
- Public API surface is frozen.

## Public APIs and Behavior Commitments (M3)
- New public APIs:
  - `Subscription`
  - `ObservableValue<T>`
  - `Store<T>`
  - `SimpleStore<T>`
  - `KeyedNode`
  - `Nodes.key(...)`
  - `Hooks.useStore(...)`
  - `Hooks.useExternal(...)`
- Behavior commitments:
  - Store selector subscriptions rerender only when selected values change.
  - External service updates trigger automatic rerender of subscribed components.
  - Subscription resources are cleaned up on unmount.
  - Focus and caret remain stable through rerenders when control identity is preserved.
  - Keyed matching is used for dynamic siblings, with positional fallback for static unkeyed siblings.

## Test Plan
1. Runtime and hook tests
- Selector-based rerender suppression for `useStore`.
- `useExternal` rerender behavior and unmount unsubscription.

2. Starter integration tests
- Auto-configuration beans are created as expected.
- Page argument resolution supports Spring-managed service parameters.

3. Validation app tests
- Service-level CRUD, search/filter behavior, and observable notifications.
- Spring boot context and persistence wiring smoke tests.
- Dynamic task list rows render with explicit keys while static controls remain unkeyed.

4. Renderer focus/reconciliation tests
- Typing in input fields remains uninterrupted across rerenders.
- Button focus is preserved for reused controls.
- Ambiguous unkeyed dynamic sibling updates fail with actionable diagnostics.

## Acceptance Criteria
- `Store` and `ObservableValue` APIs are implemented and covered by tests.
- `Hooks.useStore` and `Hooks.useExternal` are functional and lifecycle-safe.
- Starter resolves page method arguments from Spring context by type.
- Task Manager sample supports DB-backed create/edit/delete/search/filter/detail flows.
- Service-driven state changes rerender subscribed UI without manual refresh.
- Rerenders preserve focus/caret for reused controls.
- Dynamic sibling collections are keyed where required.
- M3 public API surface is frozen for M4 stabilization.

## Assumptions and Defaults
- Embedded H2 is sufficient for milestone validation scope.
- JavaFX end-to-end validation remains a required gate on supported runners.
- Internal runtime refactors are allowed in M4 without changing M3 public contracts.
- Typed route parameter conversion remains deferred beyond M3.
