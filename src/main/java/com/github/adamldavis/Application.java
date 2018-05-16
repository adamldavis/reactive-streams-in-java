package com.github.adamldavis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class Application {

    @Bean
    Flux<String> createMessageFlux(final Channel channel) {
        Flux<String> bridge = Flux.create(sink -> {
            sink.onRequest(n -> channel.poll(n).forEach(sink::next)) // 1
                    .onCancel(() -> channel.cancel()) // 2
                    .onDispose(() -> channel.close()); // 3
            
            channel.register(sink::next);
        });
        return bridge;
    }

    @Lazy(false)
    @Bean
    MessageConsumer createMessageConsumer(Flux<String> messageFlux) {
        MessageConsumer messageConsumer = new MessageConsumer();

        messageFlux.publishOn(Schedulers.newSingle("message-pub"))
                .subscribeOn(Schedulers.elastic())
                .onBackpressureBuffer()
                .subscribe(s -> messageConsumer.accept(s));

        return messageConsumer;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}