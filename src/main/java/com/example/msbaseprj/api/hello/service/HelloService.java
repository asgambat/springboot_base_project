package com.example.msbaseprj.api.hello.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.msbaseprj.api.hello.exception.CensuredWordException;

@Service
public class HelloService implements IHelloService {
    private static final Logger log = LoggerFactory.getLogger(HelloService.class);

    @Override
    public String hello(String name) {
        log.info("Richiesta hello per name={}", name);

        //add a random failure to test the exception handler
        if (Math.random() < 0.3) 
            throw new RuntimeException("Random failure for testing purposes");

        if( name.contains("ass") )
            throw new CensuredWordException("ass");
        
        return "Hello, " + name + "!";
    }

}
