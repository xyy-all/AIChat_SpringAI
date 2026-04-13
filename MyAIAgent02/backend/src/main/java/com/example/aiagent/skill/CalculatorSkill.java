package com.example.aiagent.skill;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CalculatorSkill implements Skill {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "Performs basic arithmetic calculations (addition, subtraction, multiplication, division)";
    }

    @Override
    public String execute(String input) {
        // Simple arithmetic parsing
        Pattern pattern = Pattern.compile("(\\d+)\\s*([+\\-*/])\\s*(\\d+)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            double a = Double.parseDouble(matcher.group(1));
            double b = Double.parseDouble(matcher.group(3));
            String op = matcher.group(2);
            double result;
            switch (op) {
                case "+": result = a + b; break;
                case "-": result = a - b; break;
                case "*": result = a * b; break;
                case "/":
                    if (b == 0) return "Error: division by zero";
                    result = a / b;
                    break;
                default: return "Unsupported operator";
            }
            return String.format("%f %s %f = %f", a, op, b, result);
        }
        return "Could not parse calculation. Please use format: number operator number (e.g., 2 + 3)";
    }
}