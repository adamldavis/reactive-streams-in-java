package com.github.adamldavis;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.*;

import static java.util.Arrays.*;

/**
 * Demonstrates Akka Streams in action.
 * 
 * @author Adam L. Davis
 */
public class AkkaStreamsDemo {

    public static List<Integer> doSquares() {

        final ActorSystem system = ActorSystem.create("squares"); // 1
        final Materializer mat = ActorMaterializer.create(system); // 2

        List<Integer> squares = new ArrayList<>();
        Source.range(1, 64) // 1
                .map(v -> v * v) // 2
                .runForeach(squares::add, mat); // 3

        try {Thread.sleep(300); } catch (Exception e) {}
        
        return squares;
    }

    public static List<Integer> doParallelSquares() {

        final ActorSystem system = ActorSystem.create("parallel-squares"); // 1
        final Materializer mat = ActorMaterializer.create(system); // 2

        List<Integer> squares = new ArrayList<>();
        Source.range(1, 64) //2
                .mapConcat(v -> Source.from(asList(v)).map(w -> w * w) //2
                        .runFold(new ArrayList<Integer>(), (list, x) -> {
                            list.add(x);
                            return list;
                        }, mat).toCompletableFuture().get())
                .runForeach(squares::add, mat); //3

        try {Thread.sleep(300); } catch (Exception e) {}
        
        return squares;
    }

    /** Creating a materializer with more configuration. */
    public static Materializer createMaterializer() {
        final ActorSystem system = ActorSystem.create("reactive-messages"); // 1
        ActorMaterializerSettings settings = ActorMaterializerSettings.create(system) //2
                .withMaxFixedBufferSize(100) //3
                .withSyncProcessingLimit(2); //4

        return ActorMaterializer.create(settings, system); //5
    }

    public final List<String> messageList = new ArrayList<>();

    public void printErrors() {

        final ActorSystem system = ActorSystem.create("reactive-messages"); // 1
        final Materializer mat = ActorMaterializer.create(system); // 2

        Source<String, NotUsed> messages = getMessages();
        final Source<String, NotUsed> errors = messages
                .filter(m -> m.startsWith("Error")) // 3
                .alsoTo(Sink.foreach(messageList::add)).map(m -> m.toString()); // 4

        errors.runWith(Sink.foreach(System.out::println), mat); // 5
    }

    public Source<String, NotUsed> getMessages() {
        return Source.fromPublisher(publisher);
    }

    Publisher<String> publisher;

    public void setChannel(Channel channel) {
        publisher = Flux.create(sink -> {
            channel.register(sink::next);
            sink.onCancel(() -> channel.cancel())
                    .onDispose(() -> channel.close());
        });
    }

}
