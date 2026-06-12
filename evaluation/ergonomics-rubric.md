# Developer Ergonomics Rubric

Each dimension is scored from 1 to 5.

| Dimension | 1 | 3 | 5 |
| --- | --- | --- | --- |
| State-to-UI synchronization | Mostly manual control mutation | Mixed bindings and manual refresh | State changes declaratively drive UI |
| Separation of concerns | UI, state, and service logic are intertwined | Some controller/service separation | UI components consume services through explicit boundaries |
| Navigation clarity | Navigation is ad hoc | Navigation is centralized but manual | Routes and params are framework concepts |
| Component reuse | Reuse requires controller/FXML extraction | Reuse possible with ceremony | Reuse is ordinary method/component composition |
| Testability | UI behavior requires launching toolkit | Services testable, UI harder | Runtime and services can be tested independently |

Recommended thesis use: score both implementations, then discuss why the numbers are qualitative and bounded by a single case study.
