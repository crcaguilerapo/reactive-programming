package com.cristian.examples.businessLogic;

import io.vavr.control.Try;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class TestService {
    private Mono<Integer> blockingCall(int second) {
        String nameThread = Thread.currentThread().getName();
        System.out.println("Start thread: " + nameThread);
        Try.run(() -> Thread.sleep(second * 1000)).onFailure(ex -> System.out.println(ex));
        System.out.println("Finish thread: " + nameThread);
        return Mono.just(second);
    }

    private Mono<Integer> A() {
        return blockingCall(5);
    }

    public Mono<Integer> ABC() {
        return Flux
                .just(A(), A(), A(), A(), A(), A(), A(), A())
                .publishOn(Schedulers.newBoundedElastic(1, 10, "test"))
                .flatMap(v -> v)
                .reduce((a, b) -> a + b);
    }
}
