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

package org.opengoofy.index12306.biz.aggregationservice;

import cn.hutool.core.lang.Assert;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.opengoofy.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.opengoofy.index12306.biz.ticketservice.service.TrainStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 列车站点服务单元测试
 */
@Slf4j
@SpringBootTest
public class TrainStationServiceTests {

    @Autowired
    private TrainStationService trainStationService;

    @Test
    public void testListTrainStationRoute() {
        List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute("1", "北京南", "杭州东");
        Assert.notNull(routeDTOList);
        log.info("\n{}", JSONUtil.formatJsonStr(JSONUtil.toJsonStr(routeDTOList)));
    }

    @Test
    public void testListTakeoutTrainStationRoute() {
        List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute("1", "北京南", "杭州东");
        Assert.notNull(routeDTOList);
        log.info("\n{}", JSONUtil.formatJsonStr(JSONUtil.toJsonStr(routeDTOList)));
    }
}
