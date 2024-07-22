package com.github.xioshe.datatodata;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class Product {

    private Long id;
    // 广告词
    private String adword;
    // 商品名称
    private String wname;
    // 评分
    private Integer averageScore;
    private BigDecimal martPrice;
    private Long startRemainTime;
    // 促销
    private Boolean promotion;
    private Boolean loc;
    private BigDecimal jdPrice;
    // 好评
    private String good;
    // 限时抢购
    private Integer flashSale;
    private Boolean onLine;
    private Integer totalCount;
    // 书本标记
    private Boolean book;
    private Long endRemainTime;
    private Long wareId;
    private Boolean canFreeRead;
    private String imageurl;
    private String wmaprice;
    // 类别 id
    private Long cid;

    // Getters and setters omitted for brevity

}
