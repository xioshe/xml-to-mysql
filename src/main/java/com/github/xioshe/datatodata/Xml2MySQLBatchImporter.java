package com.github.xioshe.datatodata;


import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

@Component
public class Xml2MySQLBatchImporter {

    private static final Logger log = LoggerFactory.getLogger(Xml2MySQLBatchImporter.class);

    public Document parse(String path) {
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            document = reader.read(ResourceUtils.getFile(path));
        } catch (DocumentException e) {
            log.error(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return document;
    }


    public void importe(String path) {
        var stopWatch = new StopWatch("import-from-xml");

        stopWatch.start("parse-xml");
        Document document = parse(path);
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());

        stopWatch.start("build-products");
        var products = new ArrayList<Product>();
        for (var it = document.getRootElement().elementIterator(); it.hasNext(); ) {
            var element = it.next();
            if (!StringUtils.hasText(element.elementTextTrim("id"))) {
                continue;
            }
            var product = buildProduct(element);
            products.add(product);
        }
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());

        stopWatch.start("insert-into-mysql");
        String sql = "INSERT INTO product (id, adword, wname, average_score, mart_price, start_remain_time, promotion, loc, jd_price, good, flash_sale, on_line, total_count, book, end_remain_time, ware_id, can_free_read, imageurl, wmaprice, cid) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (var connection = DriverManager.getConnection("jdbc:mysql://192.168.31.252/pmc?useSSL=false&rewriteBatchedStatements=true", "root", "123456");) {
            connection.setAutoCommit(false);

