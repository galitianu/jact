package io.jact.sample;

import io.jact.annotations.JNode;
import io.jact.annotations.JactPage;
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
        return Nodes.column(
            Nodes.text("JACT M1"),
            Nodes.text(appLabelService.label())
        );
    }
}
