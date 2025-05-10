package com.csm.logcore.aspect;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csm.logcore.entity.BusinessLogEntity;
import com.csm.logcore.annotation.BusinessLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务日志切面
 *
 * @author Simon Cai
 * @version 1.0
 * @since 2025-05-10
 */
@Aspect
@Component
@Slf4j
public class BusinessLogAspect {

    @Pointcut("@annotation(com.csm.logcore.annotation.BusinessLog)")
    public void initBusinessLogAspect() {
    }

    @Around(value = "initBusinessLogAspect()")
    public Object doAround(ProceedingJoinPoint jp) throws Throwable {

        //获取注解，获取失败直接返回，理论上不存在获取失败
        MethodSignature methodSignature = (MethodSignature) jp.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = jp.getArgs();
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        if (annotation == null) {
            log.error("BusinessLogAspect切换获取注解失败");
            return jp.proceed();
        }

        //约定日志相关的参数均在第一个入参中
        Object arg = args[0];

        //创建ExpressionParser对象
        ExpressionParser parser = new SpelExpressionParser();

        //初始化日志相关对象
        Map<Long, Object> beforeUpdateMap = new HashMap<>();
        List<Object> writeLogDataIdList = new ArrayList<>();

        try {
            //执行业务逻辑前的处理
            if (arg instanceof ArrayList) {
                //如果入参是集合，那么就遍历解析
                for (Object o : (ArrayList) arg) {
                    doBizBefore(o, annotation, parser, beforeUpdateMap, writeLogDataIdList);
                }
            } else {
                //如果入参是单个实体，那么就只需要解析一次
                doBizBefore(arg, annotation, parser, beforeUpdateMap, writeLogDataIdList);
            }
        } catch (Exception ex) {
            log.error("BusinessLogAspect执行前置操作异常，原因：", ex);
            return jp.proceed();
        }

        //实际的业务逻辑执行
        Object result = jp.proceed();

        //执行业务逻辑后的处理
        try {
            if (arg instanceof ArrayList) {
                //如果入参是集合，那么就遍历解析
                for (Object o : (ArrayList) arg) {
                    doBizAfter(o, annotation, parser, beforeUpdateMap, writeLogDataIdList);
                }
            } else {
                //如果入参是单个实体，那么就只需要解析一次
                doBizAfter(arg, annotation, parser, beforeUpdateMap, writeLogDataIdList);
            }
        } catch (Exception ex) {
            log.error("BusinessLogAspect执行后置操作异常，原因：", ex);
        }

        return result;
    }

    private void doBizBefore(Object arg, BusinessLog annotation, ExpressionParser parser, Map<Long, Object> beforeUpdateMap, List<Object> writeLogDataIdList) {

        //构造EvaluationContext对象
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(BeanUtil.beanToMap(arg));

        //判断querySql是否是null
        if (!StringUtils.hasLength(annotation.querySql())) {
            //如果无需前置查询，那么就插入一条空白记录，防止后面完全不写入日志
            writeLogDataIdList.add("");
            return;
        }

        //解析业务记录条件
        Expression queryExpression = parser.parseExpression(annotation.querySql());

        //计算表达式结果
        Object querySqlResult = queryExpression.getValue(context);
        if (querySqlResult == null) {
            return;
        }
        if (querySqlResult instanceof ArrayList) {
            writeLogDataIdList.addAll((ArrayList) querySqlResult);
        } else {
            writeLogDataIdList.add(querySqlResult);
        }

        //如果注解中的compare为true，则进行sql查询
        if (annotation.compare()) {

            //获取用于查询的mapper
            BaseMapper mapper = SpringUtil.getBean(annotation.mapper());

            //调用mapper方法查询结果，最后都添加到更新前map中
            List list = mapper.selectBatchIds(writeLogDataIdList);
            listToMap(beforeUpdateMap, list);
        }
    }

    private void doBizAfter(Object arg, BusinessLog annotation, ExpressionParser parser, Map<Long, Object> beforeUpdateMap, List<Object> writeLogDataIdList) {

        //最终要写入的日志集合
        List<BusinessLogEntity> logList = new ArrayList<>();

        //构造EvaluationContext对象
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(BeanUtil.beanToMap(arg));

        //遍历需要写入日志的记录
        for (Object item : writeLogDataIdList) {

            if (annotation.compare()) {
                //获取更新前对应的数据，如果无需比较就是空，所以直接获取判空就行了
                Object old = beforeUpdateMap.get(Long.valueOf(item.toString()));
                context.setVariable("old", old);
            }

            //对logContent字段进行SpEL表达式转换，放在这里解析，是因为如果需要前后比较，没办法就行统一解析
            JSONObject param = new JSONObject();
            if (StringUtils.hasLength(annotation.param())) {
                Expression logContentExpression = parser.parseExpression(annotation.param());
                Object result = logContentExpression.getValue(context);
                if (result != null) {
                    //将"a=1&b=2"按照"&"分割成键值对数组
                    String[] keyValuePairs = result.toString().split("&");

                    // 遍历键值对数组
                    for (String pair : keyValuePairs) {
                        // 按照"="分割键值对
                        String[] keyValue = pair.split("=");

                        // 将键值对添加到JsonObject中
                        param.put(keyValue[0], keyValue[1]);
                    }
                }
            }

            //如果存在写入日志判断条件，就进行验证，如果不符合就跳过
            if (StringUtils.hasLength(annotation.condition())) {
                Expression conditionExpression = parser.parseExpression(annotation.condition());
                if (!Boolean.parseBoolean(conditionExpression.getValue(context).toString())) {
                    continue;
                }
            }

            //生成日志写入实体，并添加到最终结果集合
            BusinessLogEntity businessLogEntity = new BusinessLogEntity();
            businessLogEntity.setLogType(annotation.logType());
            businessLogEntity.setBusinessId(item);
            businessLogEntity.setParam(param);
            if (!StringUtils.hasLength(annotation.createBy())) {
                businessLogEntity.setCreateBy("换成登录人ID，可以从ThreadLocal中获取");
            } else {
                businessLogEntity.setCreateBy(annotation.createBy());
            }

            logList.add(businessLogEntity);
        }

        log.info("最终生成日志：{}", JSONUtil.toJsonStr(logList));
        if (CollectionUtil.isNotEmpty(logList)) {

            //这里是实际写入日志的逻辑，可以发送往消息队列异步写入，也可以使用@Async异步写入

        }
    }

    private void listToMap(Map<Long, Object> resultMap, List source) {
        //校验参数
        if (CollectionUtils.isEmpty(source)) {
            return;
        }

        //解析对象中的主键字段
        Field tableIdField = getTableIdField(source.get(0));
        if (tableIdField == null) {
            log.error("写入操作日志，bean转map失败，入参：{}，原因：解析不到TableId注解", source);
            return;
        }

        //根据上面解析出来的主键字段，获取对应的主键值并且转换成map
        for (Object obj : source) {
            try {
                Long tableId = (Long) tableIdField.get(obj);
                if (tableId != null) {
                    resultMap.put(tableId, obj);
                }
            } catch (Exception ex) {
                log.error("写入操作日志，bean转map失败，入参：{}，原因：", source);
            }
        }
    }

    /**
     * 获取有@TableId注解的字段返回，即主键字段
     */
    private Field getTableIdField(Object firstObj) {
        Class<?> clazz = firstObj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(TableId.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }
}
