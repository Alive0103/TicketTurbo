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

import cn.hutool.core.io.FileUtil;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 压力测试购票接口单元测试
 * PT 全拼 Performance Testing，表示压力测试缩写
 */
@Slf4j
public class PTTicketCSVGenerationTests {

    private static final String FILE_PATH = Paths.get("").toAbsolutePath().getParent().getParent() + "/resources/csv";

    @Test
    public void simpleTest() throws IOException {
        System.out.println(FILE_PATH);
    }

    @BeforeAll
    public static void init() {
        if (!FileUtil.exist(FILE_PATH)) {
            FileUtil.mkdir(FILE_PATH);
        }
    }

    @Test
    public void csvDataGeneration() {
        // 示例数据
        List<String[]> data = List.of(
                new String[]{"trainId", "seatType", "departure", "arrival"},
                new String[]{"1", "2", "北京南", "杭州东"},
                new String[]{"1", "2", "北京南", "杭州东"},
                new String[]{"1", "3", "北京南", "杭州东"},
                new String[]{"1", "3", "北京南", "杭州东"}
        );
        try (CSVWriter writer = new CSVWriter(new FileWriter(FILE_PATH + "/简单购票接口数据.csv"))) {
            writer.writeAll(data);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 辅助方法：将指定数量的车次信息添加到 data 列表中
     *
     * @param data      要写入的总数据列表
     * @param trainId   车次 ID（可自定义）
     * @param seatType  座位类型 (0：商务座，1：一等座，2：二等座)
     * @param departure 出发地
     * @param arrival   目的地
     * @param count     需要添加的数据行数
     */
    private void addRows(List<String[]> data, String trainId, String seatType,
                         String departure, String arrival, int count) {
        for (int i = 0; i < count; i++) {
            data.add(new String[]{trainId, seatType, departure, arrival});
        }
    }

    @Test
    public void generate2000RequestsCsv() {
        // 1. 创建存放数据的列表，先加入表头
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"trainId", "seatType", "departure", "arrival"});

        // 2. 遍历列车 ID，从 5 到 24
        for (int trainId = 5; trainId <= 24; trainId++) {
            // 商务座(0) -> 共 5 条
            //   1) 北京南 -> 济南西：1
            addRows(data, String.valueOf(trainId), "0", "北京南", "济南西", 1);
            //   2) 济南西 -> 杭州东：2
            addRows(data, String.valueOf(trainId), "0", "济南西", "杭州东", 2);
            //   3) 杭州东 -> 宁波：2
            addRows(data, String.valueOf(trainId), "0", "杭州东", "宁波", 2);

            // 一等座(1) -> 共 30 条
            //   1) 北京南 -> 南京南：15
            addRows(data, String.valueOf(trainId), "1", "北京南", "南京南", 15);
            //   2) 南京南 -> 宁波：15
            addRows(data, String.valueOf(trainId), "1", "南京南", "宁波", 15);

            // 二等座(2) -> 共 65 条
            //   1) 北京南 -> 南京南：30
            addRows(data, String.valueOf(trainId), "2", "北京南", "南京南", 30);
            //   2) 南京南 -> 宁波：35
            addRows(data, String.valueOf(trainId), "2", "南京南", "宁波", 35);
        }

        // 3. 打乱除表头以外的所有数据
        Collections.shuffle(data.subList(1, data.size()));

        // 4. 将结果写入 CSV 文件
        String outputFile = FILE_PATH + "/2000个请求_购票接口压测数据.csv";
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeAll(data);
            log.info("CSV 文件生成成功，共 {} 行，路径: {}", data.size(), outputFile);
        } catch (IOException e) {
            log.error("生成 CSV 文件失败: {}", e.getMessage(), e);
        }
    }
}
