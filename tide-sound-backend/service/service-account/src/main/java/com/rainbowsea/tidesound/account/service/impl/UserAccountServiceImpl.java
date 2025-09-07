package com.rainbowsea.tidesound.account.service.impl;

import com.rainbowsea.tidesound.account.mapper.UserAccountMapper;
import com.rainbowsea.tidesound.account.service.UserAccountService;
import com.rainbowsea.tidesound.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;

}
