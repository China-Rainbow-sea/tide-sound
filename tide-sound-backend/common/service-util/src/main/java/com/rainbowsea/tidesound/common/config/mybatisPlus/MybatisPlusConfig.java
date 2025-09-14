package com.rainbowsea.tidesound.common.config.mybatisPlus;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.rainbowsea.tidesound.common.interceptor.SqlInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MybatisPlus配置类
 *
 */
@EnableTransactionManagement
@Configuration
@MapperScan("com.rainbowsea.tidesound.*.mapper")
public class MybatisPlusConfig {

    /**
     *
     * @return
     */
    @Bean
    public MybatisPlusInterceptor optimisticLockerInnerInterceptor(){
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //向Mybatis过滤器链中添加分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }


    /**
     * SQL 拦截器:
     * 可以将 Mybatis-plus 的打印日志的 ？ 自动填充对应的值上去，同时会打印显示该 SQL 执行的耗时。
     * @return
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {


//        ConfigurationCustomizer customizer = new ConfigurationCustomizer() {
//            @Override
//            public void customize(MybatisConfiguration configuration) {
//                configuration.addInterceptor(new SqlInterceptor());
//            }
//        };
//        return customizer;

        return configuration -> configuration.addInterceptor(new SqlInterceptor());
    }

}
