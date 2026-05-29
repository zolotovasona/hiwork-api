package com.example.demo.dto;

// Java 17+ record — авто-генерация конструктора, геттеров, equals, hashCode
public record LoginRequest(String email, String password, String code, String fullName) {}
