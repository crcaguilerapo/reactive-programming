package com.cristian.examples.presentation;

import com.cristian.examples.businessLogic.TestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("async")
public class Controller {

    private final TestService testService;

    public Controller(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/ABC")
    public Mono<Integer> ABC() {
        System.out.println("Processing request in thread: " + Thread.currentThread().getName());
        return testService.ABC();
    }
}
