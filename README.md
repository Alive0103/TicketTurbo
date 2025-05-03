# TicketTurbo 智能购票系统

TicketTurbo 是一个基于 SpringBoot3 + SpringCloud Alibaba 构建的现代化智能购票系统。本项目采用微服务架构设计，提供了完整的铁路购票解决方案，包括用户管理、车票查询、订单处理、支付服务等核心功能。

## 项目特性

- 🚀 基于 Java 17 + SpringBoot 3.0 最新技术栈
- 🎯 采用微服务架构，服务解耦，易于扩展
- 🔐 完善的用户认证和授权机制
- 💡 智能购票和座位分配算法
- 🌟 高性能的缓存设计和订单处理
- 📊 分布式系统解决方案

## 技术架构

### 后端技术栈

- **基础框架**：SpringBoot 3.x、SpringCloud Alibaba
- **数据存储**：MySQL、Redis
- **消息队列**：RocketMQ
- **网关服务**：Spring Cloud Gateway
- **服务注册与发现**：Nacos
- **分布式事务**：Seata
- **服务监控**：Spring Boot Admin
- **接口文档**：Swagger
- **项目构建**：Maven

### 前端技术栈

- Vue.js
- Ant Design Vue
- Axios
- Webpack

## 核心功能模块

### 1. 用户服务 (user-service)
- 用户注册登录
- 乘客信息管理
- 用户认证授权

### 2. 购票服务 (ticket-service)
- 车票查询
- 座位智能分配
- 余票管理
- 车次信息管理

### 3. 订单服务 (order-service)
- 订单创建与管理
- 订单状态流转
- 订单超时处理

### 4. 支付服务 (pay-service)
- 支付渠道对接
- 支付状态管理
- 退款处理

### 5. 网关服务 (gateway-service)
- 统一接入层
- 请求转发
- 安全控制

### 6. 聚合服务 (aggregation-service)
- 数据聚合
- 服务编排
- 性能优化

## 系统架构图

```
                   Client
                     │
                     ↓
              API Gateway
                     │
     ┌───────┬───────┼───────┬───────┐
     ↓       ↓       ↓       ↓       ↓
   User    Ticket  Order    Pay    Other
  Service  Service Service Service Services
     │       │       │       │       │
     └───────┴───────┴───────┴───────┘
                     │
              Shared Services
   (Cache/Message Queue/Database/etc.)
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RocketMQ 4.9+
- Node.js 14+

### 本地开发

1. 克隆项目
```bash
git clone git@github.com:Alive0103/TicketTurbo.git
```

2. 初始化数据库

Docker启动数据库实例：


```bash
docker run --name mysql \
-p 3306:3306 \
-e MYSQL_ROOT_HOST='%' \
-e MYSQL_ROOT_PASSWORD=root \
-d mysql:5.7.36
```
```bash
CREATE DATABASE /*!32312 IF NOT EXISTS*/ `12306` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;
# 执行数据库脚本
resources/db/12306-springboot.sql
resources/data/12306-springboot.sql
```

3. 启动基础服务
- 启动 Nacos 服务
- 启动 Redis 服务
- 启动 RocketMQ 服务

（已在虚拟机部署）

4. 启动后端服务
启动Gateway和aggregation-service（聚合服务）即可

5. 启动前端项目
```bash
npm install -g yarn

yarn install 

yarn serve
```

## 性能优化

- 使用 Redis 缓存热点数据
- RocketMQ 实现异步消息处理
- 数据库读写分离
- 分布式锁控制并发
- 智能购票算法优化

## 部署运维

- 支持 Docker 容器化部署
- 日志统一收集管理

## 开发计划

- [ ] 集成更多支付渠道
- [ ] 优化座位分配算法
- [ ] 增加可观测监控指标
- [ ] 提升系统并发性能


## 贡献指南

欢迎提交 Issue 或 Pull Request 来帮助改进项目。

Author: 
- [Alive0103 王越洋22009200894](https://github.com/Alive0103) 
- [Aran-hazel 付晗22009200800](https://github.com/Aran-hazel)

## 许可证

本项目采用 MIT 许可证，详情请参见 LICENSE 文件。
