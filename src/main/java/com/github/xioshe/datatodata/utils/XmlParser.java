package com.github.xioshe.datatodata.utils;

import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;


@Slf4j
public class XmlParser {

    public static Document parse(String path) {
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            document = reader.read(ResourceUtils.getFile(path));
        } catch (DocumentException e) {
            log.error(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return document;
    }
}
