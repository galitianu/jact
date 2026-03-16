package io.jact.sample;

import org.springframework.stereotype.Service;

@Service
public class AppLabelService {
    public String label() {
        return "Injected by Spring";
    }
}
