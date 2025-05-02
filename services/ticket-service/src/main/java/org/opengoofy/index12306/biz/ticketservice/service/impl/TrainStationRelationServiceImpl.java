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

package org.opengoofy.index12306.biz.ticketservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.opengoofy.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import org.opengoofy.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import org.opengoofy.index12306.biz.ticketservice.service.TrainStationRelationService;
import org.opengoofy.index12306.framework.starter.cache.DistributedCache;
import org.springframework.stereotype.Service;

import static org.opengoofy.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_RELATION_KEY;

/**
 * 列车站点关系接口实现层
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 */
@Service
@RequiredArgsConstructor
public class TrainStationRelationServiceImpl implements TrainStationRelationService {

    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    @Override
    public TrainStationRelationDO findRelation(String trainId, String departure, String arrival) {
        return distributedCache.safeGet(String.format(TRAIN_STATION_RELATION_KEY, trainId, departure, arrival),
                TrainStationRelationDO.class,
                () -> {
                    LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                            .eq(TrainStationRelationDO::getTrainId, trainId)
                            .eq(TrainStationRelationDO::getDeparture, departure)
                            .eq(TrainStationRelationDO::getArrival, arrival);
                    TrainStationRelationDO trainStationRelationDO = trainStationRelationMapper.selectOne(queryWrapper);
                    return trainStationRelationDO;
                },
                1800000L);
    }
}
