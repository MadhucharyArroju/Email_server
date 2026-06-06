package com.example.email.java1point8;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RemoveDuplicates {

    public static void main(String[] args) {
        String test = "abcabc";
        String res = IntStream.range(0, test.length()).mapToObj(i -> test.charAt(test.length() - 1 - i)).
                map(String::valueOf).collect(Collectors.joining());
        System.out.println("Reverse++" + res);
        String result =   removeDuplicates(test);
        System.out.println("result++" + result);
        String result8 =   removeDuplicatesonepoint8(test);
        System.out.println("result8++" + result8);
    }

    public static String removeDuplicates(String input) {
        if (input == null) {
            return "No Such String";
        }
        Set<Character> seen = new HashSet<>();
        StringBuilder sb = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (seen.add(c)) { // add() returns false if already present
                sb.append(c);
            }
        }
        return sb.toString();

    }

    public static String removeDuplicatesonepoint8(String input) {
        if (input == null) return null;

        // Using LinkedHashSet to maintain insertion order
        Set<Character> seen = new LinkedHashSet<>();

        return IntStream.range(0, input.length())
                .mapToObj(input::charAt)
                .filter(seen::add) // Only adds if not already present
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

}
