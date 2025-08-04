package org.example;

import java.util.Arrays;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        Map<String, ProtocolItem> map = ProtocolItem.loadFormXml("D:\\project\\java\\T800.1.xml");
        Object[] para = new Object[]{"你",6,6,6};
        ProtocolItem item = map.get("Init");

        // 调用 calcSendBytes 方法
        item.calcSendBytes(para);
    }
}
