package com.example.agent.skills.skill;

public interface Skill {
    String getName();
    String getDescription();
    String execute(String input);
}