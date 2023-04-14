package com.cristian.examples;

import io.vavr.concurrent.Future;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;


@SpringBootTest
class ExampleTests {

	@Test
	void collectionPipeline() {
		String stream = Stream
				.of(1, 2, 3, 4, 5)
				.filter(i -> i % 2 == 0)
				.map(i -> i * i)
				.reduce((a, b) -> a + b)
				.get()
				.toString();

		System.out.println(stream);
	}

	Integer blockingCall(int second) {
		Thread thread = Thread.currentThread();
		System.out.println("Start thread: " + thread.toString());
		Try.run(() -> Thread.sleep(second * 1000)).onFailure(ex -> System.out.println(ex));
		System.out.println("Finish thread: " + thread.toString());
		return second;
	}

	@Test
	void sync() {
		long start = System.nanoTime();
		blockingCall(5);
		blockingCall(5);
		blockingCall(5);
		long elapsed = System.nanoTime() - start;

		System.out.println(
				"Executing finished in %d ns"
						.formatted(elapsed)
		);
	}

	@Test
	void threads() throws InterruptedException {
		long start = System.nanoTime();
		Thread thread1 = new Thread(() -> blockingCall(5));
		Thread thread2 = new Thread(() -> blockingCall(5));
		Thread thread3 = new Thread(() -> blockingCall(5));

		thread1.start();
		thread2.start();
		thread3.start();
		thread1.join();
		thread2.join();
		thread3.join();
		long elapsed = System.nanoTime() - start;
		System.out.println(
				"Executing finished in %d ns"
						.formatted(elapsed)
		);
	}

	@Test
	void future() {
		long start = System.nanoTime();
		Future<Integer> future1 = Future.of(() -> blockingCall(5));
		Future<Integer> future2 = Future.of(() -> blockingCall(5));
		Future<Integer> future3 = Future.of(() -> blockingCall(5));

		future1.await();
		future2.await();
		future3.await();
		long elapsed = System.nanoTime() - start;
		System.out.println(
				"Executing finished in %d ns"
						.formatted(elapsed)
		);
	}

	@Test
	void backpressureExample() {
		Flux.range(1,5)
				.subscribe(new Subscriber<Integer>() {
					private Subscription s;
					int counter;

					@Override
					public void onSubscribe(Subscription s) {
						System.out.println("onSubscribe");
						this.s = s;
						System.out.println("Requesting 2 emissions");
						s.request(2);
					}

					@Override
					public void onNext(Integer i) {
						System.out.println("onNext " + i);
						counter++;
						if (counter % 2 == 0) {
							System.out.println("Requesting 2 emissions");
							s.request(2);
						}
					}

					@Override
					public void onError(Throwable t) {
						System.err.println("onError");
					}

					@Override
					public void onComplete() {
						System.out.println("onComplete");
					}
				});
	}



	@Test
	void boundedElastic() {
		long start = System.nanoTime();
		List l = Flux
				.just(5, 5, 5)
				.flatMap(i -> Mono.fromCallable(() -> blockingCall(i)).subscribeOn(Schedulers.boundedElastic()))
				.collectList()
				.block();
		long elapsed = System.nanoTime() - start;
		System.out.println(
				"Executing finished in %d ns"
						.formatted(elapsed)
		);
	}

	@Test
	public void onErrorExample() {
		Flux<String> fluxCalc = Flux.just(-1, 0, 1)
				.map(i -> "10 / " + i + " = " + (10 / i))
				.onErrorContinue((ex, i) -> ex.printStackTrace());

		fluxCalc
				.subscribe(
						value -> System.out.println("Next: " + value),
						error -> System.err.println("Error: " + error)
				);
	}

	@Test
	void retry() {
		Mono.just(1)
				.map(i -> {
					var r = new Random();
					if(r.nextBoolean())
						throw new RuntimeException("Can not process " + i);
					return i;
				})
				.doOnError((err) -> System.out.println("Error :: " + err))
				.retry(5)
				.subscribe(i -> System.out.println("Received :: " + i));
	}

	@Test
	public void coldPublisherExample() throws InterruptedException {
		Flux<Long> intervalFlux = Flux.interval(Duration.ofSeconds(1));
		Thread.sleep(2000);
		intervalFlux.subscribe(i -> System.out.println(String.format("Subscriber A, value: %d", i)));
		Thread.sleep(2000);
		intervalFlux.subscribe(i -> System.out.println(String.format("Subscriber B, value: %d", i)));
		Thread.sleep(3000);
	}

	@Test
	public void hotPublisherExample() throws InterruptedException {
		Flux<Long> intervalFlux = Flux.interval(Duration.ofSeconds(1));
		ConnectableFlux<Long> intervalCF = intervalFlux.publish();
		intervalCF.connect();
		Thread.sleep(2000);
		intervalCF.subscribe(i -> System.out.println(String.format("Subscriber A, value: %d", i)));
		Thread.sleep(2000);
		intervalCF.subscribe(i -> System.out.println(String.format("Subscriber B, value: %d", i)));
		Thread.sleep(3000);
	}



	@Test
	public void publishSubscribeExample() {
		Scheduler schedulerA = Schedulers.newParallel("Scheduler A");
		Scheduler schedulerB = Schedulers.newParallel("Scheduler B");
		Scheduler schedulerC = Schedulers.newParallel("Scheduler C");

		Flux.just(1)
				.map(i -> {
					System.out.println("First map: " + Thread.currentThread().getName());
					return i;
				})
				.subscribeOn(schedulerA)
				.map(i -> {
					System.out.println("Second map: " + Thread.currentThread().getName());
					return i;
				})
				.publishOn(schedulerB)
				.map(i -> {
					System.out.println("Third map: " + Thread.currentThread().getName());
					return i;
				})
				.subscribeOn(schedulerC)
				.map(i -> {
					System.out.println("Fourth map: " + Thread.currentThread().getName());
					return i;
				})
				.publishOn(schedulerA)
				.map(i -> {
					System.out.println("Fifth map: " + Thread.currentThread().getName());
					return i;
				})
				.blockLast();
	}

}
