# Import data from xml to mysql

A simple project shows how to import data from XML file to MySQL database, and how to improve the performance.

本项目演示了如何从 XML 文件导入数据到 MySQL 数据库，并展示了能大幅提升性能的两个关键优化方案。

- 开启 JDBC 批处理。
- 使用多线程消费。

测试环境为 2019 MacBook Pro，i9 16GB，数据库用的 MySQL 8.0，安装在局域网内的台式 PC 上。PC 性能孱弱，且没有对 MySQL
进行专门的优化，因此 MySQL 的写入性能相当抱歉。

导入 XML 中的 60,000 多条数据

| Plan        | Cost(seconds) | Max Memory(MB) | Max GC pause(ms) |
|-------------|---------------|----------------|------------------|
| 原始性能        | 301.2         | 656.6          | 70               |
| 开启 JDBC 批处理 | 12.7          | 673.4          | 50               |
| 开启多线程消费     | 4.4           | 859.2          | 20               |

## 启动方式

项目引入了 Spring Boot 依赖，但仅仅用了部分工具类，没有通过 Spring Boot 启动。

启动类位于 `Xml2MySQLBatchImporter` 类和 `DisruptorConcurrentBatchImporter` 类的 `main()` 方法中。

启动时建议添加 JVM 启动选项以收集 GC
日志，便于分析内存使用情况：`-Xlog:gc*=info:file=gc_%t.log:time:filecount=10,filesize=10m`。

## 优化步骤

### 开启 JDBC 批处理

1. 修改 `jdbc.url` 添加 `rewriteBatchedStatements=true` 参数。
2. 关闭自动提交事务。
3. 使用 JDBC 的 Batch 相关 API。

  ```java
  statement.addBatch();
  statement.

executeBatch();
  statement.

clearBatch();
  ```

### 开启多线程消费

使用 Disruptor 队列，实现生产者消费者模型。

- 一个生产者
- 4 个消费者，每个消费者以取模的方式消费自己的数据。

消费者实现了 `RewindableEventHandler` 接口，用于在事务回滚后重新消费当前批次数据。

详细说明见 [博客文章](https://blog.prochase.top/2024/07/mysql-batch-write/)。
