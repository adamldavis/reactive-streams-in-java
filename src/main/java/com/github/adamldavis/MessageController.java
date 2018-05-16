package com.github.adamldavis;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MessageController {

    @Autowired
    private Channel channel;
    
    final List<String> types = Arrays.asList("Error", "Warning", "Info");

    @GetMapping("/create-messages/{param}")
    public void createMessages(@PathVariable Integer param) {
        Random rnd = new Random();
        for (int i = 0; i < param; i++) {
            String msg = types.get(rnd.nextInt(3)) + ": message " + i;
            channel.publish(msg);
            System.out.println("Posted message : " + msg);
        }
    }
}
