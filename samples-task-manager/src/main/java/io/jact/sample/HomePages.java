package io.jact.sample;

import io.jact.annotations.JNode;
import io.jact.annotations.JactPage;
import io.jact.core.api.Hooks;
import io.jact.core.api.State;
import io.jact.core.node.Nodes;
import org.springframework.stereotype.Component;

@Component
public class HomePages {
    private final AppLabelService appLabelService;

    public HomePages(AppLabelService appLabelService) {
        this.appLabelService = appLabelService;
    }

    @JactPage(path = "/")
    public JNode home() {
        State<Integer> count = Hooks.useState(0);

        return Nodes.column(
            Nodes.text("JACT M2 Counter"),
            Nodes.text(appLabelService.label()),
            Nodes.text("Count: " + count.get()),
            Nodes.button("Increment", () -> count.set(count.get() + 1))
        );
    }
}
