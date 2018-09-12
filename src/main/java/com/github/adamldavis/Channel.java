package com.github.adamldavis;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import io.reactivex.functions.Consumer;

/**
 * Channel uses a Deque to act as a buffer of messages.
 * Merely here as a demo of integrating an outside system with Reactive Streams.
 * In practice this could probably be better replaced by PublishProcessor in RxJava or a Broadcast in Akka Streams
 * or UnicastProcessor in Reactor.
 *
 * This is definitely not the best implementation available for this concept.
 * For example, this Channel does not make any guarantee about ordering. Again, this is just for show.
 */
@Service
public class Channel extends Thread {

    final Deque<String> deque = new ConcurrentLinkedDeque<>();
    
    final List<Consumer<String>> listeners = new ArrayList<>();

    AtomicLong pollCount = new AtomicLong(0);

    /** Non-Blocking and increments the pollCount by n. */
    public void poll(long n) {
        synchronized (deque) {
            pollCount.getAndAdd(n);
            System.out.println("--> poll (" + n + ") called");
            if (!isAlive()) start();
        }
    }

    /** Loops forever, gets up to pollCount messages and sends them to the Listeners. */
    @Override
    public void run() {
        while (true) {
            loop();
        }
    }

    private void loop() {
        int count = 0;

        for (String msg; count < pollCount.get() && !deque.isEmpty(); count++) {
            msg = deque.pop();
            final String message = msg;
            listeners.forEach(listener -> {
                try {
                    listener.accept(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        pollCount.getAndAdd(0 - count); // decrement by number of messages accepted.
        try {
            Thread.sleep(25);
        } // sleep to allow other threads to go
        catch (Exception ignore) {}
    }

    public void cancel() {
        deque.clear();
        super.interrupt();
    }

    public void close() {
        deque.clear();
        super.interrupt();
    }

    public void publish(String message) {
        deque.add(message);
    }
    
    public void register(Consumer<String> listener) {
        listeners.add(listener);
    }
}