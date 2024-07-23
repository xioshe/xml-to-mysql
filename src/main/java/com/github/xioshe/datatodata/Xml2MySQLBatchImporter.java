package com.github.xioshe.datatodata;


import com.github.xioshe.datatodata.model.ObjectMapper;
import com.github.xioshe.datatodata.model.Product;
import com.github.xioshe.datatodata.utils.Sqls;
import com.github.xioshe.datatodata.utils.XmlParser;
import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

@Component
public class Xml2MySQLBatchImporter {

    private static final Logger log = LoggerFactory.getLogger(Xml2MySQLBatchImporter.class);

    public void importWithJdbcBatch(String path) {
        var stopWatch = new StopWatch("import-from-xml");

        stopWatch.start("parse-xml");
        Document document = XmlParser.parse(path);
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());

        stopWatch.start("build-products");
        var products = new ArrayList<Product>();
        for (var it = document.getRootElement().elementIterator(); it.hasNext(); ) {
            var element = it.next();
            if (!StringUtils.hasText(element.elementTextTrim("id"))) {
                continue;
            }
            var product = ObjectMapper.buildProduct(element);
            products.add(product);
        }
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());

        stopWatch.start("insert-into-mysql");

        try (var connection = DriverManager.getConnection("jdbc:mysql://192.168.31.252/pmc?useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true", "root", "123456");) {
            connection.setAutoCommit(false);

            try (var statement = connection.prepareStatement(Sqls.SAVE_SQL)) {
                int i = 0;
                for (Product product : products) {
                    ObjectMapper.inflationStatement(statement, product);
                    statement.addBatch();
                    i++;
                    if (i == 1000) {
                        statement.executeBatch();
                        statement.clearBatch();
                        i = 0;
                    }
                }
                if (i > 0) {
                    statement.executeBatch();
                    statement.clearBatch();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            stopWatch.stop();
        }
        log.info(stopWatch.prettyPrint());
    }

    public static void main(String[] args) {
        new Xml2MySQLBatchImporter().importWithJdbcBatch("classpath:items.xml");
//        new Xml2MySQLBatchImporter().importe("classpath:one.xml");
    }
}
