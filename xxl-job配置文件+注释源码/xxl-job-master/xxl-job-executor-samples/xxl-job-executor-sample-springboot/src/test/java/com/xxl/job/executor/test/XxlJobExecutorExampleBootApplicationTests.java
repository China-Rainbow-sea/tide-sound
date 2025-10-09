package com.xxl.job.executor.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.util.InetUtils;

import java.net.InetAddress;

@SpringBootTest
public class XxlJobExecutorExampleBootApplicationTests {

    @Autowired
    private InetUtils utils;

    @Test
    public void test() {
        InetAddress firstNonLoopbackAddress = utils.findFirstNonLoopbackAddress();
        String hostAddress = firstNonLoopbackAddress.getHostAddress();
        System.out.println(hostAddress);
    }

}