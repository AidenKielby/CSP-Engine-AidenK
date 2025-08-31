package com.aiden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Problem {
    private ArrayList<Variable> variables = new ArrayList<>();
    private ArrayList<Constraint> constraints = new ArrayList<>();
    private boolean maximiseFairness = false;
    private double highestValue = 0;
    private double lowestAvr = 1000000;
    private ArrayList<ArrayList<Variable>> vars = new ArrayList<>();

    public Problem(String FILE_PATH) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        File file = new File(FILE_PATH);

        if (!file.exists()) {
            System.err.println("YAML file not found: " + file.getAbsolutePath());
            return;
        }

        try {
            JsonNode root = mapper.readTree(file);

            JsonNode fairnessObjectiveNode = root.get("fairnessObjective");
            if (fairnessObjectiveNode == null || !fairnessObjectiveNode.isObject()) {
                System.err.println("Missing or invalid 'fairnessObjective' section.");
                return;
            }

            JsonNode objective = fairnessObjectiveNode.get("objective");
            if (objective != null) {
                if (Objects.equals(objective.textValue(), "balance")){
                    maximiseFairness = true;
                }
            }

            // Parse domains
            JsonNode domainsNode = root.get("variables");
            if (domainsNode == null || !domainsNode.isObject()) {
                System.err.println("Missing or invalid 'variables' section.");
                return;
            }

            Iterator<Map.Entry<String, JsonNode>> fields = domainsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String varName = entry.getKey();
                JsonNode values = entry.getValue();

                ArrayList<Double> domainValues = new ArrayList<>();
                for (JsonNode node : values) {
                    domainValues.add(node.asDouble());
                }
                Variable variable = new Variable(varName, domainValues, 0);
                variables.add(variable);
            }

            // Parse constraints
            JsonNode constraintsNode = root.get("constraints");
            if (constraintsNode == null || !constraintsNode.isArray()) {
                System.err.println("Missing or invalid 'constraints' section.");
                return;
            }

            for (JsonNode constraint : constraintsNode) {
                String type = constraint.get("type").asText();

                String[] arithmetic_constraint = {"sum", "product", "difference", "modulo"};
                String[] boolean_constraint = {"equal", "not_equal","value_in", "value_not_in"};

                if (Arrays.asList(boolean_constraint).contains(type)){
                    List<JsonNode> vars = new ArrayList<>();
                    constraint.get("vars").forEach(vars::add);

                    ArrayList<Variable> result = new ArrayList<>();

                    for (JsonNode node : vars) {
                        String varName = node.asText();

                        for (Variable v : variables) {
                            if (v.getVariableName().equals(varName)) {
                                result.add(v);
                                break;
                            }
                        }
                    }

                    ArrayList<Double> real_values = new ArrayList<>();
                    if (constraint.has("values")) {
                        List<JsonNode> values = new ArrayList<>();
                        constraint.get("values").forEach(values::add);

                        for (JsonNode node : values) {
                            double value = node.asDouble();
                            real_values.add(value);
                        }
                    }
                    else {
                        real_values.add(0.0);
                    }

                    double modifier = constraint.get("modifier").asDouble();

                    Constraint temp_constraint = new Constraint(type, result, real_values, modifier);

                    constraints.add(temp_constraint);
                }

                if (Arrays.asList(arithmetic_constraint).contains(type)){
                    List<JsonNode> vars = new ArrayList<>();
                    constraint.get("vars").forEach(vars::add);

                    ArrayList<Variable> result = new ArrayList<>();

                    for (JsonNode node : vars) {
                        String varName = node.asText();

                        for (Variable v : variables) {
                            if (v.getVariableName().equals(varName)) {
                                result.add(v);
                                break;
                            }
                        }
                    }

                    double value = constraint.get("value").asDouble();
                    String comparison = constraint.get("comparison").asText();
                    double modifier = constraint.get("modifier").asDouble();

                    ArithmeticConstraint temp_constraint = new ArithmeticConstraint(type, result, comparison, value, modifier);

                    constraints.add(temp_constraint);
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading YAML: " + e.getMessage());
        }
    }

    private IterationResult iterate(int depth, int maxDepth, Variable prevVar){
        if (depth < maxDepth){
            ArrayList<Double> domain = variables.get(depth).getDomain();

            IterationResult highestValue = new IterationResult(0, new ArrayList<>());

            for (int DomainIndex = 0; DomainIndex < domain.size(); DomainIndex++) {
                variables.get(depth).setValue(domain.get(DomainIndex));
                IterationResult returned = iterate(depth+1, maxDepth, variables.get(depth));
                if (returned.getBestValue() > highestValue.getBestValue()){
                    highestValue = returned;
                }
            }

            return highestValue;
        }
        else {
            ArrayList<Double> domain = variables.get(depth).getDomain();
            ArrayList<Variable> vars1 = new ArrayList<>();

            for (int DomainIndex = 0; DomainIndex < domain.size(); DomainIndex++) {
                boolean broken = false;
                variables.get(depth).setValue(domain.get(DomainIndex));
                double summedConstraintResults = 1;
                for (int ConstraintIndex = 0; ConstraintIndex < constraints.size(); ConstraintIndex++) {
                    double evaluated = constraints.get(ConstraintIndex).evaluate();
                    if (evaluated == 0 || evaluated < highestValue/4){
                        broken = true;
                        break;
                    }
                    summedConstraintResults *= evaluated;
                }
                if (! maximiseFairness){
                    if (summedConstraintResults > highestValue && !broken){
                        highestValue = summedConstraintResults;
                        vars1.clear();
                        for (Variable v : variables) {
                            vars1.add(new Variable(v.getVariableName(), v.getDomain(), v.getValue()));
                        }
                        vars.add(vars1);
                    }
                }
                else {
                    double mean = 0;
                    for (Variable v : variables) {
                        mean += v.getValue();
                    }
                    mean /= variables.size();

                    double variance = 0;
                    for (Variable v : variables) {
                        double diff = v.getValue() - mean;
                        variance += diff * diff;
                    }
                    variance /= variables.size();

                    if (summedConstraintResults >= highestValue && !broken && variance < lowestAvr){
                        highestValue = summedConstraintResults;
                        lowestAvr = variance;
                        vars1.clear();
                        for (Variable v : variables) {
                            vars1.add(new Variable(v.getVariableName(), v.getDomain(), v.getValue()));
                        }
                        vars.add(vars1);
                    }
                }

            }
            return new IterationResult(highestValue, vars);
        }
    }

    public IterationResult executeCSP(){
        return iterate(0, variables.size()-1, null);
    }

}
