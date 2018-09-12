package com.github.adamldavis;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.adamldavis.DemoData.squares;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** Identical to ReactorDemoTest but using Java 10+ var. */
public class ReactorDemo10Test {


    ReactorDemo demo = new ReactorDemo();

    @Test
    public void testDoSquares() {
        assertArrayEquals(squares.toArray(), demo.doSquares().toArray());
    }

    @Test
    public void testDoParallelSquares() {
        var result = demo.doParallelSquares()
                .stream().sorted().collect(Collectors.toList());
                
        assertArrayEquals(squares.toArray(), result.toArray());
    }

    @Test
    public void testStepVerifier_Mono_error() {
        var monoError = Mono.error(new RuntimeException("error"));

        StepVerifier.create(monoError)
                .expectErrorMessage("error")
                .verify();
    }

    @Test
    public void testStepVerifier_Mono_foo() {
        var foo = Mono.just("foo");
        StepVerifier.create(foo)
                .expectNext("foo")
                .verifyComplete();
    }

    @Test
    public void testStepVerifier_Flux() {
        var flux = Flux.just(1, 4, 9);

        StepVerifier.create(flux)
                .expectNext(1)
                .expectNext(4)
                .expectNext(9)
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    public void testStepVerifier_Context_Wrong() {
        var flux = Flux.just(1).subscriberContext(Context.of("pid", 123));

        var stringFlux = flux.flatMap(i ->
                        Mono.subscriberContext().map(ctx -> i + " pid: " + ctx.getOrDefault("pid", 0)));

        StepVerifier.create(stringFlux)
                .expectNext("1 pid: 0")
                .verifyComplete();
    }

    @Test
    public void testStepVerifier_Context_Right() {
        var flux = Flux.just(1);

        var stringFlux = flux.flatMap(i ->
                Mono.subscriberContext().map(ctx -> i + " pid: " + ctx.getOrDefault("pid", 0)));

        StepVerifier.create(stringFlux.subscriberContext(Context.of("pid", 123)))
                .expectNext("1 pid: 123")
                .verifyComplete();
    }

    @Test
    public void test_TestPublisher() {
        var publisher = TestPublisher.create(); //1
        var stringFlux = publisher.flux(); //2
        var list = new ArrayList(); //3

        stringFlux.subscribe(next -> list.add(next), ex -> ex.printStackTrace()); //4
        publisher.emit("foo", "bar"); //5

        assertEquals(2, list.size()); //6
        assertEquals("foo", list.get(0));
        assertEquals("bar", list.get(1));
    }

}
