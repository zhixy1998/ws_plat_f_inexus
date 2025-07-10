package com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList;/*
    将XML文件转换成指令集
* */


// 引入 DOM 相关类，用于解析 XML

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 主类：用于将 XML 配置加载为 Java 对象的 Map
public class XmlToCommandList {

    // 内部静态类，表示每一个 ProtocolItem 节点结构
    public static class ProtocolItem extends com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList.ProtocolItem {
        public String ID;                        // 指令 ID
        public String Describe;                  // 描述信息
        public int Timeout;                      // 超时时间（单位：毫秒）
        public boolean ACK;                      // 是否需要 ACK
        public List<String> InputStrings = new ArrayList<>();   // 输入字段列表
        public List<String> OutputStrings = new ArrayList<>();  // 输出字段列表
        public String SendBytes;                 // 要发送的字节字符串（可含公式）
        public String ReadBytes;                 // 要读取的字节长度信息（或表达式）
    }

    // 核心方法：从指定 XML 路径加载所有 ProtocolItem，返回 Map<ID, ProtocolItem>
    public static Map<String, ProtocolItem> loadFromXml(String xmlPath) throws Exception {
        Map<String, ProtocolItem> map = new LinkedHashMap<>(); // 使用 LinkedHashMap 保持顺序
        File file = new File(xmlPath);  // 读取 XML 文件

        // 创建 XML 解析器对象
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);   // 解析 XML 为 DOM 文档对象
        doc.getDocumentElement().normalize(); // 规范化 XML 文档结构（清除多余空白）

        // 获取所有 <ProtocolItem> 元素节点
        NodeList nodeList = doc.getElementsByTagName("ProtocolItem");

        // 遍历每一个 ProtocolItem 节点
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element elem = (Element) nodeList.item(i);
            ProtocolItem item = new ProtocolItem();

            // 解析基本字段：ID、Describe、Timeout、ACK
            item.ID = getText(elem, "ID");
            item.Describe = getText(elem, "Describe");
            item.Timeout = Integer.parseInt(getText(elem, "Timeout"));
            item.ACK = "Y".equalsIgnoreCase(getText(elem, "ACK")); // Y 表示 true

            // 解析 <InputString> 内所有 <string> 子节点
            Element inputElem = getElement(elem, "InputString");
            if (inputElem != null) {
                NodeList strings = inputElem.getElementsByTagName("string");
                for (int j = 0; j < strings.getLength(); j++) {
                    item.InputStrings.add(strings.item(j).getTextContent().trim());
                }
            }

            // 解析 <OutputString> 内所有 <string> 子节点
            Element outputElem = getElement(elem, "OutputString");
            if (outputElem != null) {
                NodeList strings = outputElem.getElementsByTagName("string");
                for (int j = 0; j < strings.getLength(); j++) {
                    item.OutputStrings.add(strings.item(j).getTextContent().trim());
                }
            }

            // 解析 <SendBytes> 节点下的 <string>（仅取一个）
            Element sendBytesElem = getElement(elem, "SendBytes");
            if (sendBytesElem != null) {
                item.SendBytes = getText(sendBytesElem, "string");
            }

            // 解析 <ReadBytes> 节点下的 <string>（仅取一个）
            Element readBytesElem = getElement(elem, "ReadBytes");
            if (readBytesElem != null) {
                item.ReadBytes = getText(readBytesElem, "string");
            }

            // 将当前 item 加入返回 map，key 为 ID
            map.put(item.ID, item);
        }

        return map;
    }

    // 辅助方法：从某个父节点中取某个子节点文本内容
    private static String getText(Element parent, String tag) {
        Element elem = getElement(parent, tag);
        return (elem != null) ? elem.getTextContent().trim() : "";
    }

    // 辅助方法：获取指定标签名的第一个子元素节点
    private static Element getElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        }
        return null;
    }
}
