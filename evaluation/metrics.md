# Metrics Definition

## UI Java LOC

Non-empty Java lines in files that directly define UI pages, components, controllers, or application UI entry points.

For JACT this includes:

- `HomePage.java`
- `TaskDetailPage.java`
- `TaskComponents.java`

For the JavaFX baseline this includes:

- `TaskManagerBaselineApplication.java`
- `TaskManagerController.java`

## FXML LOC

Non-empty lines in FXML layout files. JACT has no FXML by design.

## UI Files

Number of Java or FXML files directly involved in UI construction.

## Manual UI Synchronization Points

Occurrences of source markers that indicate explicit UI synchronization work:

- JavaFX listeners;
- direct calls to `refreshList`;
- direct `setText`, `setSelected`, `setDisable`, `setItems`, `setValue`, or `getItems().setAll` calls.

For JACT, equivalent updates are expressed as state/store changes and framework-driven re-rendering, so the expected count should be lower.
