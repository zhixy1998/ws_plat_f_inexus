package org.example;

import jdk.internal.dynalink.beans.StaticClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.plaf.SeparatorUI;
import javax.swing.text.Position;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class ProtocolItem {
    private String ID;
    private String Describe;
    private List<String> InputString = new ArrayList<>();
    private String[] SendBytes;

    // 1、载入xml
    public static Map<String,ProtocolItem> loadFormXml(String xmlPath) throws Exception {
        Map<String,ProtocolItem> map = new LinkedHashMap<>();
        File file =new File(xmlPath);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("ProtocolItem");

        for (int i = 0; i < nodeList.getLength(); i++){
            Element elem = (Element) nodeList.item(i);
            ProtocolItem item = new ProtocolItem();

            //从 XML 元素中读取指定标签的文本内容，并赋值
            item.ID = getText(elem,"ID");
            item.Describe = getText(elem,"Describe");

            //<InputString>
            Element inputElem = getElement(elem, "InputString");
            if (inputElem != null) {
                NodeList strings = inputElem.getElementsByTagName("string");
                for (int j = 0; j < strings.getLength(); j++) {
                    item.InputString.add(strings.item(j).getTextContent().trim());
                }
            }

            Element SendBytesElem = getElement(elem, "SendBytes");
            if (SendBytesElem != null) {
                String sendBytesStr = getText(SendBytesElem, "string");
                if (sendBytesStr != null && !sendBytesStr.isEmpty()) {
                    item.SendBytes = sendBytesStr.split(":"); // 按冒号分隔字符串
                }
            }

            // 将 item 添加到 map
            map.put(item.ID, item);
        }
        System.out.println(map);
        return map;
    }

    //获取指定的标签文本内容
    public static String getText(Element parent,String tag) {
        Element elem = getElement(parent, tag);
        return (elem != null) ? elem.getTextContent().trim() : "";
    }

    //获取指定标签的元素
    public static Element getElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        }
        return null;
    }

    @Override
    public String toString() {
        return "ProtocolItem{" +
                "ID='" + ID + '\'' +
                ", Describe='" + Describe + '\'' +
                ", InputString=" + InputString +
                ", SendBytes=" + Arrays.toString(SendBytes) +
                '}';
    }

    //输入参数处理
    void getInputValueDic (Object[] para, Map<String,String> valueDic, Map<String,String> typeDic) {
        for(int i = 0; i < InputString.size() && !InputString.get(i).isEmpty(); i++){
            String[] parts = InputString.get(i).split(":");
            String key = parts[1];
            String type = parts[2];
            if(para != null && para.length > i){
                valueDic.put(key, para[i].toString());
            }else{
                valueDic.put(key, "0");
            }
            typeDic.put(key, type);
        }
    }

    //指令组包
    public byte[] calcSendBytes(Object[] para){

        System.out.println("SendBytes before processing: " + Arrays.toString(SendBytes));


        if (SendBytes == null || SendBytes.length == 0){
            System.err.println("错误：SendBytes为空！");
            return new byte[0];
        }

//        String[] sendByteStr = SendBytes[0].split(":");
        Object[] cmd = new Object[SendBytes.length];
//        System.out.println(sendByteStr);

        Map<String, String> valueDic = new HashMap<>();
        Map<String, String> typeDic = new HashMap<>();
        getInputValueDic(para, valueDic, typeDic);

        Map<String, List<Integer>> positions = new HashMap<>();

        for (int i = 0; i < SendBytes.length; i++) {
            String token = SendBytes[i];
            System.out.print("token=" + token + " -> ");

            if (valueDic.containsKey(token)) {
                String value = valueDic.get(token);
                String type = typeDic.get(token);

                if (value != null && !value.isEmpty()) {
                    if ("3".equals(type)) {
                        // 立即用 toBytes 处理字符串（假设这个 slot 是 1 字节）
                        byte[] bytes = ProtocolItemCalculator.toBytes(value, 1, "3");
                        cmd[i] = bytes.length == 1 ? bytes[0] : bytes[bytes.length - 1];
                        System.out.println("string type -> " + Arrays.toString(bytes));
                    } else if ("0".equals(type)) {
                        try {
                            cmd[i] = Integer.parseInt(value); // 单字节数值
                            System.out.println("replaced with (type0) " + value);
                        } catch (NumberFormatException e) {
                            System.err.println("parseInt 失败 value=" + value);
                            cmd[i] = 0;
                        }
                    } else {
                        // 其他类型也可以直接放到 positions 让后面统一处理
                        cmd[i] = value;
                        System.out.println("other type=" + type + " value='" + value + "'");
                    }
                } else {
                    System.out.println("value empty, use 0");
                    cmd[i] = 0;
                }
            } else if (ProtocolItemCalculator.isNumeric(token)) {
                cmd[i] = ProtocolItemCalculator.eval(token);
                System.out.println("numeric literal -> " + cmd[i]);
            } else {
                System.out.println("no mapping, default 0");
                cmd[i] = 0;
                positions.computeIfAbsent(token, k -> new ArrayList<>()).add(i);
            }
        }


        for (String key : positions.keySet()){
            String[] tmp = new String[1];
            String result = ProtocolItemCalculator.getEquationValue(key, valueDic, tmp);

            if (result == null || result.isEmpty()) {
                System.out.println("[跳过] key=" + key + " 的表达式为空！");
                continue;
            }

            if (tmp[0] == null || !typeDic.containsKey(tmp[0])) {
                System.err.println("[错误] 未找到字段类型，key=" + key + ", tmp[0]=" + tmp[0]);
                continue;
            }

            String type = typeDic.get(tmp[0]);
            List<Integer> indexList = positions.get(key);
            byte[] valueBytes = ProtocolItemCalculator.toBytes(result, indexList.size(), type);

            for (int i = 0; i < indexList.size(); i++) {
                cmd[indexList.get(i)] = valueBytes.length == 1 ? valueBytes[0] : valueBytes[i];
            }
        }

        byte[] base = new byte[cmd.length];
        for (int i = 0; i < cmd.length; i++) {
            base[i] = (byte) (cmd[i] instanceof Number ? ((Number) cmd[i]).intValue() : 0);
        }

        System.out.println(Arrays.toString(base));  // 打印最终的字节数组

        int checksum = 0;
        for (byte b : base) {
            checksum += Byte.toUnsignedInt(b);
        }
        byte cs = (byte) (checksum & 0xFF);

        byte[] withChecksum = Arrays.copyOf(base, base.length + 1);
        withChecksum[withChecksum.length - 1] = cs;

        System.out.println(Arrays.toString(withChecksum));
        return withChecksum;
    }
}




