package com.example.aiagent.handle;

import com.example.aiagent.enums.MessageRole;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MessageRole 枚举类型处理器
 * 处理数据库 VARCHAR 与 Java 枚举之间的转换
 */
public class MessageRoleTypeHandler extends BaseTypeHandler<MessageRole> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, MessageRole parameter, JdbcType jdbcType) throws SQLException {
        // 将枚举转换为字符串存储到数据库（小写）
        ps.setString(i, parameter.name());
    }

    @Override
    public MessageRole getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String roleString = rs.getString(columnName);
        return roleString != null && !roleString.isEmpty() ? parseMessageRole(roleString) : null;
    }

    @Override
    public MessageRole getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String roleString = rs.getString(columnIndex);
        return roleString != null && !roleString.isEmpty() ? parseMessageRole(roleString) : null;
    }

    @Override
    public MessageRole getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String roleString = cs.getString(columnIndex);
        return roleString != null && !roleString.isEmpty() ? parseMessageRole(roleString) : null;
    }

    /**
     * 解析角色字符串为枚举
     * 支持大小写不敏感的匹配
     */
    private MessageRole parseMessageRole(String roleString) {
        try {
            return MessageRole.valueOf(roleString.trim());
        } catch (IllegalArgumentException e) {
            // 如果找不到对应的枚举值，返回默认值 USER
            return MessageRole.USER;
        }
    }
}
