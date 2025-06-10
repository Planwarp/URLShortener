package ru.URLShortener.URLShortener.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllErrors(Exception e) {
        System.out.println("Ой, ошибка: " + e.getMessage());
        e.printStackTrace();
        return new ResponseEntity<>("Ошибка сервера: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR); // 500
    }
}