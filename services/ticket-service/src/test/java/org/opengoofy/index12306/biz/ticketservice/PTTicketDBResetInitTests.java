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

package org.opengoofy.index12306.biz.ticketservice;

import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.opengoofy.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.opengoofy.index12306.biz.ticketservice.service.TrainStationRelationService;
import org.opengoofy.index12306.biz.ticketservice.service.TrainStationService;
import org.opengoofy.index12306.biz.ticketservice.service.cache.SeatMarginCacheLoader;
import org.opengoofy.index12306.biz.ticketservice.toolkit.StationCalculateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * 压测测试前重置列车余票以及进行购票初始化缓存等
 * PT 全拼 Performance Testing，表示压力测试缩写
 */
@Slf4j
@SpringBootTest
public class PTTicketDBResetInitTests {

    @Autowired
    private TrainStationService trainStationService;
    @Autowired
    private SeatMarginCacheLoader seatMarginCacheLoader;
    @Autowired
    private TrainStationRelationService trainStationRelationService;

    @Test
    public void testReset() {
        CompletableFuture<?>[] resetFutures = IntStream.range(5, 25)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    HttpUtil.createPost("http://127.0.0.1:9000/api/ticket-service/temp/seat/reset?trainId=" + i).execute();
                }))
                .toArray(CompletableFuture[]::new);

        // 将所有 resetFutures 的完成状态合并成一个 CompletableFuture
        CompletableFuture<Void> resetAllFutures = CompletableFuture.allOf(resetFutures);

        // 在所有 resetFutures 完成后执行 getResultFuture
        CompletableFuture<Void> getResultFuture = resetAllFutures.thenRunAsync(() -> {
            String result = HttpUtil.get("http://localhost:9000/api/ticket-service/ticket/query?fromStation=BJP&toStation=HZH&departureDate=" + DateUtil.today());
            log.info("\n{}", JSONUtil.formatJsonStr(result));
        });

        // 等待所有 futures 完成
        getResultFuture.join();
    }

    @Test
    public void cacheInit() {
        // 初始化缓存方法 listTrainStationRoute & listTakeoutTrainStationRoute
        List<String> stations = Arrays.asList("北京南", "济南西", "南京南", "杭州东", "宁波");
        String startStation = "北京南";
        String endStation = "宁波";
        List<RouteDTO> routeDTOList = StationCalculateUtil.takeoutStation(stations, startStation, endStation);

        // 同步逐步执行缓存初始化
        for (RouteDTO each : routeDTOList) {
            for (int i = 5; i < 25; i++) {
                // 执行服务调用
                trainStationService.listTrainStationRoute(String.valueOf(i), each.getStartStation(), each.getEndStation());
                trainStationService.listTakeoutTrainStationRoute(String.valueOf(i), each.getStartStation(), each.getEndStation());
                trainStationRelationService.findRelation(String.valueOf(i), each.getStartStation(), each.getEndStation());
                for (int j = 0; j < 3; j++) {
                    seatMarginCacheLoader.load(String.valueOf(i), String.valueOf(j), each.getStartStation(), each.getEndStation());
                }
            }
        }

        // 初始化用户登录信息参数
        String userLoginRequestBody = JSONUtil.createObj()
                .put("usernameOrMailOrPhone", "admin")
                .put("password", "admin123456")
                .toString();
        HttpResponse execute = HttpUtil.createPost("http://127.0.0.1:9000/api/user-service/v1/login")
                .body(userLoginRequestBody)
                .execute();
        String actualData = JSON.parseObject(execute.body()).getString("data");
        String accessToken = JSON.parseObject(actualData).getString("accessToken");

        // 执行购票请求任务，用于初始化令牌桶
        for (int i = 5; i < 25; i++) {
            HttpUtil.createPost("http://127.0.0.1:9000/api/ticket-service/ticket/purchase/v2")
                    .body(String.format("""
                            {
                                "trainId": %s,
                                "passengers": [
                                    {
                                        "passengerId": "1683029289099362304",
                                        "seatType": 0
                                    }
                                ],
                                "chooseSeats": [],
                                "departure": "北京南",
                                "arrival": "宁波"
                            }
                            """, i))
                    .header("Authorization", accessToken)
                    .execute();
        }

        log.info("所有初始化任务和购票请求任务已完成");
    }
}
