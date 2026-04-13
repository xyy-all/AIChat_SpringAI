package com.example.agent.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.agent.api.entity.OperationAuditEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作审计表Mapper接口
 */
@Mapper
public interface OperationAuditMapper extends BaseMapper<OperationAuditEntity> {

    /**
     * 根据操作类型查询审计记录
     */
    List<OperationAuditEntity> selectByOperationType(String operationType);

    /**
     * 根据会话ID查询审计记录
     */
    List<OperationAuditEntity> selectBySessionId(String sessionId);

    /**
     * 根据用户ID查询审计记录
     */
    List<OperationAuditEntity> selectByUserId(String userId);

    /**
     * 根据时间范围查询审计记录
     */
    List<OperationAuditEntity> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 批量插入审计记录
     */
    int batchInsert(@Param("list") List<OperationAuditEntity> audits);

    /**
     * 清理指定时间之前的审计记录
     */
    int cleanupBeforeTime(LocalDateTime beforeTime);

    /**
     * 统计操作类型数量
     */
    List<OperationTypeCount> countByOperationType(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 操作类型统计结果类
     */
    class OperationTypeCount {
        private String operationType;
        private Long count;

        public OperationTypeCount() {
        }

        public OperationTypeCount(String operationType, Long count) {
            this.operationType = operationType;
            this.count = count;
        }

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}