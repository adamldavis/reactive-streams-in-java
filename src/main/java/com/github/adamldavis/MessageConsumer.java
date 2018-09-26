package com.github.adamldavis;

import io.reactivex.functions.Consumer;

/**
 * Implements reactive-streams Consumer interface which could be done as a Java
 * 8 lambda, but sometimes you might want to pull the code out into its own
 * class.
 */
public class MessageConsumer implements Consumer<String> {

    final String type;

    public MessageConsumer(String type) {
        this.type = type;
    }

    @Override
    public void accept(String message) {
        // do something with the Data
        if (message.startsWith("Error:"))
            System.err.println("type=" + type + " message=" + message);
        else
            System.out.println("type=" + type + " message=" + message);
    }

}
