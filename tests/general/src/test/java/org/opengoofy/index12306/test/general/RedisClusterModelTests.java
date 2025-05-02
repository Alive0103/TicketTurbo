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

package org.opengoofy.index12306.test.general;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

/**
 * Redis Cluster 模式测试用例
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 */
@Slf4j
@SpringBootTest
public class RedisClusterModelTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedisClusterModelFail() {
        String luaScript = """
                redis.call('incr', KEYS[1])
                redis.call('incr', KEYS[2])
                """;
        try {
            stringRedisTemplate.execute(
                    (RedisCallback<Object>) (connection) -> connection.eval(
                            luaScript.getBytes(),
                            ReturnType.VALUE,
                            2, // 传入的键的数量
                            "myKey".getBytes(), "myKey2".getBytes() // 键名
                    )
            );
        } catch (Exception ex) {
            log.error("Redis cluster model 执行失败", ex);
        }
    }

    @Test
    public void testRedisClusterModelSuccess() {
        String luaScript = """
                redis.call('incr', KEYS[1])
                redis.call('incr', KEYS[2])
                """;
        stringRedisTemplate.execute(
                (RedisCallback<Object>) (connection) -> connection.eval(
                        luaScript.getBytes(),
                        ReturnType.VALUE,
                        2, // 传入的键的数量
                        "myKey{myKey}".getBytes(), "myKey2{myKey}".getBytes() // 键名
                )
        );
        Assert.notNull(stringRedisTemplate.opsForValue().get("myKey{myKey}"), "Key not found");
        Assert.notNull(stringRedisTemplate.opsForValue().get("myKey2{myKey}"), "Key not found");
        log.info("Redis cluster model 执行成功");
    }
}
