package com.elmlite.platform.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan("com.elmlite.platform.mapper")
public class MybatisConfig {
}
