package com.example.agent.skills.skill;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimeSkill implements Skill {

    @Override
    public String getName() {
        return "time";
    }

    @Override
    public String getDescription() {
        return "Gets current date and time";
    }

    @Override
    public String execute(String input) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "Current time: " + now.format(formatter);
    }
}