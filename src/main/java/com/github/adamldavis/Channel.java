package com.github.adamldavis;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Service;

import io.reactivex.functions.Consumer;

/** Channel uses a Deque to act as a buffer of messages. */
@Service
public class Channel {

    final Deque<String> deque = new ConcurrentLinkedDeque<>();
    
    final List<Consumer<String>> listeners = new ArrayList<>();

    /** Blocking and gets up to n. */
    // Just for demo. probably not the best way to to this
    public List<String> poll(long n) {
        int i = 0;
        List<String> list = new ArrayList<>();
        String poll = null;

        // wait until at least one element:
        while ((poll = deque.poll()) == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
        }
        for (; poll != null && i < n; i++, poll = deque.poll()) {
            list.add(poll);
        }
        return list;
    }

    public void cancel() {
        deque.clear();
    }

    public void close() {
        deque.clear();
    }

    public void publish(String message) {
        deque.add(message);
        listeners.forEach(listener -> {
            try {
                listener.accept(message);
            } catch (Exception e) {
                e.printStackTrace();
            }   
        });
    }
    
    public void register(Consumer<String> listener) {
        listeners.add(listener);
    }
}