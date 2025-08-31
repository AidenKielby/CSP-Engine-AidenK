package com.aiden;

import java.util.ArrayList;
import java.util.Objects;

public class Constraint {
    private String type;
    private ArrayList<Variable> variables;
    private ArrayList<Double> values;
    private double modifier;

    public Constraint(String type, ArrayList<Variable> variables, ArrayList<Double> values, double modifier) {
        this.type = type;
        this.variables = variables;
        this.values = values;
        this.modifier = modifier;
    }

    public ArrayList<Variable> getVariables() {
        return variables;
    }

    public String getType(){
        return type;
    }

    public double evaluate(){
        if (Objects.equals(this.type, "equal")){
            Variable compareToVar = variables.get(0);
            for (Variable var : variables){
                if (!Objects.equals(compareToVar.getValue(), var.getValue())){
                    return 0 + modifier;
                }
            }
            return 1 + modifier;
        }

        if (Objects.equals(this.type, "not_equal")) {
            for (int i = 0; i < variables.size(); i++) {
                for (int j = i + 1; j < variables.size(); j++) {
                    if (Objects.equals(variables.get(i).getValue(), variables.get(j).getValue())) {
                        return 0 + modifier;
                    }
                }
            }
            return 1 + modifier;
        }

        if (Objects.equals(this.type, "value_in")){
            for (Variable var : variables) {
                boolean valIn = false;
                for (double d : values) {
                    if (Objects.equals(d, var.getValue())) {
                        valIn = true;
                    }
                }
                if (!valIn){
                    return 0 + modifier;
                }
            }
            return 1 + modifier;
        }

        if (Objects.equals(this.type, "value_not_in")){
            for (Variable var : variables) {
                for (double d : values) {
                    if (Objects.equals(d, var.getValue())) {
                        return 0 + modifier;
                    }
                }
            }
            return 1 + modifier;
        }

        return 100000;
    }

}
