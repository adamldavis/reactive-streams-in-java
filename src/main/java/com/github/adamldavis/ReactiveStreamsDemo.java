package com.github.adamldavis;

import java.util.List;
import java.util.concurrent.Future;

/** Interface to implement for each implementation of reactive streams in Java for performance testing. */
public interface ReactiveStreamsDemo {

    Future<List<Integer>> doSquaresAsync(int count);

    Future<String> doStringConcatAsync(int count);

    Future<List<Integer>> doParallelSquaresAsync(int count);

    Future<String> doParallelStringConcatAsync(int count);

}
