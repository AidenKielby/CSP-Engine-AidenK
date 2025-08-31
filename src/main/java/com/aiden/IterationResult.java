package com.aiden;

import java.util.ArrayList;

public class IterationResult {
    private final double bestValue;
    private final ArrayList<ArrayList<Variable>> variables;

    public IterationResult(double bestValue, ArrayList<ArrayList<Variable>> variables) {
        this.bestValue = bestValue;
        this.variables = variables;
    }

    public double getBestValue() {
        return bestValue;
    }

    public ArrayList<ArrayList<Variable>> getVariables() {
        return variables;
    }
}
