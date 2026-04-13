package com.example.agent.skills.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.agent.skills.entity.SkillExecutionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 技能执行记录表Mapper接口
 */
@Mapper
public interface SkillExecutionMapper extends BaseMapper<SkillExecutionEntity> {

    /**
     * 批量插入执行记录
     */
    int batchInsert(@Param("list") List<SkillExecutionEntity> executions);

    /**
     * 根据会话ID查询执行记录
     */
    List<SkillExecutionEntity> selectBySessionId(String sessionId);

    /**
     * 根据技能名称查询执行记录
     */
    List<SkillExecutionEntity> selectBySkillName(String skillName);

    /**
     * 查询成功的执行记录
     */
    List<SkillExecutionEntity> selectSuccessfulExecutions();

    /**
     * 查询失败的执行记录
     */
    List<SkillExecutionEntity> selectFailedExecutions();

    /**
     * 根据会话ID和技能名称查询
     */
    List<SkillExecutionEntity> selectBySessionIdAndSkillName(@Param("sessionId") String sessionId, @Param("skillName") String skillName);

    /**
     * 查询会话的技能执行次数
     */
    int countBySessionId(String sessionId);

    /**
     * 查询技能的执行次数
     */
    int countBySkillName(String skillName);
}