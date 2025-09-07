package com.rainbowsea.tidesound.album.service.impl;

import com.rainbowsea.tidesound.album.mapper.BaseCategory1Mapper;
import com.rainbowsea.tidesound.album.mapper.BaseCategory2Mapper;
import com.rainbowsea.tidesound.album.mapper.BaseCategory3Mapper;
import com.rainbowsea.tidesound.album.service.BaseCategoryService;
import com.rainbowsea.tidesound.model.album.BaseCategory1;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;


}
