package com.rainbowsea.tidesound.account.service.impl;

import com.rainbowsea.tidesound.account.mapper.UserAccountMapper;
import com.rainbowsea.tidesound.account.service.MqOpsService;
import com.rainbowsea.tidesound.common.execption.GuiguException;
import com.rainbowsea.tidesound.model.account.UserAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {


    @Autowired
    private UserAccountMapper userAccountMapper;


    @Override
    public void userAccountRegister(String content) {
        try {
            UserAccount userAccount = new UserAccount();
            userAccount.setUserId(Long.parseLong(content));
            int insert = userAccountMapper.insert(userAccount);


            log.info("初始化用户账户：{}", insert > 0 ? "success" : "fail");

        } catch (Exception e) {
            throw new GuiguException(201, "服务内部处理数据失败");
        }

    }
}
