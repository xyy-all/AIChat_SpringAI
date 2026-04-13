package com.example.agent.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.agent.api.entity.StatisticsDailyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日统计表Mapper接口
 */
@Mapper
public interface StatisticsDailyMapper extends BaseMapper<StatisticsDailyEntity> {

    /**
     * 根据日期范围查询统计记录
     */
    List<StatisticsDailyEntity> selectByDateRange(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    /**
     * 查询最近的统计记录
     */
    List<StatisticsDailyEntity> selectRecent(@Param("limit") int limit);

    /**
     * 更新或插入统计记录（upsert）
     */
    int upsert(StatisticsDailyEntity entity);

    /**
     * 批量更新或插入统计记录
     */
    int batchUpsert(@Param("list") List<StatisticsDailyEntity> entities);

    /**
     * 获取最新的统计日期
     */
    LocalDate selectLatestStatDate();

    /**
     * 获取统计汇总信息
     */
    StatisticsSummary selectSummary(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /**
     * 统计汇总结果类
     */
    class StatisticsSummary {
        private Long totalSessions;
        private Long totalMessages;
        private Long totalTokens;
        private Long skillExecutions;
        private Long documentUploads;

        public StatisticsSummary() {
        }

        public StatisticsSummary(Long totalSessions, Long totalMessages, Long totalTokens,
                                 Long skillExecutions, Long documentUploads) {
            this.totalSessions = totalSessions;
            this.totalMessages = totalMessages;
            this.totalTokens = totalTokens;
            this.skillExecutions = skillExecutions;
            this.documentUploads = documentUploads;
        }

        public Long getTotalSessions() {
            return totalSessions;
        }

        public void setTotalSessions(Long totalSessions) {
            this.totalSessions = totalSessions;
        }

        public Long getTotalMessages() {
            return totalMessages;
        }

        public void setTotalMessages(Long totalMessages) {
            this.totalMessages = totalMessages;
        }

        public Long getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Long totalTokens) {
            this.totalTokens = totalTokens;
        }

        public Long getSkillExecutions() {
            return skillExecutions;
        }

        public void setSkillExecutions(Long skillExecutions) {
            this.skillExecutions = skillExecutions;
        }

        public Long getDocumentUploads() {
            return documentUploads;
        }

        public void setDocumentUploads(Long documentUploads) {
            this.documentUploads = documentUploads;
        }
    }
}