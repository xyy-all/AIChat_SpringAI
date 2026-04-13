package com.example.aiagent.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.example.aiagent.handle.FastJsonTypeHandler;
import com.example.aiagent.handle.ListDoubleTypeHandler;
import com.example.aiagent.handle.MessageRoleTypeHandler;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class MyBatisConfig {

    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {

        return new ConfigurationCustomizer() {
            @Override
            public void customize(MybatisConfiguration configuration) {
                // 注册自定义 TypeHandler
                configuration.getTypeHandlerRegistry().register(ListDoubleTypeHandler.class);
                configuration.getTypeHandlerRegistry().register(FastJsonTypeHandler.class);
                configuration.getTypeHandlerRegistry().register(MessageRoleTypeHandler.class);
            }
        };
    }
}