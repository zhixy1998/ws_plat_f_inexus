package com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class ProtocolItem {
    public String ID;
    public String Describe;
    public String Timeout;
    public String ACK;
    public transient byte[] TypeByte;

    public List<String> InputString = new ArrayList<>();
    public List<String> OutputString = new ArrayList<>();
    public String[] SendBytes;
    public String[] ReadBytes;

    // ================= 1. 载入 XML =================
    public static Map<String, ProtocolItem> loadFromXml(String xmlPath) throws Exception {
        Map<String, ProtocolItem> map = new LinkedHashMap<>();
        File file = new File(xmlPath);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("ProtocolItem");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element elem = (Element) nodeList.item(i);
            ProtocolItem item = new ProtocolItem();

            item.ID = getText(elem, "ID");
            item.Describe = getText(elem, "Describe");
            item.Timeout = getText(elem, "Timeout");
            item.ACK = getText(elem, "ACK");

            Element inputElem = getElement(elem, "InputString");
            if (inputElem != null) {
                NodeList strings = inputElem.getElementsByTagName("string");
                for (int j = 0; j < strings.getLength(); j++) {
                    item.InputString.add(strings.item(j).getTextContent().trim());
                }
            }

            Element outputElem = getElement(elem, "OutputString");
            if (outputElem != null) {
                NodeList strings = outputElem.getElementsByTagName("string");
                for (int j = 0; j < strings.getLength(); j++) {
                    item.OutputString.add(strings.item(j).getTextContent().trim());
                }
            }

            Element sendBytesElem = getElement(elem, "SendBytes");
            if (sendBytesElem != null) {
                item.SendBytes = new String[]{getText(sendBytesElem, "string")};
            }

            Element readBytesElem = getElement(elem, "ReadBytes");
            if (readBytesElem != null) {
                item.ReadBytes = new String[]{getText(readBytesElem, "string")};
            }

            map.put(item.ID, item);
        }

        return map;
    }

    private static String getText(Element parent, String tag) {
        Element elem = getElement(parent, tag);
        return (elem != null) ? elem.getTextContent().trim() : "";
    }

    private static Element getElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        }
        return null;
    }

    // ================= 2. 输入参数处理 =================
    void getInputValueDic(Object[] para, Map<String, String> valueDic, Map<String, String> typeDic) {
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
    }

    // ================= 3. 指令组包 =================
    public byte[] calcSendBytes(Object[] para) {
        if (SendBytes == null || SendBytes.length == 0) {
            System.err.println("[错误] SendBytes 为空！");
            return new byte[0];
        }

        String[] sendByteStr = SendBytes[0].split(":");
        Object[] cmd = new Object[sendByteStr.length];

        Map<String, String> valueDic = new HashMap<>();
        Map<String, String> typeDic = new HashMap<>();
        getInputValueDic(para, valueDic, typeDic);

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
                System.err.println("[跳过] key=" + key + " 的表达式为空！");
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

    // ================= 4. 指令解包 =================
    public Object[] parseOutputs(byte[] reads, int startPos, int endPos) {
        Object[] ret = new Object[OutputString.size()];
        Map<String, int[]> positions = new HashMap<>();
        String[] rs = ReadBytes[0].split(":");

        for (int i = 0; i < rs.length; i++) {
            if (!isNumeric(rs[i]) && !"check".equalsIgnoreCase(rs[i])) {
                int finalI = i;
                int finalI1 = i;
                positions.computeIfAbsent(rs[i], k -> new int[]{finalI, finalI1})[1] = i;
            }
        }

        for (int i = 0; i < OutputString.size(); i++) {
            String[] parts = OutputString.get(i).split(":");
            String key = parts[1];
            String type = parts[2];
            for (String k : positions.keySet()) {
                if (k.contains(key)) {
                    int[] pos = positions.get(k);
                    byte[] bs = Arrays.copyOfRange(reads, startPos + pos[0], startPos + pos[1] + 1);
                    Object parsed = ProtocolItemCalculator.parseBytes(bs, type);
                    ret[i] = ProtocolItemCalculator.getElementValue(k, parsed);
                }
            }
        }
        return ret;
    }

    public Object[] parseOutputSN(Queue<Byte> reads, int startPos, byte[] head) {
        Object[] ret = new Object[OutputString.size()];
        Map<String, int[]> positions = new HashMap<>();
        String[] rs = ReadBytes[0].split(":");

        for (int i = 0; i < rs.length; i++) {
            if (!isNumeric(rs[i]) && !"check".equalsIgnoreCase(rs[i])) {
                int finalI = i;
                int finalI1 = i;
                positions.computeIfAbsent(rs[i], k -> new int[]{finalI, finalI1})[1] = i;
            }
        }

        for (int i = 0; i < OutputString.size(); i++) {
            String[] parts = OutputString.get(i).split(":");
            String key = parts[1];
            for (String k : positions.keySet()) {
                if (k.contains(key)) {
                    int[] pos = positions.get(k);
                    byte[] bs = new byte[pos[1] - pos[0] + 1];
                    for (int j = 0; j < bs.length; j++) {
                        bs[j] = ((List<Byte>) reads).get(j + startPos + pos[0]);
                    }
                    StringBuilder builder = new StringBuilder();
                    for (byte b : bs) builder.append(String.format("%02d", b));
                    ret[i] = builder.toString();
                }
            }
        }

        return new Object[] {
                String.format("%s-%s-%s-%s", ret[0], ret[1], ret[2], ret[3])
        };
    }

    public int tryGetKey(String value, Map<Integer, String> positions) {
        return positions.entrySet().stream()
                .filter(e -> e.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(0);
    }

    public boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
