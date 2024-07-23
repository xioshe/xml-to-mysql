package com.github.xioshe.datatodata;


import com.github.xioshe.datatodata.model.ObjectMapper;
import com.github.xioshe.datatodata.model.Product;
import com.github.xioshe.datatodata.utils.MysqlPropertiesConstant;
import com.github.xioshe.datatodata.utils.Sqls;
import com.github.xioshe.datatodata.utils.XmlParser;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RewindableEventHandler;
import com.lmax.disruptor.RewindableException;
import com.lmax.disruptor.SimpleBatchRewindStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

@Component
public class DisruptorConcurrentBatchImporter {

    private static final Logger log = LoggerFactory.getLogger(DisruptorConcurrentBatchImporter.class);

    public static void main(String[] args) throws SQLException, InterruptedException {
        new DisruptorConcurrentBatchImporter().importWithDisruptor("classpath:items.xml");
//        new DisruptorBatchImporter().importWithDisruptor("classpath:one.xml");
    }

    public void importWithDisruptor(String path) throws InterruptedException {
        var disruptor = new Disruptor<>(
                ProductEvent::new,
                1024,
                DaemonThreadFactory.INSTANCE,
                ProducerType.SINGLE,
                new BusySpinWaitStrategy());

        CountDownLatch shutdownLatch = new CountDownLatch(4);
        var consumers = new RewindableEventHandler[4];
        for (int i = 0; i < consumers.length; i++) {
            consumers[i] = new SaveDbHandler(i, 4, shutdownLatch);
        }
        disruptor.handleEventsWith(new SimpleBatchRewindStrategy(), consumers)
                .then(new ClearingEventHandler());
        var ringBuffer = disruptor.start();

        var stopWatch = new StopWatch("import-from-xml");

        stopWatch.start("parse-xml");
        Document document = XmlParser.parse(path);
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());

        stopWatch.start("build-products-then-write-to-mysql");
        for (var it = document.getRootElement().elementIterator(); it.hasNext(); ) {
            var element = it.next();
            if (!StringUtils.hasText(element.elementTextTrim("id"))) {
                continue;
            }
            var product = ObjectMapper.buildProduct(element);
            ringBuffer.publishEvent((event, sequence, buffer) -> event.setProduct(product));
        }

        disruptor.shutdown();
        shutdownLatch.await();

        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
    }

    @Setter
    @Getter
    static class ProductEvent {
        private Product product;

        public void clear() {
            product = null;
        }
    }

    @Slf4j
    static class SaveDbHandler implements RewindableEventHandler<ProductEvent> {

        private final long ordinal;
        private final long numberOfConsumers;
        private final CountDownLatch latch;

        private Connection connection = null;
        private PreparedStatement statement = null;

        public SaveDbHandler(final long ordinal, final long numberOfConsumers, final CountDownLatch latch) {
            this.ordinal = ordinal;
            this.numberOfConsumers = numberOfConsumers;
            this.latch = latch;
        }

        @Override
        public void onStart() {
            log.info("{}: onStart", ordinal);
        }

        @Override
        public void onBatchStart(long batchSize, long queueDepth) {
            log.debug("{}: onBatchStart, batchSize: {}, queueDepth: {}", ordinal, batchSize, queueDepth);
        }

        @Override
        public void onEvent(ProductEvent event, long sequence, boolean endOfBatch) throws Exception, RewindableException {
            if (sequence % numberOfConsumers != ordinal) {
                return;
            }
            log.debug("{}: onEvent, sequence: {}, endOfBatch:{}", ordinal, sequence, endOfBatch);
            if (statement == null) {
                initConnection();
            }
            try {
                ObjectMapper.inflationStatement(statement, event.getProduct());
                statement.addBatch();
                if (endOfBatch) {
                    statement.executeBatch();
                    statement.clearBatch();
                    connection.commit();
                }
            } catch (SQLException e) {
                log.error("[{}] handler error, sequence: {}, endOfBatch:{}", ordinal, sequence, endOfBatch, e);
                try {
                    if (connection != null)
                        connection.rollback();
                } catch (SQLException se2) {
                    log.error("rollback error", se2);
                }
                // 抛出 RewindableException， Disruptor 会重新消费当前 Batch
                throw new RewindableException(e);
            }
        }

        @Override
        public void onTimeout(long sequence) {
            log.info("{}: onTimeout", ordinal);
            log.info("sequence: {}", sequence);
        }

        @Override
        public void onShutdown() {
            log.info("{}: onShutdown", ordinal);
            if (connection != null) {
                try {
                    var effects = statement.executeBatch();
                    log.info("executeBatch on shutdown, effects: {}", effects.length);
                    statement.clearBatch();
                    connection.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            latch.countDown();
        }

        private void initConnection() throws SQLException {
            if (connection == null) {
                log.info("init connection");
                connection = DriverManager.getConnection(
                        MysqlPropertiesConstant.JDBC_URL,
                        MysqlPropertiesConstant.MYSQL_USERNAME,
                        MysqlPropertiesConstant.MYSQL_PASSWORD);
                connection.setAutoCommit(false);
            }
            statement = connection.prepareStatement(Sqls.SAVE_SQL);
        }
    }

    static class ClearingEventHandler implements EventHandler<ProductEvent> {
        @Override
        public void onEvent(ProductEvent event, long sequence, boolean endOfBatch) {
            event.clear(); // help gc
        }
    }
}
