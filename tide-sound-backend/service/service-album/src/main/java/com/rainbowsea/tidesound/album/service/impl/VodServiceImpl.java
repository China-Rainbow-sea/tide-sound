package com.rainbowsea.tidesound.album.service.impl;

import com.rainbowsea.tidesound.album.config.VodConstantProperties;
import com.rainbowsea.tidesound.album.service.VodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

}
