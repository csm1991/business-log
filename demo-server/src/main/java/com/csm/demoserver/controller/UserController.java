package com.csm.demoserver.controller;

import com.csm.demoserver.constants.BusinessLogConstants;
import com.csm.demoserver.req.LoginReq;
import com.csm.demoserver.req.UserInfoReq;
import com.csm.logcore.annotation.BusinessLog;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 业务日志测试接口
 *
 * @author Simon Cai
 * @version 1.0
 * @since 2025-05-10
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping(value = "/login")
    @BusinessLog(param = "'username=' + #username + '&password=' +#password",logType = BusinessLogConstants.LOGIN)
    public String login(@RequestBody LoginReq req) {
        return UUID.randomUUID().toString();
    }

    @PostMapping(value = "/info")
    @BusinessLog(param = "'userId=' + #userId", querySql = "#userId", compare = false, logType = BusinessLogConstants.GET_USER_INFO)
    public String info(@RequestBody UserInfoReq req) {
        return UUID.randomUUID().toString();
    }
}
