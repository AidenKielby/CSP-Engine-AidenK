package com.aiden;

import java.util.ArrayList;

public class Variable {
    private String variableName;
    private double value;
    private ArrayList<Double> domain;
    private int cost;

    public Variable(String variableName, ArrayList<Double> domain, int cost) {
        this.variableName = variableName;
        this.domain = domain;
        this.cost = cost;
    }

    public Variable(String variableName, ArrayList<Double> domain, double value) {
        this.variableName = variableName;
        this.domain = domain;
        this.value = value;

    }

    public double getValue() {
        return value;
    }

    public double getCost() {
        return cost;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public ArrayList<Double> getDomain() {
        return domain;
    }

    public void setDomain(ArrayList<Double> domain) {
        this.domain = domain;
    }

    public String getVariableName() {
        return variableName;
    }
}
