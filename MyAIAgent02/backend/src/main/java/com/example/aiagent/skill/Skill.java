package com.example.aiagent.skill;

public interface Skill {
    String getName();
    String getDescription();
    String execute(String input);
}