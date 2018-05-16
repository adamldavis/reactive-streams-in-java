package com.github.adamldavis;

import org.springframework.stereotype.Service;

import io.reactivex.functions.Consumer;

/**
 * Implements reactive-streams Consumer interface which could be done as a Java
 * 8 lambda, but sometimes you might want to pull the code out into its own
 * class.
 */
@Service
public class MessageConsumer implements Consumer<String> {

    @Override
    public void accept(String message) {
        // do something with the Data
        if (message.startsWith("Error:"))
            System.err.println("message=" + message);
        else
            System.out.println("message=" + message);
    }

}
