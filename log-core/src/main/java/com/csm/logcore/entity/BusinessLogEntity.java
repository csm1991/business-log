package com.csm.logcore.entity;

import cn.hutool.json.JSONObject;
import lombok.Data;

import java.io.Serializable;

/**
 * 业务日志最终生成的日志信息实体
 *
 * @author Simon Cai
 * @version 1.0
 * @since 2025-05-10
 */
@Data
public class BusinessLogEntity implements Serializable {

    private String logType;

    private Object businessId;

    private JSONObject param;

    private String createBy;
}
