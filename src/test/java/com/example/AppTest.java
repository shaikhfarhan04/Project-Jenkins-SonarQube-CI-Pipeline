package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AppTest {

    @Test
    public void testAddition() {
        assertEquals(10, App.add(5, 5));
    }

    @Test
    public void testMultiplication() {
        assertEquals(25, App.multiply(5, 5));
    }
}