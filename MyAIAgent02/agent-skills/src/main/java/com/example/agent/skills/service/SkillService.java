package com.example.agent.skills.service;

import com.example.agent.skills.skill.Skill;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 技能服务接口，定义技能查询与执行的统一入口。
 */
public interface SkillService {

    List<Skill> getAllSkills();

    List<Map<String, String>> getAllSkillInfos();

    Optional<Skill> getSkill(String name);

    String executeSkill(String skillName, String input);
}
