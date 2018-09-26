package com.github.adamldavis;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Performance test to compare all three implementations: Reactor, RxJava, and Akka Streams.
 * Runs doParallelSquares many times in different ordering and gathers resulting timing averages.
 */
public class PerformanceTest {

    @FunctionalInterface
    interface AsyncRunnable {
        void run() throws ExecutionException, InterruptedException;
    }

    final Map<String, Long> times = new HashMap<>();
    final Map<String, ReactiveStreamsDemo> demos = new HashMap<>();

    @Before
    public void setup() {
        times.clear();
        demos.clear();
        demos.put("akka-st", new AkkaStreamsDemo());
        demos.put("reactor", new ReactorDemo());
        demos.put("rxjava", new RxJavaDemo());
    }

    public static Long time(AsyncRunnable runnable) {
        long start = System.nanoTime();
        try {
            runnable.run();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return System.nanoTime() - start;
    }

    public static Long multiTime(AsyncRunnable runnable) {
        long time = 0;
        long count = 100;

        for (int i = 0; i < count; i++) time += time(runnable);

        return time / count;
    }

    public Long timeSquares(ReactiveStreamsDemo demo) {
        return multiTime(() -> demo.doSquaresAsync(64).get());
    }
    public Long timeParallelSquares(ReactiveStreamsDemo demo) {
        return multiTime(() -> demo.doParallelSquaresAsync(64).get());
    }
    public Long timeStringConcat(ReactiveStreamsDemo demo) {
        return multiTime(() -> demo.doStringConcatAsync(64).get());
    }
    public Long timeParallelStringConcat(ReactiveStreamsDemo demo) {
        return multiTime(() -> demo.doParallelStringConcatAsync(64).get());
    }

    @Test
    public void timeTest1() {
        doTest();
    }

    @Test
    public void timeTest2() {
        doTest();
    }

    @Test
    public void timeTest3() {
        doTest();
    }

    public void doTest() {
        var keys = demos.keySet().toArray(new String[3]);
        var indexList = new ArrayList<>(asList(0,1,2));
        var rnd = new Random();

        while (!indexList.isEmpty()) {
            int i = indexList.size() == 1 ? 0 : rnd.nextInt(indexList.size());
            int index = indexList.remove(i);
            var key = keys[index];
            var demo = demos.get(key);
            assertNotNull(demo);

            times.put(" Squares  " + key, timeSquares(demo));
            times.put(" PSquares " + key, timeParallelSquares(demo));
            times.put(" StrCon   " + key, timeStringConcat(demo));
            times.put(" PStrCon  " + key, timeParallelStringConcat(demo));
        }
        printTimes();
    }

    private void printTimes() {
        Flux.fromIterable(times.keySet()).sort()
            .map(key -> key + ",\t" + times.get(key))
            .subscribe(System.out::println);
    }
    
    @Test
    public void testSquaresResults() throws Exception {
        int count = 15;
        var keys = new ArrayList<>(demos.keySet());
        var demo1 = demos.get(keys.get(0));
        var demo2 = demos.get(keys.get(1));
        var demo3 = demos.get(keys.get(2));
        var r1 = demo1.doSquaresAsync(count).get();
        var r2 = demo2.doSquaresAsync(count).get();
        var r3 = demo3.doSquaresAsync(count).get();
        assertEquals(demo1.getClass() + " != " + demo2.getClass(), r1, r2);
        assertEquals(demo3.getClass() + " != " + demo2.getClass(), r3, r2);
    }

    @Test
    public void testParSquaresResults() throws Exception {
        int count = 15;
        var keys = new ArrayList<>(demos.keySet());
        var demo1 = demos.get(keys.get(0));
        var demo2 = demos.get(keys.get(1));
        var demo3 = demos.get(keys.get(2));
        var join = Collectors.toList();
        var r1 = demo1.doParallelSquaresAsync(count).get().stream().sorted().collect(join);
        var r2 = demo2.doParallelSquaresAsync(count).get().stream().sorted().collect(join);
        var r3 = demo3.doParallelSquaresAsync(count).get().stream().sorted().collect(join);
        assertEquals(demo1.getClass() + " != " + demo2.getClass(), r1, r2);
        assertEquals(demo3.getClass() + " != " + demo2.getClass(), r3, r2);
    }

    @Test
    public void testStringConcatResults() throws Exception {
        int count = 11;
        var keys = new ArrayList<>(demos.keySet());
        var demo1 = demos.get(keys.get(0));
        var demo2 = demos.get(keys.get(1));
        var demo3 = demos.get(keys.get(2));
        var r1 = demo1.doStringConcatAsync(count).get();
        var r2 = demo2.doStringConcatAsync(count).get();
        var r3 = demo3.doStringConcatAsync(count).get();
        assertEquals(demo1.getClass() + " != " + demo2.getClass(), r1, r2);
        assertEquals(demo3.getClass() + " != " + demo2.getClass(), r3, r2);
    }
    
    @Test
    public void testParallelStringConcatResults() throws Exception {
        int count = 11;
        var keys = new ArrayList<>(demos.keySet());
        var demo1 = demos.get(keys.get(0));
        var demo2 = demos.get(keys.get(1));
        var demo3 = demos.get(keys.get(2));
        var r1 = demo1.doParallelStringConcatAsync(count).get();
        var r2 = demo2.doParallelStringConcatAsync(count).get();
        var r3 = demo3.doParallelStringConcatAsync(count).get();
        assertEquals(demo1.getClass() + " != " + demo2.getClass(), r1.length(), r2.length());
        assertEquals(demo3.getClass() + " != " + demo2.getClass(), r3.length(), r2.length());
    }

    
}
