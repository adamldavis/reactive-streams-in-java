package com.github.adamldavis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Demonstrates Reactor in action.
 * 
 * @author Adam L. Davis
 */
public class ReactorDemo {

    public static List<Integer> doSquares() {
        List<Integer> squares = new ArrayList<>();
        Flux.range(1, 64) // 1
                .onBackpressureBuffer().map(v -> v * v) // 3
                .subscribeOn(Schedulers.immediate())
                .subscribe(squares::add); // 4

        return squares;
    }

    public static List<Integer> doParallelSquares() {
        List<Integer> squares = new ArrayList<>();
        Flux.range(1, 64).flatMap(v -> // 1
        Mono.just(v).subscribeOn(Schedulers.newSingle("comp")).map(w -> w * w))
                .doOnError(ex -> ex.printStackTrace()) // 2
                .doOnComplete(() -> System.out.println("Completed")) // 3
                .subscribeOn(Schedulers.immediate()).subscribe(squares::add);
        
        try {Thread.sleep(100); } catch (Exception e) {}
        return squares;
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
