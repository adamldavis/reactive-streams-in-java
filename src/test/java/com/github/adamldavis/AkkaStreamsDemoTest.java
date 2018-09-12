package com.github.adamldavis;

import static com.github.adamldavis.DemoData.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class AkkaStreamsDemoTest {

    AkkaStreamsDemo demo = new AkkaStreamsDemo();

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
        
    Channel channel = new Channel();

    @Test
    public void testPrintErrors() {
        // given
        demo.setChannel(channel);
        // when
        demo.printErrors();

        int count = 201;

        for (int i = 0; i < count; i++) {
            channel.publish("Error: " + i);
        }
        try { Thread.sleep(2000); } catch (Exception e) { throw new RuntimeException(e); }
        // then
        assertFalse(demo.messageList.isEmpty());
        assertEquals(count, demo.messageList.size());
        assertNotNull(demo.publisher);
    }

}