            try (var statement = connection.prepareStatement(sql)) {
                int i = 0;
                for (Product product : products) {
                    inflationStatement(statement, product);
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

    private Product buildProduct(Element element) {
        var product = new Product();
        var id = element.elementTextTrim("id");
        product.setId(Long.parseLong(id));
        product.setAdword(element.elementTextTrim("adword"));
        product.setWname(element.elementTextTrim("wname"));
        var averageScore = element.elementTextTrim("averageScore");
        if (StringUtils.hasText(averageScore)) {
            product.setAverageScore(Integer.parseInt(averageScore));
        }
        var martPrice = element.elementTextTrim("martPrice");
        if (StringUtils.hasText(martPrice)) {
            product.setMartPrice(new BigDecimal(martPrice));
        }
        var startRemainTime = element.elementTextTrim("startRemainTime");
        if (StringUtils.hasText(startRemainTime)) {
            product.setStartRemainTime(Long.parseLong(startRemainTime));
        }
        var promotion = element.elementTextTrim("promotion");
        if (StringUtils.hasText(promotion)) {
            product.setPromotion(Boolean.parseBoolean(promotion));
        }
        var loc = element.elementTextTrim("loc");
        if (StringUtils.hasText(loc)) {
            product.setLoc(Boolean.parseBoolean(loc));
        }
        var jdPrice = element.elementTextTrim("jdPrice");
        if (StringUtils.hasText(jdPrice)) {
            product.setJdPrice(new BigDecimal(jdPrice));
        }
        var good = element.elementTextTrim("good");
        if (StringUtils.hasText(good)) {
            product.setGood(good);
        }
        var flashSale = element.elementTextTrim("flashSale");
        if (StringUtils.hasText(flashSale)) {
            product.setFlashSale(Integer.parseInt(flashSale));
        }
        var online = element.elementTextTrim("online");
        if (StringUtils.hasText(online)) {
            product.setOnLine(Boolean.parseBoolean(online));
        }
        var totalCount = element.elementTextTrim("totalCount");
        if (StringUtils.hasText(totalCount)) {
            product.setTotalCount(Integer.parseInt(totalCount));
        }
        var book = element.elementTextTrim("book");
        if (StringUtils.hasText(book)) {
            product.setBook(Boolean.parseBoolean(book));
        }
        var endRemainTime = element.elementTextTrim("endRemainTime");
        if (StringUtils.hasText(endRemainTime)) {
            product.setEndRemainTime(Long.parseLong(endRemainTime));
        }
        var wareId = element.elementTextTrim("wareId");
        if (StringUtils.hasText(wareId)) {
            product.setWareId(Long.parseLong(wareId));
        }
        var canFreeRead = element.elementTextTrim("canFreeRead");
        if (StringUtils.hasText(canFreeRead)) {
            product.setCanFreeRead(Boolean.parseBoolean(canFreeRead));
        }
        var imageurl = element.elementTextTrim("imageurl");
        if (StringUtils.hasText(imageurl)) {
            product.setImageurl(imageurl);
        }
        var wmaprice = element.elementTextTrim("wmaprice");
        if (StringUtils.hasText(wmaprice)) {
            product.setWmaprice(wmaprice);
        }
        var cid = element.elementTextTrim("cid");
        if (StringUtils.hasText(cid)) {
            product.setCid(Long.parseLong(cid));
        }
        return product;
    }

    private void inflationStatement(PreparedStatement statement, Product product) throws SQLException {
        statement.setLong(1, product.getId());
        statement.setString(2, product.getAdword());
        statement.setString(3, product.getWname());

        if (product.getAverageScore() != null) {
            statement.setInt(4, product.getAverageScore());
        } else {
            statement.setNull(4, JDBCType.INTEGER.ordinal());
        }

        var martPrice = product.getMartPrice();
        if (martPrice != null) {
            statement.setBigDecimal(5, martPrice);
        } else {
            statement.setNull(5, JDBCType.DECIMAL.ordinal());
        }

        var startRemainTime = product.getStartRemainTime();
        if (startRemainTime != null) {
            statement.setLong(6, startRemainTime);
        } else {
            statement.setNull(6, JDBCType.BIGINT.ordinal());
        }

        var promotion = product.getPromotion();
        if (promotion != null) {
            statement.setBoolean(7, promotion);
        } else {
            statement.setNull(7, JDBCType.BOOLEAN.ordinal());
        }

        var loc = product.getLoc();
        if (loc != null) {
            statement.setBoolean(8, loc);
        } else {
            statement.setNull(8, JDBCType.BOOLEAN.ordinal());
        }

        var jdPrice = product.getJdPrice();
        if (jdPrice != null) {
            statement.setBigDecimal(9, jdPrice);
        } else {
            statement.setNull(9, JDBCType.DECIMAL.ordinal());
        }
        statement.setString(10, product.getGood());

        var flashSale = product.getFlashSale();
        if (flashSale != null) {
            statement.setInt(11, flashSale);
        } else {
            statement.setNull(11, JDBCType.INTEGER.ordinal());
        }

        var online = product.getOnLine();
        if (online != null) {
            statement.setBoolean(12, online);
        } else {
            statement.setNull(12, JDBCType.BOOLEAN.ordinal());
        }

        var totalCount = product.getTotalCount();
        if (totalCount != null) {
            statement.setInt(13, totalCount);
        } else {
            statement.setNull(13, JDBCType.INTEGER.ordinal());
        }

        var book = product.getBook();
        if (book != null) {
            statement.setBoolean(14, book);
        } else {
            statement.setNull(14, JDBCType.BOOLEAN.ordinal());
        }

        var endRemainTime = product.getEndRemainTime();
        if (endRemainTime != null) {
            statement.setLong(15, endRemainTime);
        } else {
            statement.setNull(15, JDBCType.BIGINT.ordinal());
        }

        var wareId = product.getWareId();
        if (wareId != null) {
            statement.setLong(16, wareId);
        } else {
            statement.setNull(16, JDBCType.BIGINT.ordinal());
        }

        var canFreeRead = product.getCanFreeRead();
        if (canFreeRead != null) {
            statement.setBoolean(17, canFreeRead);
        } else {
            statement.setNull(17, JDBCType.BOOLEAN.ordinal());
        }

        statement.setString(18, product.getImageurl());
        statement.setString(19, product.getWmaprice());
        var cid = product.getCid();
        if (cid != null) {
            statement.setLong(20, cid);
        } else {
            statement.setNull(20, JDBCType.BIGINT.ordinal());
        }
    }

    public static void main(String[] args) {
        new Xml2MySQLBatchImporter().importe("classpath:item_jd.xml");
//        new Xml2MySQLBatchImporter().importe("classpath:one.xml");
    }
}
