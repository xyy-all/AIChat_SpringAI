package com.example.aiagent.skill;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class WeatherSkill implements Skill {

    private final Random random = new Random();
    private final String[] conditions = {"Sunny", "Cloudy", "Rainy", "Snowy", "Windy"};

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getDescription() {
        return "Gets current weather for a location (simulated)";
    }

    @Override
    public String execute(String input) {
        String location = input.trim();
        if (location.isEmpty()) {
            location = "unknown location";
        }
        String condition = conditions[random.nextInt(conditions.length)];
        int temp = random.nextInt(30) + 10; // 10-40°C
        return String.format("Weather in %s: %s, %d°C", location, condition, temp);
    }
}