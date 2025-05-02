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

package org.opengoofy.index12306.biz.orderservice.service.impl;

import cn.crane4j.annotation.AutoOperate;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.opengoofy.index12306.biz.orderservice.common.enums.OrderCanalErrorCodeEnum;
import org.opengoofy.index12306.biz.orderservice.common.enums.OrderItemStatusEnum;
import org.opengoofy.index12306.biz.orderservice.common.enums.OrderStatusEnum;
import org.opengoofy.index12306.biz.orderservice.dao.entity.OrderDO;
import org.opengoofy.index12306.biz.orderservice.dao.entity.OrderItemDO;
import org.opengoofy.index12306.biz.orderservice.dao.entity.OrderItemPassengerDO;
import org.opengoofy.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import org.opengoofy.index12306.biz.orderservice.dao.mapper.OrderMapper;
import org.opengoofy.index12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import org.opengoofy.index12306.biz.orderservice.dto.req.CancelTicketOrderReqDTO;
import org.opengoofy.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import org.opengoofy.index12306.biz.orderservice.dto.req.TicketOrderItemCreateReqDTO;
import org.opengoofy.index12306.biz.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import org.opengoofy.index12306.biz.orderservice.dto.req.TicketOrderSelfPageQueryReqDTO;
import org.opengoofy.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.opengoofy.index12306.biz.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import org.opengoofy.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.opengoofy.index12306.biz.orderservice.mq.event.DelayCloseOrderEvent;
import org.opengoofy.index12306.biz.orderservice.mq.event.PayResultCallbackOrderEvent;
import org.opengoofy.index12306.biz.orderservice.mq.produce.DelayCloseOrderSendProduce;
import org.opengoofy.index12306.biz.orderservice.remote.UserRemoteService;
import org.opengoofy.index12306.biz.orderservice.remote.dto.UserQueryActualRespDTO;
import org.opengoofy.index12306.biz.orderservice.service.OrderItemService;
import org.opengoofy.index12306.biz.orderservice.service.OrderPassengerRelationService;
import org.opengoofy.index12306.biz.orderservice.service.OrderService;
import org.opengoofy.index12306.biz.orderservice.service.orderid.OrderIdGeneratorManager;
import org.opengoofy.index12306.framework.starter.common.toolkit.BeanUtil;
import org.opengoofy.index12306.framework.starter.convention.exception.ClientException;
import org.opengoofy.index12306.framework.starter.convention.exception.ServiceException;
import org.opengoofy.index12306.framework.starter.convention.page.PageResponse;
import org.opengoofy.index12306.framework.starter.convention.result.Result;
import org.opengoofy.index12306.framework.starter.database.toolkit.PageUtil;
import org.opengoofy.index12306.frameworks.starter.user.core.UserContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 订单服务接口层实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderItemService orderItemService;
    private final OrderPassengerRelationService orderPassengerRelationService;
    private final RedissonClient redissonClient;
    private final DelayCloseOrderSendProduce delayCloseOrderSendProduce;
    private final UserRemoteService userRemoteService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 根据订单号查询车票订单详细信息（含乘车人明细）
     *
     * @param orderSn 订单流水号（唯一标识）
     * @return 包含主订单信息和乘车人明细的完整订单数据
     */
    @Override
    public TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn) {
        // 构建主订单查询条件（根据订单号精准匹配）
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);// 订单号作为唯一查询条件
        // 查询主订单数据（单个订单）
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        // 转换主订单对象为响应DTO
        TicketOrderDetailRespDTO result = BeanUtil.convert(orderDO, TicketOrderDetailRespDTO.class);
        // 构建子订单项查询条件（关联同一订单号）
        LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, orderSn);
        // 查询所有关联的乘车人子订单数据
        List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
        // 转换子订单数据并设置到响应对象中
        result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
        return result;
    }

    /**
     * 分页查询车票订单
     *
     * @param requestParam 包含用户ID、分页参数和订单状态等查询条件
     * @return 分页的订单详细信息，包含乘车人明细数据
     */
    @AutoOperate(type = TicketOrderDetailRespDTO.class, on = "data.records")// 自动操作注解，对结果集的data.records字段进行后处理
    @Override
    public PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
        // 构建订单主表查询条件
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, requestParam.getUserId())// 匹配用户ID
                .in(OrderDO::getStatus, buildOrderStatusList(requestParam)) // 根据请求参数构建状态查询列表
                .orderByDesc(OrderDO::getOrderTime); // 按订单时间倒序排序
        // 执行分页查询（使用MyBatis-Plus的分页功能）
        IPage<OrderDO> orderPage = orderMapper.selectPage(PageUtil.convert(requestParam), queryWrapper);
        // 转换分页结果并填充子订单数据
        return PageUtil.convert(orderPage, each -> {
            // 转换主订单对象
            TicketOrderDetailRespDTO result = BeanUtil.convert(each, TicketOrderDetailRespDTO.class);
            // 构建子订单查询条件（根据订单号查询乘车人明细）
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn()); // 匹配订单流水号
            // 查询子订单数据
            List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
            // 设置乘车人详细信息到返回对象
            result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
            return result;
        });
    }

    /**
     * 创建车票订单（核心事务方法）
     *
     * @param requestParam 订单创建请求参数
     * @return 生成的订单流水号
     *
     * 实现要点：
     * 1. 订单号基因生成法：融合用户ID实现分库分表友好性
     * 2. 三级数据存储：主订单表、子订单表、乘车人关系表
     * 3. 事务边界控制：编程式事务确保主从表数据一致性
     * 4. 异步消息解耦：订单关闭延迟消息与主事务分离
     */
    @Trace(operationName = "create-ticket-order")// 全链路追踪标识
    @Override
    public String createTicketOrder(TicketOrderCreateReqDTO requestParam) {
        // 通过基因法将用户 ID 融入到订单号（解决分库分表关联查询问题）
        String orderSn = OrderIdGeneratorManager.generateId(requestParam.getUserId());
        // 主订单数据构建（状态初始化为待支付）
        OrderDO orderDO = OrderDO.builder().orderSn(orderSn)
                .orderTime(requestParam.getOrderTime())
                .departure(requestParam.getDeparture())
                .departureTime(requestParam.getDepartureTime())
                .ridingDate(requestParam.getRidingDate())
                .arrivalTime(requestParam.getArrivalTime())
                .trainNumber(requestParam.getTrainNumber())
                .arrival(requestParam.getArrival())
                .trainId(requestParam.getTrainId())
                .source(requestParam.getSource())
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .username(requestParam.getUsername())
                .userId(String.valueOf(requestParam.getUserId()))
                .build();
        // 子订单及乘车人关系数据组装
        List<TicketOrderItemCreateReqDTO> ticketOrderItems = requestParam.getTicketOrderItems();
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        List<OrderItemPassengerDO> orderPassengerRelationDOList = new ArrayList<>();
        ticketOrderItems.forEach(each -> {
            // 子订单构建（包含座位详情）
            OrderItemDO orderItemDO = OrderItemDO.builder()
                    .trainId(requestParam.getTrainId())
                    .seatNumber(each.getSeatNumber())
                    .carriageNumber(each.getCarriageNumber())
                    .realName(each.getRealName())
                    .orderSn(orderSn)
                    .phone(each.getPhone())
                    .seatType(each.getSeatType())
                    .username(requestParam.getUsername()).amount(each.getAmount()).carriageNumber(each.getCarriageNumber())
                    .idCard(each.getIdCard())
                    .ticketType(each.getTicketType())
                    .idType(each.getIdType())
                    .userId(String.valueOf(requestParam.getUserId()))
                    .status(0)
                    .build();
            // 乘车人关系构建
            orderItemDOList.add(orderItemDO);
            OrderItemPassengerDO orderPassengerRelationDO = OrderItemPassengerDO.builder()
                    .idType(each.getIdType())
                    .idCard(each.getIdCard())
                    .orderSn(orderSn)
                    .build();
            orderPassengerRelationDOList.add(orderPassengerRelationDO);
        });
        // 通过编程式事务开启订单表新增操作 编程式事务控制（确保主从表原子性写入）
        transactionTemplate.executeWithoutResult(status -> {
            try {
                orderMapper.insert(orderDO);// 主订单写入
                orderItemService.saveBatch(orderItemDOList); // 批量插入子订单
                orderPassengerRelationService.saveBatch(orderPassengerRelationDOList);  // 批量插入乘车关系
            } catch (Exception ex) {
                status.setRollbackOnly(); // 异常时强制回滚
                throw ex;// 封装业务异常
            }
        });
        // 异步发送延迟关闭订单消息（与事务解耦）
        // 为什么延时关闭订单的消息不放入事务中？
        // 核心原因是为了最大程度减少数据库事务的执行时间。如果事务长时间占用数据库连接资源，会导致数据库整体性能下降，进而影响系统的吞吐量
        // 额外再说一点：除去非必要场景，事务里最好只有增删改的数据库事务操作，缓存、消息队列以及文件等都不要出现在事务里
        SendResult sendResult = null;
        try {
            // 发送 RocketMQ 延时消息，指定时间后取消订单
            DelayCloseOrderEvent delayCloseOrderEvent = DelayCloseOrderEvent.builder() // 事件对象构建
                    .trainId(String.valueOf(requestParam.getTrainId()))
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderSn(orderSn)
                    .trainPurchaseTicketResults(requestParam.getTicketOrderItems())
                    .build();
            // 创建订单并支付后延时关闭订单消息怎么办？https://nageoffer.com/12306/question
            sendResult = delayCloseOrderSendProduce.sendMessage(delayCloseOrderEvent);
            // 消息状态校验（SEND_OK为RocketMQ成功状态）
            if (!Objects.equals(sendResult.getSendStatus(), SendStatus.SEND_OK)) {
                throw new ServiceException("投递延迟关闭订单消息队列失败");// 触发监控告警 TODO
            }
        } catch (Throwable ex) {
            // 不抛异常确保主流程继续，但需记录足够上下文用于事后补偿
            log.error("延迟关闭订单消息队列发送错误，请求参数：{}，消息参数：{}", JSON.toJSONString(requestParam), JSON.toJSONString(sendResult), ex); // 审计日志记录
            // 为什么消息发送失败不需要抛异常终止业务流程？
            // 因为延迟关闭订单属于非核心的旁路逻辑，其失败不应影响用户的主要业务流程（如购买流程）
            // 为避免因旁路逻辑失败而影响核心业务，我们可以在这里加入报警机制
            // 此外，消息队列通常具备高可用性，或者可以在此实现一套消息队列的降级方案来保障流程的顺畅
        }
        return orderSn;
    }

    /**
     * 关闭待支付订单（状态校验入口）
     *
     * @param requestParam 订单关闭请求
     * @return 是否关闭成功
     *
     * 设计说明：
     * 1. 前置状态校验：仅允许关闭待支付订单
     * 2. 业务逻辑复用：委托cancelTickOrder执行实际关闭
     * 3. 事务传播：基于注解的事务管理
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean closeTickOrder(CancelTicketOrderReqDTO requestParam) {
        // 状态预校验（避免无效的分布式锁获取）
        String orderSn = requestParam.getOrderSn();
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn)
                .select(OrderDO::getStatus);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        if (Objects.isNull(orderDO) || orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            return false;// 状态不匹配直接返回
        }
        // 原则上订单关闭和订单取消这两个方法可以复用，为了区分未来考虑到的场景，这里对方法进行拆分但复用逻辑
        return cancelTickOrder(requestParam); // 委托核心取消逻辑
    }

    /**
     * 订单取消核心逻辑（分布式锁保护）
     *
     * 关键防御点：
     * 1. 分布式锁粒度控制：基于订单号的细粒度锁
     * 2. 双重状态校验：防止并发场景下的状态覆盖
     * 3. 更新操作原子性：主订单与子订单状态同步更新
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean cancelTickOrder(CancelTicketOrderReqDTO requestParam) {
        String orderSn = requestParam.getOrderSn();
        // 前置校验（订单存在性及状态）
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        } else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        // 分布式锁获取（设置锁超时防止死锁）
        RLock lock = redissonClient.getLock(StrBuilder.create("order:canal:order_sn_").append(orderSn).toString());
        if (!lock.tryLock()) {
            throw new ClientException(OrderCanalErrorCodeEnum.ORDER_CANAL_REPETITION_ERROR);
        }
        try {
            // 锁内二次校验（防止等待期间状态变更）
            OrderDO updateOrderDO = new OrderDO();
            // 主从表状态更新
            updateOrderDO.setStatus(OrderStatusEnum.CLOSED.getStatus());
            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, orderSn);
            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
            OrderItemDO updateOrderItemDO = new OrderItemDO();
            updateOrderItemDO.setStatus(OrderItemStatusEnum.CLOSED.getStatus());
            LambdaUpdateWrapper<OrderItemDO> updateItemWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, orderSn);
            int updateItemResult = orderItemMapper.update(updateOrderItemDO, updateItemWrapper);
            if (updateItemResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
        } finally {
            lock.unlock();
        }
        return true;
    }

    /**
     * 订单状态反转操作（用于支付超时等场景的状态回滚）
     *
     * @param requestParam 包含订单号和目标状态的反转请求参数
     *
     * 实现要点：
     * 1. 前置校验：确保订单存在且当前处于待支付状态
     * 2. 分布式锁：防止并发状态修改导致数据不一致
     * 3. 双表更新：同步更新主订单和子订单状态
     */
    @Override
    public void statusReversal(OrderStatusReversalDTO requestParam) {
        // 根据订单号查询主订单
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        // 订单存在性校验
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        }
        // 状态前置校验（必须为待支付状态）
        else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        // 获取订单粒度的分布式锁（防止并发操作）
        RLock lock = redissonClient.getLock(StrBuilder.create("order:status-reversal:order_sn_").append(requestParam.getOrderSn()).toString());
        if (!lock.tryLock()) {
            log.warn("订单重复修改状态，状态反转请求参数：{}", JSON.toJSONString(requestParam));
        }
        try {
            // 更新主订单状态
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(requestParam.getOrderStatus());
            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
            // 同步更新子订单状态
            OrderItemDO orderItemDO = new OrderItemDO();
            orderItemDO.setStatus(requestParam.getOrderItemStatus());
            LambdaUpdateWrapper<OrderItemDO> orderItemUpdateWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn());
            int orderItemUpdateResult = orderItemMapper.update(orderItemDO, orderItemUpdateWrapper);
            if (orderItemUpdateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 支付回调订单状态更新
     *
     * @param requestParam 支付结果回调事件参数
     *
     * 注意：
     * 1. 核心更新支付时间和支付渠道
     * 2. 未做订单存在性校验（假设回调来源可靠）
     */
    @Override
    public void payCallbackOrder(PayResultCallbackOrderEvent requestParam) {
        // 构建支付信息更新对象
        OrderDO updateOrderDO = new OrderDO();
        updateOrderDO.setPayTime(requestParam.getGmtPayment());// 支付时间
        updateOrderDO.setPayType(requestParam.getChannel());// 支付渠道
        // 根据订单号执行更新
        LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
        // 更新结果校验
        if (updateResult <= 0) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        }
    }

    /**
     * 查询用户本人车票订单（基于乘车人维度）
     *
     * @param requestParam 分页查询参数
     * @return 分页的乘车人关联订单数据
     *
     * 实现逻辑：
     * 1. 获取当前登录用户实名信息
     * 2. 根据身份证查询乘车人关联订单
     * 3. 关联查询主订单和子订单详情
     */
    @Override
    public PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam) {
        // 远程调用获取当前用户实名信息
        Result<UserQueryActualRespDTO> userActualResp = userRemoteService.queryActualUserByUsername(UserContext.getUsername());
        // 构建乘车人关联订单查询条件（按身份证查询）
        LambdaQueryWrapper<OrderItemPassengerDO> queryWrapper = Wrappers.lambdaQuery(OrderItemPassengerDO.class)
                .eq(OrderItemPassengerDO::getIdCard, userActualResp.getData().getIdCard())
                .orderByDesc(OrderItemPassengerDO::getCreateTime);
        // 执行分页查询乘车人关联记录
        IPage<OrderItemPassengerDO> orderItemPassengerPage = orderPassengerRelationService.page(PageUtil.convert(requestParam), queryWrapper);
        // 转换分页结果并填充订单详情
        return PageUtil.convert(orderItemPassengerPage, each -> {
            // 查询主订单信息
            LambdaQueryWrapper<OrderDO> orderQueryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                    .eq(OrderDO::getOrderSn, each.getOrderSn());
            OrderDO orderDO = orderMapper.selectOne(orderQueryWrapper);
            // 查询对应子订单项
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn())
                    .eq(OrderItemDO::getIdCard, each.getIdCard());
            OrderItemDO orderItemDO = orderItemMapper.selectOne(orderItemQueryWrapper);
            // 合并主订单和子订单数据
            TicketOrderDetailSelfRespDTO actualResult = BeanUtil.convert(orderDO, TicketOrderDetailSelfRespDTO.class);
            BeanUtil.convertIgnoreNullAndBlank(orderItemDO, actualResult);
            return actualResult;
        });
    }

    /**
     * 构建订单状态查询列表（根据前端传入的状态类型）
     *
     * @param requestParam 订单分页查询参数
     * @return 对应的状态码列表
     *
     * 状态类型映射关系：
     * 0 -> 待支付
     * 1 -> 已支付/部分退款/全额退款
     * 2 -> 已完成
     */
    private List<Integer> buildOrderStatusList(TicketOrderPageQueryReqDTO requestParam) {
        List<Integer> result = new ArrayList<>();
        switch (requestParam.getStatusType()) {
            case 0 -> result = ListUtil.of(// 待处理订单
                    OrderStatusEnum.PENDING_PAYMENT.getStatus()
            );
            case 1 -> result = ListUtil.of(// 支付相关状态
                    OrderStatusEnum.ALREADY_PAID.getStatus(),
                    OrderStatusEnum.PARTIAL_REFUND.getStatus(),
                    OrderStatusEnum.FULL_REFUND.getStatus()
            );
            case 2 -> result = ListUtil.of(// 已完成订单
                    OrderStatusEnum.COMPLETED.getStatus()
            );
        }
        return result;
    }
}
