# Thesis Implementation Notes

## Scope

JACT is implemented as a reusable framework rather than as a single final application. The sample Task Manager is used to validate framework behavior and support thesis evaluation.

## Modules

- `jact-annotations`: public annotations and the `JNode` marker contract.
- `jact-compiler`: annotation processor that discovers pages/components and generates runtime registries with route/component metadata.
- `jact-core`: runtime, hooks, state/store abstractions, routing, declarative node types, lifecycle management, and component expansion.
- `jact-javafx`: JavaFX renderer bridge with direct node reconciliation.
- `jact-spring-boot-starter`: Spring Boot auto-configuration, generated registry loading, page/component invocation, and startup properties.
- `samples-task-manager`: JACT reference application.
- `samples-task-manager-javafx-baseline`: JavaFX MVC/FXML baseline application.

## Public API Surface

- Annotations: `@JactPage`, `@JactComponent`.
- UI nodes and factories: `JNode`, `Nodes`, `TextNode`, `ButtonNode`, `TextInputNode`, `CheckboxNode`, `SelectNode`, `ContainerNode`, `RowNode`, `ScrollAreaNode`, `SpacerNode`, `DividerNode`, `ComponentNode`, `KeyedNode`, `StyledNode`.
- Styling: `NodeStyle`.
- Runtime hooks: `Hooks.useState`, `Hooks.useEffect`, `Hooks.useMemo`, `Hooks.useStore`, `Hooks.useExternal`, `Hooks.navigator`, `Hooks.routeParams`.
- State contracts: `State`, `Store`, `SimpleStore`, `ObservableValue`, `Subscription`.
- Routing: `Navigator`, `RouteParams`, `RouteTemplate`.

## Implemented Thesis Claims

- Annotated pages and components are discovered at compile time.
- Components are invokable through the runtime and can receive explicit props plus Spring-managed dependencies.
- Hook state is isolated per page/component render identity.
- Dynamic routes and query params are available through `RouteParams`, including typed helpers.
- JavaFX rendering reconciles a retained node tree and supports keyed dynamic children.
- Spring Boot auto-configuration wires runtime, renderer, navigator, and generated metadata.
- The Task Manager sample demonstrates component composition, service state subscriptions, forms, filters, navigation, and detail views.
- The baseline module provides a comparable JavaFX MVC/FXML implementation for evaluation.

## Evaluation

Run:

```bash
evaluation/run-metrics.sh
```

The script generates `evaluation/results/jact-vs-javafx-baseline.md` with UI LOC, file counts, FXML LOC, and manual UI synchronization markers.

## Known Limitations

- JavaFX is the only renderer.
- The UI node set is intentionally small and thesis-focused.
- Styling support is lightweight compared with a mature design system.
- Route guards, hot reload, advanced devtools, and broad performance benchmarking are out of scope.
- The evaluation is a single case study and should be discussed as bounded evidence, not a universal benchmark.
