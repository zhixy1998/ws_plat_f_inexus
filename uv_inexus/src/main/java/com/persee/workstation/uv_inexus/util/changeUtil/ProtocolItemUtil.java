/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.util.changeUtil;

import com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList.ProtocolItemCalculator;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.*;

@Service("ProtocolItemUtil")
public class ProtocolItemUtil {

    public Map<String, Transition> loadFromXml(String xmlPath) throws Exception {
//        Map<String, Transition> map = new LinkedHashMap<>();
//        File file = new File(xmlPath);
//        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//        Document doc = builder.parse(file);
//        doc.getDocumentElement().normalize();
        Map<String, Transition> map = new LinkedHashMap<>();

        // 使用类加载器读取资源文件
        InputStream inputStream = ProtocolItemUtil.class.getClassLoader().getResourceAsStream(xmlPath);
        if (inputStream == null) {
            throw new Exception("无法找到资源文件: " + xmlPath);
        }
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("ProtocolItem");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element elem = (Element) nodeList.item(i);
            Transition item = new Transition();
            item.setInputString(new ArrayList<>());
            item.setOutputString(new ArrayList<>());
            item.setID(getText(elem, "ID"));
            item.setDescribe(getText(elem, "Describe"));
            item.setTimeout(getText(elem, "Timeout"));
            item.setACK(getText(elem, "ACK"));

            Element inputElem = getElement(elem, "InputString");
            if (inputElem != null) {
                NodeList strings = inputElem.getElementsByTagName("string");
                for (int j = 0; j < strings.getLength(); j++) {
                    item.getInputString().add(strings.item(j).getTextContent().trim());
                }
            }

            Element outputElem = getElement(elem, "OutputString");
            if (outputElem != null) {
                NodeList strings = outputElem.getElementsByTagName("string");
                for (int j = 0; j < strings.getLength(); j++) {
                    item.getOutputString().add(strings.item(j).getTextContent().trim());
                }
            }

            Element sendBytesElem = getElement(elem, "SendBytes");
            if (sendBytesElem != null) {
                item.setSendBytes(new String[]{getText(sendBytesElem, "string")});
            }

            Element readBytesElem = getElement(elem, "ReadBytes");
            if (readBytesElem != null) {
                item.setReadBytes(new String[]{getText(readBytesElem, "string")});
            }

            map.put(item.getID(), item);
        }

        return map;
    }

    private String getText(Element parent, String tag) {
        Element elem = getElement(parent, tag);
        return (elem != null) ? elem.getTextContent().trim() : "";
    }

    private Element getElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        }
        return null;
    }

    // ================= 3. 指令组包 =================
    public byte[] calcSendBytes(Object[] para, Transition transition) {
        if (transition.getSendBytes() == null || transition.getSendBytes().length == 0) {
            System.err.println("[错误] SendBytes 为空！");
            return new byte[0];
        }

        String[] sendByteStr = transition.getSendBytes()[0].split(":");
        Object[] cmd = new Object[sendByteStr.length];

        Map<String, Map<String, String>> dic = getInputValueDic(para, transition.getInputString());
        Map<String, String> valueDic = dic.get("valueDic");
        Map<String, String> typeDic = dic.get("typeDic");

        Map<String, List<Integer>> positions = new HashMap<>();
        int checkIndex = -1;

        for (int i = 0; i < sendByteStr.length; i++) {
            String token = sendByteStr[i];
            if ("check".equalsIgnoreCase(token)) {
                checkIndex = i;
                continue;
            }

            if (ProtocolItemCalculator.isNumeric(token)) {
                cmd[i] = ProtocolItemCalculator.eval(token);
            } else {
                cmd[i] = 0;
                positions.computeIfAbsent(token, k -> new ArrayList<>()).add(i);
            }
        }

        for (String key : positions.keySet()) {
            String[] tmp = new String[1];
            String result = ProtocolItemCalculator.getEquationValue(key, valueDic, tmp);

            if (result == null || result.isEmpty()) {
                continue;
            }

            if (tmp[0] == null || !typeDic.containsKey(tmp[0])) {
                continue;
            }

            String type = typeDic.get(tmp[0]);
            List<Integer> indexList = positions.get(key);
            byte[] valueBytes = ProtocolItemCalculator.toBytes(result, indexList.size(), type);

            for (int i = 0; i < indexList.size(); i++) {
                cmd[indexList.get(i)] = valueBytes.length == 1 ? valueBytes[0] : valueBytes[i];
            }
        }

        byte[] cmdb = new byte[cmd.length];
        for (int i = 0; i < cmd.length; i++) {
            cmdb[i] = (byte) (cmd[i] instanceof Number ? ((Number) cmd[i]).intValue() : 0);
        }

        if (checkIndex >= 0 && checkIndex < cmdb.length) {
            int checksum = 0;
            for (int i = 0; i < checkIndex; i++) {
                checksum += Byte.toUnsignedInt(cmdb[i]);
            }
            cmdb[checkIndex] = (byte) (checksum & 0xFF);
        }

        return cmdb;
    }

    // ================= 2. 输入参数处理 =================
    Map<String,Map<String, String>> getInputValueDic(Object[] para,List<String> InputString) {
        Map<String, String> valueDic = new HashMap<>();
        Map<String, String> typeDic = new HashMap<>();
        for (int i = 0; i < InputString.size() && !InputString.get(i).isEmpty(); i++) {
            String[] parts = InputString.get(i).split(":");
            String key = parts[1];
            String type = parts[2];
            if (para != null && para.length > i) {
                valueDic.put(key, para[i].toString());
            } else {
                valueDic.put(key, "0");
            }
            typeDic.put(key, type);
        }
        Map<String, Map<String, String> > map = new HashMap<>();
        map.put("valueDic", valueDic);
        map.put("typeDic", typeDic);
        return map;
    }
}
