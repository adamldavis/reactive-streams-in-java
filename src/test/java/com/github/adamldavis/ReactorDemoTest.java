package com.github.adamldavis;

import static com.github.adamldavis.DemoData.*;
import static org.junit.Assert.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.util.context.Context;

public class ReactorDemoTest {


    ReactorDemo demo = new ReactorDemo();

    @Test
    public void testDoSquares() {
        assertArrayEquals(squares.toArray(), demo.doSquares().toArray());
    }

    @Test
    public void testDoParallelSquares() {
        List result = demo.doParallelSquares()
                .stream().sorted().collect(Collectors.toList());
                
        assertArrayEquals(squares.toArray(), result.toArray());
    }

    @Test
    public void testStepVerifier_Mono_error() {
        Mono<String> monoError = Mono.error(new RuntimeException("error"));

        StepVerifier.create(monoError)
                .expectErrorMessage("error")
                .verify();
    }

    @Test
    public void testStepVerifier_Mono_foo() {
        Mono<String> foo = Mono.just("foo");
        StepVerifier.create(foo)
                .expectNext("foo")
                .verifyComplete();
    }

    @Test
    public void testStepVerifier_Flux() {
        Flux<Integer> flux = Flux.just(1, 4, 9);

        StepVerifier.create(flux)
                .expectNext(1)
                .expectNext(4)
                .expectNext(9)
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    public void testStepVerifier_Context_Wrong() {
        Flux<Integer> flux = Flux.just(1).subscriberContext(Context.of("pid", 123));

        Flux<String> stringFlux = flux.flatMap(i ->
                        Mono.subscriberContext().map(ctx -> i + " pid: " + ctx.getOrDefault("pid", 0)));

        StepVerifier.create(stringFlux)
                .expectNext("1 pid: 0")
                .verifyComplete();
    }

    @Test
    public void testStepVerifier_Context_Right() {
        Flux<Integer> flux = Flux.just(1);

        Flux<String> stringFlux = flux.flatMap(i ->
                Mono.subscriberContext().map(ctx -> i + " pid: " + ctx.getOrDefault("pid", 0)));

        StepVerifier.create(stringFlux.subscriberContext(Context.of("pid", 123)))
                .expectNext("1 pid: 123")
                .verifyComplete();
    }

    @Test
    public void test_TestPublisher() {
        TestPublisher<Object> publisher = TestPublisher.create(); //1
        Flux<Object> stringFlux = publisher.flux(); //2
        List list = new ArrayList(); //3

        stringFlux.subscribe(next -> list.add(next), ex -> ex.printStackTrace()); //4
        publisher.emit("foo", "bar"); //5

        assertEquals(2, list.size()); //6
        assertEquals("foo", list.get(0));
        assertEquals("bar", list.get(1));
    }

}
