package com.github.xioshe.datatodata.utils;


public class Sqls {

  public static String INSERT_SQL = "INSERT INTO product (id, adword, wname, average_score, mart_price, start_remain_time, promotion, loc, jd_price, good, flash_sale, on_line, total_count, book, end_remain_time, ware_id, can_free_read, imageurl, wmaprice, cid) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


    public static String SAVE_SQL = """
            INSERT INTO product (id, adword, wname, average_score, mart_price, start_remain_time, promotion, loc, jd_price, good, flash_sale, on_line, total_count, book, end_remain_time, ware_id, can_free_read, imageurl, wmaprice, cid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              adword = VALUES(adword),
              wname = VALUES(wname),
              average_score = VALUES(average_score),
              mart_price = VALUES(mart_price),
              start_remain_time = VALUES(start_remain_time),
              promotion = VALUES(promotion),
              loc = VALUES(loc),
              jd_price = VALUES(jd_price),
              good = VALUES(good),
              flash_sale = VALUES(flash_sale),
              on_line = VALUES(on_line),
              total_count = VALUES(total_count),
              book = VALUES(book),
              end_remain_time = VALUES(end_remain_time),
              ware_id = VALUES(ware_id),
              can_free_read = VALUES(can_free_read),
              imageurl = VALUES(imageurl),
              wmaprice = VALUES(wmaprice),
              cid = VALUES(cid)
            """.trim();

}
