package com.recallx.recallx.api;

import com.recallx.recallx.service.AskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ask")
public class AskController {

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    /** Hindsight caps a query at 500 tokens; 500 characters keeps well under that. */
    public record AskRequest(@NotBlank @Size(max = 500) String question) { }

    @PostMapping
    public AskService.AskResponse ask(@Valid @RequestBody AskRequest request) {
        return askService.ask(request.question().trim());
    }
}
