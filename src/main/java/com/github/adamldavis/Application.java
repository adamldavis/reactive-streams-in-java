package com.github.adamldavis;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
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
        // using Reactor to create a Flux linked to the Channel:
        Flux<String> bridge = Flux.create(sink -> {
                sink.onRequest(n -> channel.poll(n)) // 1
                    .onCancel(channel::cancel) // 2
                    .onDispose(channel::close); // 3
            
            channel.register(sink::next); //4
        });
        return bridge;
    }

    @Lazy(false)
    @Bean
    MessageConsumer createMessageConsumer(Flux<String> messageFlux) {
        MessageConsumer messageConsumer = new MessageConsumer("Reactor");
        // using Reactor to consume messages:
        messageFlux.publishOn(Schedulers.newSingle("message-pub"))
                .subscribeOn(Schedulers.elastic())
                .onBackpressureBuffer(100) // 100 max buffer
                .subscribe(s -> messageConsumer.accept(s));

        return messageConsumer;
    }

    @Lazy(false)
    @Bean
    MessageConsumer createAkkaMessageConsumer(Flux<String> messageFlux, ActorSystem actorSystem) {
        MessageConsumer messageConsumer = new MessageConsumer("Akka");
        // using Akka Streams to consume messages:
        ActorMaterializer mat = ActorMaterializer.create(actorSystem);

        Source.fromPublisher(messageFlux)
                .buffer(100, OverflowStrategy.backpressure()) // 100 max buffer
                .to(Sink.foreach(msg -> messageConsumer.accept(msg)))
                .run(mat);

        return messageConsumer;
    }

    @Bean
    ActorSystem createActorSystem() {
        return ActorSystem.create();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}