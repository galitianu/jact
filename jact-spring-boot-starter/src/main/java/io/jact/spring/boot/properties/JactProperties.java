package io.jact.spring.boot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jact")
public class JactProperties {
    private boolean enabled = false;
    private String initialPage = "/";
    private String windowTitle = "JACT";
    private int windowWidth = 960;
    private int windowHeight = 640;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInitialPage() {
        return initialPage;
    }

    public void setInitialPage(String initialPage) {
        if (initialPage == null || initialPage.isBlank()) {
            throw new IllegalArgumentException("jact.initial-page must not be blank");
        }
        this.initialPage = initialPage;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public void setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        if (windowWidth <= 0) {
            throw new IllegalArgumentException("jact.window-width must be greater than zero");
        }
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        if (windowHeight <= 0) {
            throw new IllegalArgumentException("jact.window-height must be greater than zero");
        }
        this.windowHeight = windowHeight;
    }
}
