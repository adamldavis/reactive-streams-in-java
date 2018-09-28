package com.github.adamldavis;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.*;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;

import static java.util.Arrays.*;

/**
 * Demonstrates Akka Streams in action.
 * 
 * @author Adam L. Davis
 */
public class AkkaStreamsDemo implements ReactiveStreamsDemo {

    static final Materializer materializer = createMaterializer();

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
                .mapConcat(v -> Source.single(v).map(w -> w * w) //2
                        .runFold(asList(), (nil, x) -> asList(x), mat).toCompletableFuture().get())
                .runForeach(x -> squares.add((int) x), mat); //3

        try {Thread.sleep(300); } catch (Exception e) {}

        return squares;
    }

    public static CompletionStage<ArrayList<Integer>> parallelSquares() {
        return Source.range(1, 64) //2
                .mapConcat(v -> Source.from(asList(v)).map(w -> w * w) //2
                        .runFold(asList(), (nil, x) -> asList(x), materializer).toCompletableFuture().get())
                .runFold(new ArrayList<Integer>(), (list, x) -> {
                    list.add((int) x);
                    return list;
                }, materializer).toCompletableFuture();
    }

    /** Creating a materializer with more configuration. */
    public static Materializer createMaterializer() {
        final ActorSystem system = ActorSystem.create("reactive-messages"); // 1
        ActorMaterializerSettings settings = ActorMaterializerSettings.create(system) //2
                .withMaxFixedBufferSize(100) //3
                .withInputBuffer(8, 16); //4

        return ActorMaterializer.create(settings, system); //5
    }


    @Override
    public java.util.concurrent.Future<List<Integer>> doSquaresAsync(int count) {
        return Source.range(1, count).map(x -> x * x)
                .toMat(Sink.seq(), Keep.right()).run(materializer).toCompletableFuture();
    }

    @Override
    public java.util.concurrent.Future<String> doStringConcatAsync(int count) {
        return Source.range(0, count - 1).map(x -> "i=" + x)
                .fold(new StringBuilder(), (builder, it) -> builder.append(it))
                .map(StringBuilder::toString)
                .toMat(Sink.last(), Keep.right()).run(materializer)
                .toCompletableFuture();
    }

    @Override
    public java.util.concurrent.Future<List<Integer>> doParallelSquaresAsync(int count) {
        return Source.range(1, count)
                .mapAsyncUnordered(8, w ->
                        Source.single(w).map(x -> x * x).runWith(Sink.last(), materializer))
                .toMat(Sink.seq(), Keep.right()).run(materializer).toCompletableFuture();
    }

    @Override
    public java.util.concurrent.Future<String> doParallelStringConcatAsync(int count) {
        return Source.range(0, count - 1).mapAsyncUnordered(8, x ->
                        Source.single(x).map(i -> "i=" + i).runWith(Sink.last(), materializer))
                .fold(new StringBuilder(), (builder, it) -> builder.append(it))
                .map(StringBuilder::toString)
                .toMat(Sink.last(), Keep.right()).run(materializer)
                .toCompletableFuture();
    }

    public final Collection<String> messageList = new ConcurrentLinkedDeque<>();

    /** Adds error messages to messageList. */
    public void printErrors() {

        final ActorSystem system = ActorSystem.create("reactive-messages"); // 1
        final Materializer mat = ActorMaterializer.create(system); // 2

        Source<String, NotUsed> messages = getMessages();
        final Source<String, NotUsed> errors = messages
                .filter(m -> m.startsWith("Error")) // 3
                .buffer(200, OverflowStrategy.fail())
                .alsoTo(Sink.foreach(messageList::add)).map(m -> m.toString()); // 4

        errors.runWith(Sink.foreach(System.out::println), mat); // 5
    }

    public Source<String, NotUsed> getMessages() {
        return Source.fromPublisher(publisher);
    }

    Publisher<String> publisher;

    public void setChannel(Channel channel) {
        publisher = Flux.create(sink -> {
            sink.onRequest(channel::poll)
                .onCancel(channel::cancel)
                .onDispose(channel::close);
            channel.register(sink::next);
        });
    }

    public Sink<String, CompletionStage<IOResult>> lineSink(String filename) {
        return Flow.of(String.class)
                .map(s -> ByteString.fromString(s.toString() + "\n"))
                .toMat(FileIO.toPath(Paths.get(filename)), Keep.right());
    }

    public void saveTextFile(List<String> text) {
        Sink<String, CompletionStage<IOResult>> sink = lineSink("testfile.txt");
        Source.from(text).runWith(sink, materializer);
    }

    public Graph<SinkShape<String>, NotUsed> createFileSinkGraph() {
        return GraphDSL.create(builder -> {
            FlowShape<String, String> flowShape = builder
                    .add(Flow.of(String.class).async()); //1
            var sink = lineSink("testfile.txt"); //2
            var sinkShape = builder.add(sink); //3

            builder.from(flowShape.out()).to(sinkShape); //4
            return new SinkShape<>(flowShape.in()); //5
        });
    }

    public void saveTextFileUsingGraph(List<String> text) {
        Sink.fromGraph(createFileSinkGraph())
                .runWith(Source.from(text), materializer);
    }
}
