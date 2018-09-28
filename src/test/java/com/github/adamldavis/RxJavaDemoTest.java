package com.github.adamldavis;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.adamldavis.DemoData.squares;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RxJavaDemoTest {

    RxJavaDemo demo = new RxJavaDemo();

    @Test
    public void testDoSquares() {
        assertArrayEquals(squares.toArray(), demo.doSquares().toArray());
    }

    @Test
    public void testDoParallelSquares() {
        List result = demo.doParallelSquares()
                .stream().sorted().collect(Collectors.toList());
                
        assertArrayEquals(squares.toArray(), result.toArray());
    }

    @Test
    public void testrunComputation() throws Exception {
        demo.runComputation();
        Thread.sleep(1100);
    }

    @Test
    public void testReadFile() {
        demo.readFile(new File("README.md"));
    }

    @Test
    public void testReadFile2() {
        demo.readFile2(new File("README.md"));
    }

    @Test
    public void testBackpressure() throws InterruptedException {
        assertEquals(10, demo.countUsingBackpressure(1050));
    }

    @Test
    public void testSubscriber() {
        TestSubscriber<Integer> ts =
                Flowable.range(1, 5).test();

        assertEquals(5, ts.valueCount());
    }

    @Test
    public void testSubscriberWithException() {
        Flowable<Integer> flowable = Flowable.create(source -> {
            source.onNext(1);
            source.onError(new RuntimeException());
        }, BackpressureStrategy.LATEST);

        TestSubscriber<Integer> ts = flowable.test();

        ts.assertSubscribed();
        ts.assertError(RuntimeException.class);
    }

    @Test
    public void testObserver() {
        TestObserver<Integer> ts =
                Observable.range(1, 5).test();

        assertEquals(5, ts.valueCount());
    }

    @Test
    public void testScheduler() {
        TestScheduler scheduler = new TestScheduler(); //1
        Observable<Long> tick = Observable
                .interval(1, TimeUnit.SECONDS, scheduler); //2
        Observable<String> observable =
                Observable.just("foo", "bar", "biz", "baz") //3
                .zipWith(tick, (string, index) -> index + "-" + string);//4
        TestObserver<String> testObserver = observable
                .subscribeOn(scheduler).test();//5

        scheduler.advanceTimeBy(2300, TimeUnit.MILLISECONDS);//6

        testObserver.assertNoErrors(); //7
        testObserver.assertValues("0-foo", "1-bar");
        testObserver.assertNotComplete();
    }

}
