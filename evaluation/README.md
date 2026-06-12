# JACT Thesis Evaluation

This directory contains the reproducible comparison package for the thesis evaluation.

The comparison target is:

- `samples-task-manager`: Task Manager implemented with JACT components and Spring Boot services.
- `samples-task-manager-javafx-baseline`: comparable Task Manager implemented with JavaFX MVC/FXML.

The evaluation focuses on implementation effort, architectural structure, and reactive update ergonomics. It does not claim a complete performance benchmark of all Java desktop UI approaches.

## Run

```bash
evaluation/run-metrics.sh
```

The script writes `evaluation/results/jact-vs-javafx-baseline.md`.

## Metrics

The generated table includes:

- UI-related Java LOC;
- FXML LOC where applicable;
- number of UI Java files;
- number of FXML files;
- number of manually identifiable UI synchronization points.

Manual synchronization points are counted by simple source markers such as JavaFX listeners, explicit refresh methods, and direct control updates. This is intentionally transparent and conservative rather than a hidden benchmark.
