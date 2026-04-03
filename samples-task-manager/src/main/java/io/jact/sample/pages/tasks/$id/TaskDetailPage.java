package io.jact.sample.pages.tasks.$id;

import io.jact.annotations.JNode;
import io.jact.annotations.JactPage;
import io.jact.core.api.Navigator;
import io.jact.core.node.Nodes;
import io.jact.core.routing.RouteParams;
import org.springframework.stereotype.Component;

@Component
public class TaskDetailPage {
    @JactPage
    public JNode taskDetail(RouteParams params, Navigator navigator) {
        return Nodes.column(
            Nodes.text("Task details"),
            Nodes.text("Task id: " + params.get("id")),
            Nodes.text("Current route: " + navigator.currentPath())
        );
    }
}
