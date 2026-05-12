package com.example.msbaseprj.api.hello.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.msbaseprj.api.hello.model.HelloResponse;
import com.example.msbaseprj.api.hello.service.IHelloService;

@RestController
@RequestMapping("/api/hello")
public class HelloController {
    private final IHelloService helloService;
    
    public HelloController(IHelloService helloService) {
        this.helloService = helloService;
    }

    @GetMapping
    public HelloResponse hello(
            @RequestParam(value = "name", defaultValue = "World") String name) {
        return new HelloResponse(helloService.hello(name));
    }

}
