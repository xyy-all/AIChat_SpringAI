package com.example.aiagent.service;

import com.example.aiagent.skill.Skill;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SkillService {

    private final Map<String, Skill> skills = new HashMap<>();

    public SkillService(List<Skill> skillList) {
        for (Skill skill : skillList) {
            skills.put(skill.getName().toLowerCase(), skill);
        }
    }

    public List<Skill> getAllSkills() {
        return List.copyOf(skills.values());
    }

    public List<Map<String, String>> getAllSkillInfos() {
        return skills.values().stream()
                .map(skill -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("name", skill.getName());
                    info.put("description", skill.getDescription());
                    return info;
                })
                .toList();
    }

    public Optional<Skill> getSkill(String name) {
        return Optional.ofNullable(skills.get(name.toLowerCase()));
    }

    public String executeSkill(String skillName, String input) {
        Optional<Skill> skill = getSkill(skillName);
        if (skill.isPresent()) {
            return skill.get().execute(input);
        }
        return "Skill not found: " + skillName;
    }
}