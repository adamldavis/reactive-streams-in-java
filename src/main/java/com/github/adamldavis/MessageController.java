package com.github.adamldavis;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MessageController {

    @Autowired
    private Channel channel;
    
    final List<String> types = Arrays.asList("Error", "Warning", "Info");

    final String INFO = "Channel is running? %s, name: %s, pollCount: %d. (Use /messages/100 to add 100 messages)\n";
    final String POSTED = "Posted %d messages.\n";

    @GetMapping("/messages/{param}")
    public ResponseEntity<String> createMessages(@PathVariable Integer param) {
        Random rnd = new Random();
        for (int i = 0; i < param; i++) {
            String msg = types.get(rnd.nextInt(3)) + ": message " + i;
            channel.publish(msg);
            System.out.println("Posted message : " + msg);
        }
        return ResponseEntity.ok(String.format(POSTED, param) + getInfo());
    }

    @GetMapping("/")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok(getInfo());
    }

    private String getInfo() {
        return String.format(INFO, channel.isAlive(), channel.getName(), channel.pollCount);
    }
}
