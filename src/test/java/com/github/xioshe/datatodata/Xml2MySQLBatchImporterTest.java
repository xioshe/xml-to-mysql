package com.github.xioshe.datatodata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class Xml2MySQLBatchImporterTest {

    Xml2MySQLBatchImporter importer = new Xml2MySQLBatchImporter();

    @Test
    void test_parse() throws IOException {
        var document = importer.parse("classpath:one.xml");
        Assertions.assertNotNull(document);
        Assertions.assertEquals("22", document.selectSingleNode("/RECORDS/RECORD/id").getText());
    }

}