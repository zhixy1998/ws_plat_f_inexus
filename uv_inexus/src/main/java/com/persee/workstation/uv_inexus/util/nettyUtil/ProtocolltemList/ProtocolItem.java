package com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList;/*
    识别一条ProtocolItem
* */


import java.util.*;

public class ProtocolItem {
        public String ID;
        public String Describe;
        public String Timeout;
        public String ACK;

        // XML 忽略字段，在 Java 中不参与序列化可以使用 transient
        public transient byte[] TypeByte;

        public List<String> InputString;
        public List<String> OutputString;
        public String[] SendBytes;
        public String[] ReadBytes;

        // 获取输入值字典和类型字典
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

        //根据变量、公式生成完整 byte[]
        public byte[] getNewIpSendBytes(Object[] para) {
            String[] sendByteStr = SendBytes[0].split(":");
            Map<String, String> valueDic = new HashMap<>();
            Map<String, String> typeDic = new HashMap<>();
            getInputValueDic(para, valueDic, typeDic);

            byte[] instructions = new byte[sendByteStr.length];
            for (int i = 0; i < sendByteStr.length - 1; i++) {
                if (!isNumeric(sendByteStr[i])) {
                    for (String key : valueDic.keySet()) {
                        if (sendByteStr[i].contains(key)) {
                            sendByteStr[i] = valueDic.get(key);
                        }
                    }
                }
                instructions[i] = (byte) Integer.parseInt(sendByteStr[i]);
            }
            return instructions;
        }

        //根据变量、公式生成完整 byte[]
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

            Map<String, List<Integer>> positions = new HashMap<>();  // 关键改动：记录所有出现位置
            int checkIndex = -1;

            // 第一步：初始化 cmd，记录变量名位置
            for (int i = 0; i < sendByteStr.length; i++) {
                String token = sendByteStr[i];
                if ("check".equalsIgnoreCase(token)) {
                    checkIndex = i;
                    continue;
                }

                if (ProtocolItemCalculator.isNumeric(token)) {
                    cmd[i] = ProtocolItemCalculator.eval(token);
                } else {
                    cmd[i] = 0; // 占位
                    positions.computeIfAbsent(token, k -> new ArrayList<>()).add(i);
                }
            }

            // 第二步：将变量替换为值
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
                int repeatCount = indexList.size();

                byte[] valueBytes = ProtocolItemCalculator.toBytes(result, repeatCount, type);

                for (int i = 0; i < indexList.size(); i++) {
                    int pos = indexList.get(i);
                    if (pos < cmd.length) {
                        // 如果 valueBytes 只有 1 个字节，重复写入
                        cmd[pos] = valueBytes.length == 1 ? valueBytes[0] : valueBytes[i];
                    }
                }

            }

            // 第三步：转换成 byte[]
            byte[] cmdb = new byte[cmd.length];
            for (int i = 0; i < cmd.length; i++) {
                cmdb[i] = (byte) (cmd[i] instanceof Number ? ((Number) cmd[i]).intValue() : 0);
            }

            // 第四步：计算 check 校验和
            if (checkIndex >= 0 && checkIndex < cmdb.length) {
                int checksum = 0;
                for (int i = 0; i < checkIndex; i++) {
                    checksum += Byte.toUnsignedInt(cmdb[i]);
                }
                cmdb[checkIndex] = (byte) (checksum & 0xFF);
            }

            return cmdb;
        }




    //	将回应 byte[] 转换为 Java 对象
        public Object[] parseOutputs(byte[] reads, int startPos, int endPos) {
            Object[] ret = new Object[OutputString.size()];
            Map<String, int[]> positions = new HashMap<>();
            String[] rs = ReadBytes[0].split(":");

            for (int i = 0; i < rs.length; i++) {
                if (!isNumeric(rs[i]) && !"check".equalsIgnoreCase(rs[i])) {
                    int finalI = i;
                    int finalI1 = i;
                    positions.computeIfAbsent(rs[i], k -> new int[]{finalI, finalI1});
                    positions.get(rs[i])[1] = i;
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

        //简化形式返回序列号等拼接字符串
        public Object[] parseOutputSN(Queue<Byte> reads, int startPos, byte[] head) {
            // 简化，只展示一个场景
            Object[] ret = new Object[OutputString.size()];
            Map<String, int[]> positions = new HashMap<>();
            String[] rs = ReadBytes[0].split(":");

            for (int i = 0; i < rs.length; i++) {
                if (!isNumeric(rs[i]) && !"check".equalsIgnoreCase(rs[i])) {
                    int finalI = i;
                    int finalI1 = i;
                    positions.computeIfAbsent(rs[i], k -> new int[]{finalI, finalI1});
                    positions.get(rs[i])[1] = i;
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

//    tryGetKey(value, map) 就是：
//     “我知道某个值 value，你告诉我这个值在 map 的哪个 key 上”。
//    如果没有，就默认返回 0。
        public int tryGetKey(String value, Map<Integer, String> positions) {
            return positions.entrySet().stream()
                    .filter(e -> e.getValue().equals(value))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(0);
        }

        //判断字符串是否是数值
        public boolean isNumeric(String str) {
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

}
