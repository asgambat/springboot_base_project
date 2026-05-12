package com.example.msbaseprj.api.hello.exception;

public class CensuredWordException extends RuntimeException {

    public CensuredWordException(String message) {
        super("Found a word that is not tollerated: " + message);
    }

}
