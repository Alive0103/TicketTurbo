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

package org.opengoofy.index12306.biz.aggregationservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.opengoofy.index12306.biz.ticketservice.dao.entity.StationDO;
import org.opengoofy.index12306.biz.ticketservice.dao.mapper.StationMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 读写分离测试：用于验证读写分离的实现效果
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 */
@RestController
@RequiredArgsConstructor
public class ReadWriteSplittingDemoController {

    private final StationMapper stationMapper;

    @GetMapping("/database/readwrite-splitting/insert-select/station")
    public StationDO readWriteSplittingInsert() {
        // 新增测试站点
        stationMapper.insertOrUpdate(buildInsertStationDO());

        // 查询新增测试站点
        LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                .eq(StationDO::getCode, "TEST");
        StationDO selectStationDO = stationMapper.selectOne(queryWrapper);
        return selectStationDO;
    }

    @GetMapping("/database/readwrite-splitting/select/station")
    public StationDO readWriteSplittingSelect() {
        return stationMapper.selectById(1);
    }

    @GetMapping("/database/readwrite-splitting/hint-select/station")
    public StationDO readWriteSplittingHintSelect() {
        // 强制将数据库操作路由设置为仅操作主数据库
        HintManager.getInstance().setWriteRouteOnly();
        try {
            return stationMapper.selectById(1);
        } finally {
            // 底层使用 ThreadLocal 保存路由配置，通过 clear 清除主库路由设置
            HintManager.clear();
        }
    }

    private StationDO buildInsertStationDO() {
        StationDO stationDO = new StationDO();
        stationDO.setId(12L);
        stationDO.setCode("TEST");
        stationDO.setName("测试站点");
        stationDO.setSpell("ceshizhandian");
        stationDO.setRegion("TEST");
        stationDO.setRegionName("测试地区");
        stationDO.setCreateTime(new Date());
        stationDO.setUpdateTime(new Date());
        stationDO.setDelFlag(0);
        return stationDO;
    }
}
