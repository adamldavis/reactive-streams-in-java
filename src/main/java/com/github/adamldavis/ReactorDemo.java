package com.github.adamldavis;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * Demonstrates Reactor in action.
 * 
 * @author Adam L. Davis
 */
public class ReactorDemo implements ReactiveStreamsDemo {

    public static Flux<Long> exampleSquaresUsingGenerate() {
        Flux<Long> squares = Flux.generate(
                AtomicLong::new, //1
                (state, sink) -> {
                    long i = state.getAndIncrement();
                    sink.next(i * i); //2
                    if (i == 10) sink.complete(); //3
                    return state;
                });
        return squares;
    }

    public static List<Integer> doSquares() {
        List<Integer> squares = new ArrayList<>();
        Flux.range(1, 64) // 1
                .onBackpressureBuffer(256).map(v -> v * v) // 3
                .subscribeOn(Schedulers.immediate())
                .subscribe(squares::add); // 4

        return squares;
    }

    public static List<Integer> doParallelSquares() {
        return Flux.range(1, 64).flatMap(v -> // 1
            Mono.just(v).subscribeOn(Schedulers.parallel()).map(w -> w * w))
                .doOnError(ex -> ex.printStackTrace()) // 2
                .doOnComplete(() -> System.out.println("Completed")) // 3
                .subscribeOn(Schedulers.immediate()).collectList().block();
    }

    @Override
    public Future<List<Integer>> doSquaresAsync(int count) {
        return Flux.range(1, count).map(w -> w * w)
                .subscribeOn(Schedulers.immediate()).collectList().toFuture();
    }

    @Override
    public Future<String> doStringConcatAsync(int count) {
        return Flux.range(0, count)
                .map(i -> "i=" + i)
                .collect(() -> new StringBuilder(),
                    (stringBuilder, o) -> stringBuilder.append(o))
                .map(StringBuilder::toString)
                .toFuture();
    }

    @Override
    public Future<List<Integer>> doParallelSquaresAsync(int count) {
        return Flux.range(1, count).flatMap(v -> // 1
            Mono.just(v).subscribeOn(Schedulers.parallel()).map(w -> w * w))
                .subscribeOn(Schedulers.immediate()).collectList().toFuture();
    }

    @Override
    public Future<String> doParallelStringConcatAsync(int count) {
        BiConsumer<StringBuilder, Object> collector =
                (stringBuilder, o) -> stringBuilder.append(o);
        return Flux.range(0, count)
                .map(i -> "i=" + i)
                .window(10)
                .flatMap(flux -> flux.subscribeOn(Schedulers.parallel())
                        .collect(() -> new StringBuilder(), collector))
                .collect(() -> new StringBuilder(), collector)
                .map(StringBuilder::toString)
                .single().toFuture();
    }

    public static void runComputation() throws Exception {
        StringBuffer sb = new StringBuffer();
        Mono<String> source = Mono.fromCallable(() -> { // 1
            Thread.sleep(1000); // imitate expensive computation
            return "Done";
        });
        source.doOnNext((x) -> System.out.println("Completed runComputation"));

        Flux<String> background = source.subscribeOn(Schedulers.elastic())
                .flux(); // 2

        Flux<String> foreground = background.publishOn(Schedulers.immediate());

        foreground.subscribe(System.out::println, Throwable::printStackTrace);// 4
    }

    public static void writeFile(File file) {
        try (PrintWriter pw = new PrintWriter(file)) {
            Flux.range(1, 100)
                    .publishOn(Schedulers
                            .fromExecutor(Executors.newFixedThreadPool(1)))
                    .subscribeOn(Schedulers.immediate()).subscribe(pw::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void readFile(File file) {
        try (final BufferedReader br = new BufferedReader(
                new FileReader(file))) {

            Flux<String> flow = Flux.from(new FilePublisher(br));

            flow.publishOn(Schedulers.elastic())
                    .subscribeOn(Schedulers.immediate())
                    .subscribe(System.out::println);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void useProcessor() {
        // Processor implements both Publisher and Subscriber
        // DirectProcessor is the simplest Processor from Reactor
        final FluxProcessor<String, String> processor = DirectProcessor.create();
        //TODO
    }

    static class FilePublisher implements Publisher<String> {
        BufferedReader reader;

        public FilePublisher(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void subscribe(Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(
                    new FilePublisherSubscription(this, subscriber));
        }

        public String readLine() throws IOException {
            return reader.readLine();
        }
    }

    static class FilePublisherSubscription implements Subscription {
        FilePublisher publisher;
        Subscriber<? super String> subscriber;

        public FilePublisherSubscription(FilePublisher publisher,
                Subscriber<? super String> subscriber) {
            this.publisher = publisher;
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            try {
                String line;
                for (int i = 0; i < n && publisher != null
                        && (line = publisher.readLine()) != null; i++) {
                    if (subscriber != null)
                        subscriber.onNext(line);
                }
            } catch (IOException ex) {
                subscriber.onError(ex);
            }
            subscriber.onComplete();
        }

        @Override
        public void cancel() {
            publisher = null;
        }
    }

}
