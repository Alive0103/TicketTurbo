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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.Test;
import org.opengoofy.index12306.biz.ticketservice.dao.entity.CarriageDO;
import org.opengoofy.index12306.biz.ticketservice.dao.entity.SeatDO;
import org.opengoofy.index12306.biz.ticketservice.dao.entity.TrainDO;
import org.opengoofy.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import org.opengoofy.index12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import org.opengoofy.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import org.opengoofy.index12306.biz.ticketservice.dao.mapper.CarriageMapper;
import org.opengoofy.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import org.opengoofy.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import org.opengoofy.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import org.opengoofy.index12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import org.opengoofy.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 生成多条高铁列车数据库信息单元测试
 * PT 全拼 Performance Testing，表示压力测试缩写
 */
@SpringBootTest
public class PTTicketDBGeneratedTests {

    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private CarriageMapper carriageMapper;
    @Autowired
    private TrainStationMapper trainStationMapper;
    @Autowired
    private TrainStationRelationMapper trainStationRelationMapper;
    @Autowired
    private TrainStationPriceMapper trainStationPriceMapper;
    @Autowired
    private SeatMapper seatMapper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void testGeneratedData() {
        Long trainId = 1L;
        transactionTemplate.executeWithoutResult(status -> {
            try {
                for (int id = 5; id < 25; id++) {
                    Long actualId = Long.valueOf(id);
                    // 获取列车信息 t_train
                    TrainDO trainDO = trainMapper.selectById(trainId);
                    trainDO.setId(Long.valueOf(actualId));
                    trainMapper.insert(trainDO);

                    // 获取车厢表信息 t_carriage
                    List<CarriageDO> carriageDOList = carriageMapper.selectList(Wrappers.lambdaQuery(CarriageDO.class).eq(CarriageDO::getTrainId, trainId));
                    List<CarriageDO> actualCarriageDOList = carriageDOList.stream().map(each -> {
                        each.setId(null);
                        each.setTrainId(actualId);
                        return each;
                    }).toList();
                    carriageMapper.insert(actualCarriageDOList);

                    // 获取列车站点信息 t_train_station
                    List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(Wrappers.lambdaQuery(TrainStationDO.class).eq(TrainStationDO::getTrainId, trainId));
                    List<TrainStationDO> actualTrainStationDOList = trainStationDOList.stream().map(each -> {
                        each.setId(null);
                        each.setTrainId(actualId);
                        return each;
                    }).toList();
                    trainStationMapper.insert(actualTrainStationDOList);

                    // 获取列车站点关联表信息 t_train_station_relation
                    List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper.selectList(Wrappers.lambdaQuery(TrainStationRelationDO.class).eq(TrainStationRelationDO::getTrainId, trainId));
                    List<TrainStationRelationDO> actualTrainStationRelationDOList = trainStationRelationDOList.stream().map(each -> {
                        each.setId(null);
                        each.setTrainId(actualId);
                        return each;
                    }).toList();
                    trainStationRelationMapper.insert(actualTrainStationRelationDOList);

                    // 列车站点价格表 t_train_station_price
                    List<TrainStationPriceDO> trainStationPriceDOList = trainStationPriceMapper.selectList(Wrappers.lambdaQuery(TrainStationPriceDO.class).eq(TrainStationPriceDO::getTrainId, trainId));
                    List<TrainStationPriceDO> actualTrainStationPriceDOList = trainStationPriceDOList.stream().map(each -> {
                        each.setId(null);
                        each.setTrainId(actualId);
                        return each;
                    }).toList();
                    trainStationPriceMapper.insert(actualTrainStationPriceDOList);

                    // 列车座位 t_seat
                    List<SeatDO> seatDOList = seatMapper.selectList(Wrappers.lambdaQuery(SeatDO.class).eq(SeatDO::getTrainId, trainId));
                    List<SeatDO> actualSeatDOList = seatDOList.stream().map(each -> {
                        each.setId(null);
                        each.setTrainId(actualId);
                        return each;
                    }).toList();
                    seatMapper.insert(actualSeatDOList);
                }
            } catch (Exception ex) {
                status.setRollbackOnly();
                throw ex;
            }
        });
    }
}
