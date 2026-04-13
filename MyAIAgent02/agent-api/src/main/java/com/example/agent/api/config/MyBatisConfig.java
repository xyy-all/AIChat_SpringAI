package com.example.agent.api.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.example.agent.core.handle.FastJsonTypeHandler;
import com.example.agent.core.handle.FastJsonListTypeHandler;
import com.example.agent.core.handle.MessageRoleTypeHandler;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class MyBatisConfig {

    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {

        return new ConfigurationCustomizer() {
            @Override
            public void customize(MybatisConfiguration configuration) {
                // 注册自定义 TypeHandler
                configuration.getTypeHandlerRegistry().register(FastJsonListTypeHandler.class);
                configuration.getTypeHandlerRegistry().register(FastJsonTypeHandler.class);
                configuration.getTypeHandlerRegistry().register(MessageRoleTypeHandler.class);
            }
        };
    }
}