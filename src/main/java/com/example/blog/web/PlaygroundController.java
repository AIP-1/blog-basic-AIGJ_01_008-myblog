package com.example.blog.web;

import com.example.blog.service.PlaygroundService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PlaygroundController {

    private final PlaygroundService playgroundService;

    public PlaygroundController(PlaygroundService playgroundService) {
        this.playgroundService = playgroundService;
    }

    public record RunRequest(String code, String input) {
    }

    @GetMapping("/playground")
    public String page() {
        return "playground";
    }

    @PostMapping("/playground/run")
    @ResponseBody
    public PlaygroundService.Result run(@RequestBody RunRequest request) throws InterruptedException {
        return playgroundService.run(request.code(), request.input());
    }
}
