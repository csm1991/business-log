package com.csm.demoserver.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录接口入参
 *
 * @author Simon Cai
 * @version 1.0
 * @since 2025-05-10
 */
@Data
public class LoginReq implements Serializable {

    private String username;

    private String password;
}
