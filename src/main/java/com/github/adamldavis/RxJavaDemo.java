package com.github.adamldavis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

/**
 * Demonstrates RxJava 2 in action.
 * 
 * @author Adam L. Davis
 */
public class RxJavaDemo {


    public static List<Integer> doSquares() {
        List<Integer> squares = new ArrayList<>();
        Flowable.range(1, 64) //1
            .observeOn(Schedulers.computation()) //2
            .map(v -> v * v) //3
            .blockingSubscribe(squares::add); //4
        
        return squares;
    }

    public static List<Integer> doParallelSquares() {
        List<Integer> squares = new ArrayList<>();
        Flowable.range(1, 64)
            .flatMap(v -> //1
              Flowable.just(v)
                .subscribeOn(Schedulers.computation())
                .map(w -> w * w)
            )
            .doOnError(ex -> ex.printStackTrace()) //2
            .doOnComplete(() -> System.out.println("Completed")) //3
            .blockingSubscribe(squares::add);
            
        return squares;
    }

    public static void runComputation() throws Exception {
        StringBuffer sb = new StringBuffer();
        Flowable<String> source = Flowable.fromCallable(() -> { //1
            Thread.sleep(1000); //  imitate expensive computation
            return "Done";
        });
        source.doOnComplete(() -> System.out.println("Completed runComputation"));

        Flowable<String> background = source.subscribeOn(Schedulers.io()); //2

        Flowable<String> foreground = background.observeOn(Schedulers.single());//3

        foreground.subscribe(System.out::println, Throwable::printStackTrace);//4
    }
    
    public static void writeFile(File file) {
        try (PrintWriter pw = new PrintWriter(file)) {
            Flowable.range(1, 100)
                .observeOn(Schedulers.newThread())
                .blockingSubscribe(pw::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void readFile(File file) {
        try (final BufferedReader br = new BufferedReader(new FileReader(file))) {

            Flowable<String> flow = Flowable.fromPublisher(new FilePublisher(br));

            flow.observeOn(Schedulers.io())
                    .blockingSubscribe(System.out::println);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static class FilePublisher implements Publisher<String> {
        BufferedReader reader;
        public FilePublisher(BufferedReader reader) { this.reader = reader; }
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
        public FilePublisherSubscription( FilePublisher publisher, 
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
                    if (subscriber != null) subscriber.onNext(line);
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
