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

package com.persee.workstation.uv_inexus.util.changeUtil.T800;

import com.persee.workstation.uv_inexus.util.changeUtil.ProtocolItemUtil;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;


@Service("ProtocolItem")
public class ProtocolItem {

    // 1、载入xml
    public Map<String, T800Transition> loadFormXml(String xmlPath) throws Exception {
        Map<String,T800Transition> map = new LinkedHashMap<>();

        // 使用类加载器读取资源文件
        InputStream inputStream = ProtocolItemUtil.class.getClassLoader().getResourceAsStream(xmlPath);
        if (inputStream == null) {
            throw new Exception("无法找到资源文件: " + xmlPath);
        }
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("ProtocolItem");

        for (int i = 0; i < nodeList.getLength(); i++){
            Element elem = (Element) nodeList.item(i);
            T800Transition item = new T800Transition();
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

    //获取指定的标签文本内容
    public String getText(Element parent,String tag) {
        Element elem = getElement(parent, tag);
        return (elem != null) ? elem.getTextContent().trim() : "";
    }

    //获取指定标签的元素
    public Element getElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        }
        return null;
    }

//    @Override
//    public String toString() {
//        return "ProtocolItem{" +
//                "ID='" + ID + '\'' +
//                ", Describe='" + Describe + '\'' +
//                ", InputString=" + InputString +
//                ", SendBytes=" + Arrays.toString(SendBytes) +
//                '}';
//    }

    //输入参数处理
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

    //指令组包
    public byte[] T800calcSendBytes(Object[] para , T800Transition T800Transition) {

        System.out.println("SendBytes before processing: " + Arrays.toString(T800Transition.getSendBytes()));

        if (T800Transition.getSendBytes() == null || T800Transition.getSendBytes().length == 0) {
            System.err.println("错误：SendBytes为空！");
            return new byte[0];
        }

        String[] sendByteStr = T800Transition.getSendBytes()[0].split(":");
        Object[] cmd = new Object[sendByteStr.length];

        Map<String, Map<String, String>> dic = getInputValueDic(para, T800Transition.getInputString());
        Map<String, String> valueDic = dic.get("valueDic");
        Map<String, String> typeDic = dic.get("typeDic");;

        Map<String, List<Integer>> positions = new HashMap<>();


        // 第一轮：替换固定值、输入值
        for (int i = 0; i < sendByteStr.length; i++) {
            String token = sendByteStr[i];
            System.out.print("token=" + token + " -> ");

            if (valueDic.containsKey(token)) {
                String value = valueDic.get(token);
                String type = typeDic.get(token);

                if (value != null && !value.isEmpty()) {
                    if ("3".equals(type)) {
                        byte[] bytes = ProtocolItemCalculator.toBytes(value, 1, "3");
                        cmd[i] = bytes.length == 1 ? bytes[0] : bytes[bytes.length - 1];
                        System.out.println("string type -> " + Arrays.toString(bytes));
                    } else if ("0".equals(type)) {
                        try {
                            cmd[i] = Integer.parseInt(value);
                            System.out.println("replaced with (type0) " + value);
                        } catch (NumberFormatException e) {
                            System.err.println("parseInt 失败 value=" + value);
                            cmd[i] = 0;
                        }
                    } else if ("12".equals(type)) {
                        try {
                            cmd[i] = Integer.parseInt(value) & 0xFFF; // 12bit 先存整数
                            System.out.println("type12 -> " + cmd[i]);
                        } catch (Exception e) {
                            cmd[i] = 0;
                        }
                    } else {
                        cmd[i] = value;
                        System.out.println("other type=" + type + " value='" + value + "'");
                    }
                } else {
                    cmd[i] = 0;
                    System.out.println("value empty, use 0");
                }
            } else if (ProtocolItemCalculator.isNumeric(token)) {
                cmd[i] = ProtocolItemCalculator.eval(token);
                System.out.println("numeric literal -> " + cmd[i]);
            } else {
                cmd[i] = 0;
                System.out.println("no mapping, default 0");
                positions.computeIfAbsent(token, k -> new ArrayList<>()).add(i);
            }
        }

        // 第二轮：处理表达式占位符
        for (String key : positions.keySet()) {
            String[] tmp = new String[1];
            String result = ProtocolItemCalculator.getEquationValue(key, valueDic, tmp);
            if (result == null || result.isEmpty()) continue;
            if (tmp[0] == null || !typeDic.containsKey(tmp[0])) continue;

            String type = typeDic.get(tmp[0]);
            List<Integer> indexList = positions.get(key);

            if ("12".equals(type)) {
                int val = Integer.parseInt(result) & 0xFFF;
                cmd[indexList.get(0)] = val;
            } else {
                byte[] valueBytes = ProtocolItemCalculator.toBytes(result, indexList.size(), type);
                for (int i = 0; i < indexList.size(); i++) {
                    cmd[indexList.get(i)] = valueBytes.length == 1 ? valueBytes[0] : valueBytes[i];
                }
            }
        }

        // 新增：连续位拼接 8bit/12bit
        List<Integer> bitWidths = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < cmd.length; i++) {
            int val = 0;
            int bitWidth = 8;
            if (cmd[i] instanceof Number) {
                val = ((Number) cmd[i]).intValue();
            }
            String type = typeDic.getOrDefault(sendByteStr[i], "0");
            if ("12".equals(type)) bitWidth = 12;
            bitWidths.add(bitWidth);
            values.add(val);
        }

        List<Byte> out = new ArrayList<>();
        int bitBuffer = 0;
        int bitCount = 0;

        for (int i = 0; i < values.size(); i++) {
            bitBuffer = (bitBuffer << bitWidths.get(i)) | values.get(i);
            bitCount += bitWidths.get(i);

            while (bitCount >= 8) {
                bitCount -= 8;
                out.add((byte) ((bitBuffer >> bitCount) & 0xFF));
            }
        }

        if (bitCount > 0) {
            out.add((byte) ((bitBuffer << (8 - bitCount)) & 0xFF));
        }

        byte[] base = new byte[out.size()];
        for (int i = 0; i < out.size(); i++) base[i] = out.get(i);

        System.out.println("拼包结果 (不含校验和): " + Arrays.toString(base));

        // 校验和
        int checksum = 0;
        for (byte b : base) checksum += Byte.toUnsignedInt(b);
        byte[] withChecksum = Arrays.copyOf(base, base.length + 1);
        withChecksum[withChecksum.length - 1] = (byte) (checksum & 0xFF);

        System.out.println("最终报文 (含校验和): " + Arrays.toString(withChecksum));
        return withChecksum;
    }
}




