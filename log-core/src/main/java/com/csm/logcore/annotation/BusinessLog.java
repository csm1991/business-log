package com.csm.logcore.annotation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.lang.annotation.*;

/**
 * 业务日志注解
 *
 * @author Simon Cai
 * @version 1.0
 * @since 2025-05-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BusinessLog {

    /**
     * 日志写入参数，支持SpEL表达式
     */
    String param() default "";

    /**
     * 需要记日志的数据对应的查询条件，目前只支持主键ID，支持集合
     */
    String querySql() default "";

    /**
     * 是否需要比较前后差异
     */
    boolean compare() default false;

    /**
     * 查询数据对应的mapper类
     */
    Class<? extends BaseMapper> mapper() default BaseMapper.class;

    /**
     * 日志类型，入口指定类型，写入队列根据类型拼接真实文案
     * 国际化
     */
    String logType();

    /**
     * 判断条件，满足条件的才会写入日志，支持SpEL表达式
     */
    String condition() default "";

    /**
     * 用户ID，可选参数，如果不提供则默认从ThreadLocal获取
     */
    String createBy() default "";
}
