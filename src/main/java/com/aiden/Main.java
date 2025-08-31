package com.aiden;


import java.util.ArrayList;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Create a scanner to read user input
        Scanner scanner = new Scanner(System.in);

        // Ask the user for the file location
        System.out.print("Please enter the file location (e.g., 'src/main/resources/problem.yaml'): ");
        String inputFilePath = scanner.nextLine();

        // Create the Problem instance using the file path from user input
        Problem problem = new Problem(inputFilePath);

        // Inform the user that the process is starting
        System.out.println("Running the CSP solver...");

        // Execute the CSP and get the result
        IterationResult result = problem.executeCSP();

        // Display the best value found by the solver
        System.out.println("Best Value: " + result.getBestValue());

        // Get the list of variables and display their names and values
        ArrayList<ArrayList<Variable>> vars = result.getVariables();
        System.out.println("\nVariable Assignments:");
        for (ArrayList<Variable> v : vars) {
            System.out.println("\n------------------------\n");
            for (Variable va : v) {
                System.out.println("Name: " + va.getVariableName() + " | Value: " + va.getValue());
            }
        }

        // Close the scanner to prevent resource leak
        scanner.close();
    }
}