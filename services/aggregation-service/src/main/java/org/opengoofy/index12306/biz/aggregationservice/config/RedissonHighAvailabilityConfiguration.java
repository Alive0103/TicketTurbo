/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengoofy.index12306.biz.aggregationservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Redis 高可用架构方案配置类
 * 默认不启用，如果大家想要测试主从和 Cluster 集群模式，放开 @Configuration 注释，以及选择对应 REDISSON_CONFIG_FILE
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 */
// @Configuration
public class RedissonHighAvailabilityConfiguration {

    /**
     * Master-Slaver 一主多从架构配置
     */
    private static final String REDISSON_CONFIG_FILE = "redisson-master-2slave.yaml";

    /**
     * Cluster 多主多从架构配置
     */
    // private static final String REDISSON_CONFIG_FILE = "redisson-cluster.yaml";

    /**
     * 通过 Redisson Yaml 文件方式加载 Redis 高可用方案
     */
    @Bean
    public RedissonClient redissonClient() throws IOException {
        Config config = Config.fromYAML(getClass().getClassLoader().getResource(REDISSON_CONFIG_FILE));
        return Redisson.create(config);
    }
}
