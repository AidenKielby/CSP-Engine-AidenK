package com.aiden;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ArithmeticConstraint extends Constraint{
    private String type;
    private ArrayList<Variable> variables;
    private String comparison;
    private double value;
    private double modifier;

    public ArithmeticConstraint(String type, ArrayList<Variable> variables, String comparison, double value, double modifier) {
        super(type, variables, new ArrayList<>(List.of(value)), modifier);

        this.type = type;
        this.variables = variables;
        this.comparison = comparison;
        this.value = value;
        this.modifier = modifier;
    }

    public ArrayList<Variable> getVariables() {
        return variables;
    }

    public String getType(){
        return type;
    }

    private double compare(double a, double b, String comparison, double modifier) {
        switch (comparison) {
            case "equal":
                return Math.abs(a - b) < 1e-6 ? 1 + modifier : 0 + modifier;
            case "greater_than":
                return a > b ? 1 + modifier : 0 + modifier;
            case "less_than":
                return a < b ? 1 + modifier : 0 + modifier;
            default:
                System.err.println("Invalid comparison: " + comparison);
                return 0 + modifier;
        }
    }

    public double evaluate(){
        if (Objects.equals(this.type, "sum")){
            double varSum = 0;
            for (Variable var : variables) {
                varSum += var.getValue();
            }
            return compare(varSum, value, comparison, modifier);

        }

        if (Objects.equals(this.type, "product")){
            double varSum = 1;
            for (Variable var : variables) {
                varSum *= var.getValue();
            }
            return compare(varSum, value, comparison, modifier);

        }

        if (Objects.equals(this.type, "difference")){
            double varSum = variables.get(0).getValue();
            for (int i = 1; i < variables.size(); i++) {
                varSum -= variables.get(i).getValue();
            }
            return compare(varSum, value, comparison, modifier);

        }

        if (Objects.equals(this.type, "modulo")){
            double varSum = variables.get(0).getValue();
            for (int i = 1; i < variables.size(); i++) {
                double divisor = variables.get(i).getValue();
                if (divisor == 0) {
                    System.err.println("Modulo by zero in constraint.");
                    return 0 + modifier;
                }
                varSum %= divisor;
            }
            return compare(varSum, value, comparison, modifier);

        }

        return 100000;
    }

}
