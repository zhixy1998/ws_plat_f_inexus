/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.changeUtil;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.StepPoint;
import com.persee.workstation.uv_inexus.util.changeUtil.T800.ProtocolItem;
import com.persee.workstation.uv_inexus.util.changeUtil.T800.ResponseParser;
import com.persee.workstation.uv_inexus.util.changeUtil.T800.T800MessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.T800.T800Transition;
import com.persee.workstation.uv_inexus.vo.instrumentVo.FindDeviceVO;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.persee.workstation.uv_inexus.util.changeUtil.ResponseDeal.deviceInfo;

@Service("DevMessageUtil")
@Slf4j
public class DevMessageUtil {
    @Autowired
    MessageUtil messageUtil;

    @Autowired
    ProtocolItemUtil protocolItemUtil;

    @Autowired
    ProtocolItem protocolItem;

    @Autowired
    T800MessageUtil t800MessageUtil;

    @Autowired
    FormulaUtil formulaUtil;

//    @Autowired
//    private ExperimentTestService experimentService;

    @Value("${device.default.T600xmlPath}")
    String T600xmlPath;

    @Value("${device.default.T800xmlPath}")
    String T800xmlPath;


//    String xmlPath = "E:\\product\\ws_plat_inexus\\uv_inexus\\src\\main\\java\\com\\persee\\workstation\\uv_inexus\\util\\changeUtil\\T600.1.xml" ;

    private volatile static String defaultIp = "255.255.255.255";

    private volatile static String T800defaultIp = "192.168.61.68";
//    private volatile static String defaultIp = "192.168.32.254";
    private volatile  Map<String, Transition> xml;

    private static String ipEnd = "253";

    private static String flag = "T600" ;

    // 缓存一份 T800 协议表
    private  Map<String, T800Transition> t800xml;

    /**
     * 连接设备
     * @param list
     * @return
     */
    public R<FindDeviceVO> connectDevice(List<Command> list) throws Exception {
        while (!DetectionConstants.isDeviceIdle) {
            Thread.sleep(100);
        }
        Constants.devConnectInfo.clear();
        DetectionConstants.isDeviceIdle = false;

        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();
        defaultIp = "255.255.255.255";

        byte[] sendData = new byte[]{
                (byte) 0xFF, (byte) 0xB5, 0x00, 0x00, 0x00, 0x14, 0x00, 0x28,
                0x14, (byte) 0xFC, 0x00, 0x1B, 0x00, 0x10, 0x21, 0x01,
                0x01, 0x00, 0x00, 0x30, (byte) 0xE0, (byte) 0xA6, 0x66,
                (byte) 0x89, 0x5A, 0x13, (byte) 0x88, (byte) 0xC0, (byte) 0xA8,
                0x01, 0x64, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00,
                (byte) 0xC0, (byte) 0xA8, 0x01, 0x01, 0x0A, 0x26
        };

        boolean isT800 = sendBroadcastAndWaitReply(socket, sendData, 5000, 100);

        System.out.println("最终结果: " + isT800);

        // 2. 根据结果走不同逻辑
        if (isT800) {
            flag = "T600"; //******************************************
            System.out.println("检测到T800设备");

            return connectT800(socket, list);
        } else {
            flag = "T600";
            System.out.println("检测到T600设备");
            return connectT600(socket, list);
        }
    }

    public R<FindDeviceVO> connectT600(DatagramSocket socket, List<Command> list) {
        Boolean b = changeXml(T600xmlPath);
        if (!b) {
            return R.fail("T600xml文件加载失败");
        }
        FindDeviceVO findDeviceVO = new FindDeviceVO();
        //获取的波长值临时存储
        Object temp=new Object();
        for (Command command : list) {
            String cmdId = command.getCmdId();
            if (cmdId.equals(FindDevEnum.FindDevice.getComId())){
                Object[] param = new Object[]{0,0,0,0,0,0};
                String macAddress = NetworkUtils.getEthernetMacAddress();
                if (macAddress != null) {
                    String[] macParts = macAddress.split(":");
                    // 将上位机的前 6 个 MAC 地址部分插入到 param 数组的前 6 个位置
                    for (int i = 0; i < 6; i++) {

                        int anInt = Integer.parseInt(macParts[i], 16);

                        param[i] = anInt;  // 将每一部分从十六进制转换为十进制
                    }
                }
                command.setParams(param);
            }

            if (cmdId.equals(FindDevEnum.NewIP.getComId())) {
                Object[] param = new Object[]{0, 0, 0, 0, 0, 0, 52, 218, 166, 101, 144, 86, 169, 254, 136, 253};
//                Object[] param = new Object[]{64, 194, 186, 169, 8, 100, 52, 221, 166, 101, 166, 103, 169, 254, 136, 253};
//                String address = socket.getLocalAddress().getHostAddress();
//                Object[] split = address.split("\\.");
//                int length = param.length;
//                for (int i = 2; i >= 0; i--) {
//                    param[length - i - 2] = split[2-i];
//                }
//                command.setParams(param);
//            }

                // 获取上位机的 MAC 地址
                String macAddress = NetworkUtils.getEthernetMacAddress();
                if (macAddress != null) {
                    String[] macParts = macAddress.split(":");
                    // 将上位机的前 6 个 MAC 地址部分插入到 param 数组的前 6 个位置
                    for (int i = 0; i < 6; i++) {
                        param[i] = Integer.parseInt(macParts[i], 16);  // 将每一部分从十六进制转换为十进制
                    }
                }

                // 获取下位机的 MAC 地址的十进制数据
                if (deviceInfo.size() == 6) {
                    for (int i = 0; i < deviceInfo.size(); i++) {
                        param[6 + i] = deviceInfo.get(i);  // 将 MAC 地址的十进制值填充到 param 数组的相应位置
                    }
                }

                // 获取本机的 IP 地址并插入到 param 数组的倒数 3 位
                String address = socket.getLocalAddress().getHostAddress();
                String[] split = address.split("\\.");
                for (int i = 0; i <= 2; i++) {
                    param[12 + i] = Integer.parseInt(split[i]);  // 逆序插入 IP 地址的最后 3 个部分
                }
                // 设置参数
                command.setParams(param);
            }

            if (cmdId.equals(FindDevEnum.Connect.getComId())) {
                String cmdId1 = command.getCmdId();
                if (cmdId1.equals(FindDevEnum.Connect.getComId())) {
                    String macAddress = NetworkUtils.getEthernetMacAddress();
                    String[] macParts = macAddress.split(":");
                    Object[] param = new Object[6];
                    for (int i = 0; i < 6; i++){
                        param[i] = Integer.parseInt(macParts[i], 16);
                    }
                    command.setParams(param);
                }
            }

            //发送报文组
            Transition transition = xml.get(cmdId);
            int ReplyNumber = transition.getReplyNumber();
            int Timeout = transition.getTimeout();
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.quickSendAndReceive(socket, defaultIp, cmdId, bytes, ReplyNumber, Timeout);

            if (responses == null || responses.isEmpty()) {
                if (cmdId.equals(FindDevEnum.FindDevice.getComId())) {
                    return R.fail("没有仪器响应，请检查仪器连接状态！");
                } else if (cmdId.equals(FindDevEnum.GetWlList.getComId()) || cmdId.equals(FindDevEnum.GetCellPos.getComId())){
                    break;
                } else {
                    return R.fail("仪器连接失败，请检查仪器！");
                }

            }

            Object parse = ResponseDeal.parse(cmdId, responses, bytes);

            if (parse == null) {
                return R.fail("接收报文解析异常");
            }

//            if (cmdId.equals(FindDevEnum.FindDevice.getComId())) {
//                defaultIp = parse.toString();
//                findDeviceVO.setIp(defaultIp);
//            }


            if (cmdId.equals(FindDevEnum.NewIP.getComId())) {
                String ip = socket.getLocalAddress().getHostAddress();
                String[] split = ip.split("\\.");
                String s = ip.substring(0, ip.length() - split[split.length - 1].length()) + ipEnd;
                findDeviceVO.setIp(s);
                defaultIp = s;
                continue;
            }

            if (cmdId.equals(FindDevEnum.GetSn.getComId())) {
                String s = String.valueOf(parse);
                s = s.substring(0, 16);
                findDeviceVO.setDeviceSerialNumber(s);
                continue;
            }
            if (cmdId.equals(FindDevEnum.GetInitStatus.getComId())) {
                if (parse.toString().equals("true")) {
                    findDeviceVO.setIsInited(1);
                } else {
                    findDeviceVO.setIsInited(0);
                }
            }
            if (cmdId.equals(FindDevEnum.GetAccessoryType.getComId())) {

                Integer s = (Integer) parse;
                findDeviceVO.setAttachmentType(s);
            }
            if (cmdId.equals(FindDevEnum.GetWlList.getComId())) {
                temp=parse;
            }
            if (cmdId.equals(FindDevEnum.GetCellPos.getComId())) {
                Integer s = (Integer) parse;
                findDeviceVO.setCurrentPoolNum(s);
            }
        }
        findDeviceVO.setLinearSegment(formulaUtil.addFormula(temp,findDeviceVO.getDeviceSerialNumber()));
        return R.ok(findDeviceVO);
    }

    private R<FindDeviceVO> connectT800(DatagramSocket socket, List<Command> list) throws Exception {
        // 这里可以加载 T800 专用的 XML 或协议定义
        Boolean b = T800changeXml(T800xmlPath);
        if (!b) {
            return R.fail("T800xml文件加载失败");
        }
        FindDeviceVO findDeviceVO = new FindDeviceVO();
        findDeviceVO.setAttachmentType(25);
        findDeviceVO.setIsDevOpened(1);
        findDeviceVO.setIp("192.168.61.89");
        findDeviceVO.setIsInited(1);

        for (Command command : list) {
            String cmdId = command.getCmdId();
            if (cmdId.equals(FindDevEnum.NewIP.getComId())) {
                Object[] param = new Object[]{0, 0, 0, 0, 0, 0, 52, 218, 166, 101, 144, 86, 169, 254, 136, 253};
                String address = socket.getLocalAddress().getHostAddress();
                Object[] split = address.split("\\.");
                int length = param.length;
                for (int i = 2; i >= 0; i--) {
                    param[length - i - 2] = split[2-i];
                }
                command.setParams(param);
            }

            //**********************************改这里了********************************************
            if (!ensureT800ProtocolsLoaded()) {
                return R.fail("T800 xml 加载失败");
            }

            T800Transition item = t800xml.get(cmdId);
            if (item == null) {
                log.warn("T800 协议中未找到 cmdId={}", cmdId);
                continue;
            }

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,t800xml.get(cmdId));
            t800MessageUtil.sendCommand(cmdId, bytes, defaultIp);
            String result = t800MessageUtil.getResultSingle(cmdId, 4000);
            String result1 = ResponseParser.Result;

            if (result != null) {
                System.out.println("解析结果：" + result);
            } else {
                System.out.println("超时未收到解析结果");
            }

            if (cmdId.equals(FindDevEnum.GetSn.getComId())) {
                String s = String.valueOf(result1);
                s = s.substring(0, 16);
                findDeviceVO.setDeviceSerialNumber(s);
                continue;
            }
            if (cmdId.equals(FindDevEnum.GetInitStatus.getComId())) {
                if (result1.equals("true")) {
                    findDeviceVO.setIsInited(1);
                } else {
                    findDeviceVO.setIsInited(2);
                }
            }
        }
        return R.ok(findDeviceVO);
    }


    /**
     * 断开连接
     */
    public void disConnect(String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return;
        }
        Command command = new Command("DisConnect", new Object[]{});
        DatagramSocket socket = ans.getData();
        if(flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return;
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.quickSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse.equals("false")) {
                return;
            }
        }
        else if(flag == "T800"){
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return;
            }
            String cmdId = command.getCmdId();

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,t800xml.get(cmdId));
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String result = ResponseParser.Result;

            if (result.equals("false")) {
                return;
            }
        }
    }


    /**
     * 设置单条指令
     * @param command
     * @param deviceIp
     */
    public void SetOneCommand(Command command,String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return;
        }
        DatagramSocket socket = ans.getData();
        if(flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return;
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.quickSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse.equals("false")) {
                return;
            }
        }
        else if(flag == "T800"){
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return;
            }
            String cmdId = command.getCmdId();

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,t800xml.get(cmdId));
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String result = ResponseParser.Result;

            if (result.equals("false")) {
                return;
            }
        }
    }

    /**
     * 仪器校正
     */
    public R<String> deviceCalibration(Command command, String deviceIp) throws InterruptedException {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();
        if(flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return R.fail("xml文件加载失败");
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                responses = messageUtil.quickSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
                if (responses.size() < 2) {
                    i++;
                } else {
                    break;
                }
            }

            if (responses.size() < 2) {
                return R.fail("校正失败");
            }
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            String result = parse.toString();
            if ("false".equals(result)) {
                return R.fail();
            }
        }
        else if(flag == "T800"){
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return R.fail("T800xml文件加载失败");
            }
            String cmdId = command.getCmdId();

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,t800xml.get(cmdId));
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                return R.fail("T800发送命令失败");
            }
//            String result = t800MessageUtil.getResultSingle(cmdId, 3000);
            String result = ResponseParser.Result;

            if (result != null) {
                System.out.println("解析结果：" + result);
            } else {
                System.out.println("超时未收到解析结果");
            }
            if ("false".equals(result)) {
                return R.fail();
            }

        }

        return R.ok();
    }

    /**
     * 改变灯状态或切换灯
     */
    public Boolean changeLamp(Command command, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return false;
        }
        DatagramSocket socket = ans.getData();
        if(flag.equals("T600")) {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return false;
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.quickSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            return !parse.equals("false");
        }
        else if(flag.equals("T800")){
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return false;
            }
            String cmdId = command.getCmdId();

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,t800xml.get(cmdId));
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String result = ResponseParser.Result;

            return !result.equals("false");
        }
        return true;
    }

    /**
     * 多波长校正，波长实际情况获取，发现设备后执行
     */
    public R<String> dealWlList(String deviceSerialNumber, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();
        Command command = new Command("GetWlList",  null);
        if(flag.equals("T600")) {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return R.fail("xml文件加载失败");
            }


            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            String result = String.valueOf(parse);
            List<StepPoint> linearSegments = JSON.parseArray(parse.toString(), StepPoint.class);
            if ("false".equals(result)) {
                return R.fail();
            }
//            formulaUtil.addFormula(linearSegments, deviceSerialNumber);
        }
        else if(flag == "T800"){
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return R.fail("T800xml文件加载失败");
            }
            String cmdId = command.getCmdId();

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,t800xml.get(cmdId));
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                return R.fail("T800发送命令失败");
            }
//            String result = t800MessageUtil.getResultSingle(cmdId, 3000);
            String result = ResponseParser.Result;

            if (result != null) {
                System.out.println("解析结果：" + result);
            } else {
                System.out.println("超时未收到解析结果");
            }
            if ("false".equals(result)) {
                return R.fail();
            }

        }

        return R.ok();
    }


    /**
     * 初始化设备，暂放
     *
     * @param
     * @return
     */
    public R<String> initDevice(Command  command, String deviceIp) throws Exception {
//        R<DatagramSocket> ans = messageUtil.getSocket();
//        if (!ans.getSuccess()) {
//            return R.fail(ans.getMessage());
//        }
//        DatagramSocket socket = ans.getData();
//        if(flag.equals("T600")) {
//            Boolean b = changeXml(T600xmlPath);
//            if (!b) {
//                return R.fail("xml文件加载失败");
//            }
//            String cmdId = command.getCmdId();
//            //发送报文组
//            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
//            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
//            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
//            String result = parse.toString();
//            if ("false".equals(result)) {
//                return R.fail();
//            }
        R<DatagramSocket> ans = messageUtil.getSocket();
        DatagramSocket socket = ans.getData();
        if ("T600".equals(flag)) {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return R.fail("xml文件加载失败");
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);

            if ("InitLampMotor".equals(cmdId) || "InitSlit1Motor".equals(cmdId) || "InitWlMotor".equals(cmdId)) {

                if (responses.size() == 1) {
                    System.out.println(cmdId + " 仅收到1条回应，重发一次");
                    responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
                }
            }

            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            String result = parse.toString();
            if ("false".equals(result)) {
                return R.fail();
            }

        } else if(flag == "T800"){
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return R.fail("T800xml文件加载失败");
            }
            String cmdId = command.getCmdId();

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,t800xml.get(cmdId));
            t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
//            String result = t800MessageUtil.getResultSingle(cmdId, 3000);
            String result = ResponseParser.Result;

            if (result != null) {
                System.out.println("解析结果：" + result);
            } else {
                System.out.println("超时未收到解析结果");
            }
            if ("false".equals(result)) {
                return R.fail();
            }

        }
        return R.ok();
    }

    /**
     * 初始化之后，设置初始化状态
     */
    public R<String> setDeviceInitStatus(String deviceIp, Integer status) throws Exception {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();

        if(flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return R.fail("xml文件加载失败");
            }
            String cmdId = "SetInitStatus";
            Object[] param = new Object[]{status};

            //发送报文组
            Transition transition = xml.get(cmdId);
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            byte[] bytes = protocolItemUtil.calcSendBytes(param, transition);
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            String result = parse.toString();
            if ("false".equals(result)) {
                return R.fail();
            }
        } else if(flag == "T800"){
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return R.fail("T800xml文件加载失败");
            }
            String cmdId = "SetInitStatus";
            Object[] param = new Object[]{status};

            byte[] bytes = protocolItem.T800calcSendBytes(param ,t800xml.get(cmdId));
            t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
//            String result = t800MessageUtil.getResultSingle(cmdId, 3000);
            String result = ResponseParser.Result;

            if (result != null) {
                System.out.println("解析结果：" + result);
            } else {
                System.out.println("超时未收到解析结果");
            }
            if ("false".equals(result)) {
                return R.fail();
            }

        }
        return R.ok();
    }

    /**
     * 设置当前样品池位置
     * @param poolNum
     * @param deviceIp
     * @return
     */
    public R<String> setCurrentCellPos(Integer poolNum, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();

        Boolean b = changeXml(T600xmlPath);
        if (!b) {
            return R.fail("xml文件加载失败");
        }
        String cmdId = "SetCellPos";

        //发送报文组
        Transition transition = xml.get(cmdId);
        int timeout = transition.getTimeout();
        int replyNumber = transition.getReplyNumber();
        byte[] bytes = protocolItemUtil.calcSendBytes(new Object[]{poolNum}, transition);
        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
        Object parse = ResponseDeal.parse(cmdId, responses, bytes);
        String result = parse.toString();
        if ("false".equals(result)) {
            return R.fail();
        }

        return R.ok();
    }

    /**
     * 校零
     */
    public R<String> zeroCalibration(Command command) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();
        Boolean b = changeXml(T600xmlPath);
        if (!b) {
            return R.fail("转换xml出错");
        }
        String cmdId = command.getCmdId();
        Transition transition = xml.get(cmdId);
        //发送报文组
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        List<byte[]> responses = messageUtil.quickSendAndReceive(socket, defaultIp, cmdId, bytes, transition.getReplyNumber(), transition.getTimeout());
        Object parse = ResponseDeal.parse(cmdId, responses, bytes);
        if (parse.equals("false")) {
            return R.fail("校零失败");
        }
        return R.ok(parse.toString());
    }

    /**
     * 光谱扫描:设置参数
     */
    public R<String> spectrumScanSet(List<Command> list, String deviceIp) throws Exception {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();

        if(flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return R.fail("转换xml出错");
            }
            for (Command command : list) {
                String cmdId = command.getCmdId();
                //发送报文组
                Transition transition = xml.get(cmdId);
                int timeout = transition.getTimeout();
                int replyNumber = transition.getReplyNumber();
                byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
                List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
                Object parse = ResponseDeal.parse(cmdId, responses, bytes);
                    if (parse.equals("false")) {
                    return R.fail("解析报文出错");
                }
            }
        }

        else if(flag == "T800"){
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return R.fail("T800 xml 加载失败");
            }
            for (Command command : list) {
                String cmdId = command.getCmdId();
                //发送报文组
                byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,t800xml.get(cmdId));
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
                String result = ResponseParser.Result;
                if (result != null) {
                    System.out.println("解析结果：" + result);
                } else {
                    System.out.println("超时未收到解析结果");
                }
                if ("true".equals(result)) {
                    return R.ok();
                }
            }
        }
            return R.ok("true");
    }

    /**
     * 停止指令
     */
    public R<String> stop(Command cmd, String deviceIp) throws Exception {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();

        Boolean b = changeXml(T600xmlPath);
        if (!b) {
            return R.fail("转换xml出错");
        }
        String cmdId = cmd.getCmdId();
        //发送报文组
        Transition transition = xml.get(cmdId);
        int timeout = transition.getTimeout();
        int replyNumber = transition.getReplyNumber();
        byte[] bytes = protocolItemUtil.calcSendBytes(cmd.getParams(), transition);
        messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
        return R.ok();
    }

    /**
     * 光谱扫描:读取样品池类型
     */
    public Result spectrumScanReadSamplePoolType(Command command) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return Result.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();
        Boolean b = changeXml(T600xmlPath);
        if (!b) {
            return Result.fail("转换xml出错");
        }
        String cmdId = command.getCmdId();
        //发送报文组
        Transition transition = xml.get(cmdId);
        int timeout = transition.getTimeout();
        int replyNumber = transition.getReplyNumber();
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, defaultIp, cmdId, bytes, replyNumber, timeout);
        Object parse = ResponseDeal.parse(cmdId, responses, bytes);
        if (parse.equals("false")) {
            return Result.fail("解析报文出错");
        }
        return Result.ok(parse.toString());
    }

    /**
     * 光谱扫描:基线
     */
    public R<List<ChangeParam>> spectrumScanGetBaseline(Command command, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();

        if (flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return R.fail("转换xml出错");
            }
            String cmdId = command.getCmdId();
            //发送报文组
            Transition transition = xml.get(cmdId);
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse.equals("false")) {
                return R.fail("基线失败");
            }
            List<ChangeParam> changeParam = (List<ChangeParam>) parse;
            return R.ok(changeParam);
        }
        else if (flag == "T800") {
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return R.fail("T800 xml 加载失败");
            }
            String cmdId = command.getCmdId();

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams(), t800xml.get(cmdId));
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String param = ResponseParser.parseScan(bytes);
//            String json = JSON.toJSONString(param);
            System.out.println("解析结果 JSON：" + param);
        }
        return R.fail();
    }

    /**
     * 光谱扫描:基线(实时推送版)
     */
    public void spectrumScanGetBaselineRealTime(Command command, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return ;
        }
        DatagramSocket socket = re.getData();

        Boolean b = changeXml(T600xmlPath);
        if (!b) {
            log.info("转换xml出错");
            return;
        }
        String cmdId = command.getCmdId();
        Transition transition = xml.get(cmdId);
        int timeout = transition.getTimeout();
        int replyNumber = transition.getReplyNumber();
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        Constants.startTime = LocalDateTime.now();
        messageUtil.scanSendAndDealBaseline(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
    }

    /**
     * 定位波长
     */
    public Boolean wavelengthLocate(Command command, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return  false;
        }
        DatagramSocket socket = ans.getData();
        if(flag.equals("T600")) {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return false;
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            //发送报文组
            int timeout = transition.getTimeout();
            int replyNumber = transition.getReplyNumber();
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse.equals("false")) {
                return false;
            }
        }
        else if(flag.equals("T800")){
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return false;
            }
            String cmdId = command.getCmdId();

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,t800xml.get(cmdId));
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String result = ResponseParser.Result;

            if (result.equals("false")) {
                return false;
            }
        }
        return true;
    }

    /**
     * 光度测量：
     */
    public R<ChangeParam> photometricAndRationMeasurement(List<Command> list, String deviceIp) throws Exception {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(re.getMessage());
        }
        DatagramSocket socket = re.getData();
        ChangeParam changeParam = null;

        if(flag.equals("T600")) {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return R.fail("转换xml出错");
            }

            for (Command command : list) {
                String cmdId = command.getCmdId();
                Transition transition = xml.get(cmdId);
                int ReplyNumber = transition.getReplyNumber();
                int Timeout = transition.getTimeout();
                //发送报文组
                byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
                List<byte[]> responses = messageUtil.quickSendAndReceive(socket, deviceIp, cmdId, bytes, ReplyNumber, Timeout);
                Object parse = ResponseDeal.parse(cmdId, responses, bytes);

                if (parse == null || parse.equals("false")) {
                    return R.fail("解析报文出错");
                }
                if (cmdId.equals("Measure")) {
                    changeParam = (ChangeParam) parse;
                }
            }
        }
        else if(flag.equals("T800")) {
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return R.fail("转换xml出错");
            }
            for (Command command : list) {
                String cmdId = command.getCmdId();
                T800Transition transition = t800xml.get(cmdId);

                byte[] bytes = protocolItem.T800calcSendBytes(command.getParams(), transition);
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
                Object parse = ResponseParser.parseMeasure(bytes);
                if (parse.equals("false")) {
                    return R.fail("解析报文出错");
                }
                changeParam = (ChangeParam) parse;
            }
            return R.ok(changeParam);
        }

        return R.ok(changeParam);
    }

    /**
     * 定量测定(微分)：
     */
    public R<List<ChangeParam>> quantitativeDifferentialScan(List<Command> list, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(re.getMessage());
        }
        DatagramSocket socket = re.getData();
        List<ChangeParam> changeParams = null;

        if (flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return R.fail("转换xml出错");
            }

            for (Command command : list) {
                String cmdId = command.getCmdId();
                Transition transition = xml.get(cmdId);
                int ReplyNumber = transition.getReplyNumber();
                int Timeout = transition.getTimeout();

                //发送报文组
                byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
                List<byte[]> responses = messageUtil.quickSendAndReceive(socket, deviceIp, cmdId, bytes, ReplyNumber, Timeout);
                Object parse = ResponseDeal.parse(cmdId, responses, bytes);
                if (parse == null || parse.equals("false")) {
                    return R.fail("解析报文出错");
                }
                changeParams.add((ChangeParam) parse);
            }
        }
        else if (flag == "T800") {
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return R.fail("转换xml出错");
            }
            for (Command command : list) {
                String cmdId = command.getCmdId();

                T800Transition transition = t800xml.get(cmdId);

                byte[] bytes = protocolItem.T800calcSendBytes(command.getParams(), transition);
                try {
                    t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                Object parse = ResponseParser.parseMeasure(bytes);
                if (parse.equals("false")) {
                    return R.fail("解析报文出错");
                }
                changeParams.add((ChangeParam) parse);
            }
        }
        return R.ok(changeParams);
    }

    /**
     * 光谱扫描: 获取测量数据
     */
    public void getMeasureData(Command command, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return;
        }
        DatagramSocket socket = re.getData();

        if (flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                log.info("转换xml出错");
                return;
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int ReplyNumber = transition.getReplyNumber();
            int Timeout = transition.getTimeout();
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            Constants.startTime = LocalDateTime.now();
            messageUtil.scanSendAndDeal(socket, deviceIp, cmdId, bytes, ReplyNumber, Timeout);
        }

        else if (flag == "T800") {
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                log.info("转换xml出错");
                return;
            }
            String cmdId = command.getCmdId();

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams(), t800xml.get(cmdId));
            Constants.startTime = LocalDateTime.now();
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//                Object param = ResponseParser.parseNullReport(bytes);
//                String json = JSON.toJSONString(param);
//                System.out.println("解析结果 JSON：" + json);
        }
    }

    /**
     * 定量测定(微分): 获取测量数据
     */
    public List<ChangeParam> getQuantitativeDifferentialMeasureData(Command command, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return null;
        }
        DatagramSocket socket = re.getData();

        Boolean b = changeXml(T600xmlPath);
        if (!b) {
            log.info("转换xml出错");
            return null;
        }
        String cmdId = command.getCmdId();
        Transition transition = xml.get(cmdId);
        int ReplyNumber = transition.getReplyNumber();
        int Timeout = transition.getTimeout();
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        List<ChangeParam> changeParams = messageUtil.RationDifferentialCall(socket, deviceIp, cmdId, bytes, ReplyNumber, Timeout);
        if (changeParams == null) {
            log.info("获取数据失败");
            return null;
        }
        return changeParams;
    }


    /**
     * 转换xml为map
     * @return
     */
    private Boolean changeXml(String xmlPath) {
        // 判断xml是否为空
        if (xml!=null&&!xml.isEmpty()) {
            // 如果不为空，则返回true
            return true;
        }

        try {
            if(T600xmlPath.equals(xmlPath)){
                // 从xmlPath路径加载xml
                xml = protocolItemUtil.loadFromXml(xmlPath);
            }
        } catch (Exception e) {
            // 如果加载失败，则记录错误日志，并返回false
            log.error("转换xml出错");
            return false;
        }
        // 如果加载成功，则返回true
        return true;
    }

    private Boolean T800changeXml(String xmlPath) {
        // 判断xml是否为空
        if (t800xml!=null&&!t800xml.isEmpty()) {
            // 如果不为空，则返回true
            return true;
        }

        try {
            if(T800xmlPath.equals(xmlPath)){
                // 从xmlPath路径加载xml
                t800xml = protocolItem.loadFormXml(xmlPath);
            }
        } catch (Exception e) {
            // 如果加载失败，则记录错误日志，并返回false
            log.error("转换xml出错");
            return false;
        }
        // 如果加载成功，则返回true
        return true;
    }



    /**
     * 光度测量：校零
     */
    public R<ChangeParam> zero(Command command, String deviceIp) throws Exception {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(re.getMessage());
        }
        DatagramSocket socket = re.getData();

        if (flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return R.fail("转换xml出错");
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int ReplyNumber = transition.getReplyNumber();
            int Timeout = transition.getTimeout();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, ReplyNumber, Timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse.equals("false")) {
                return R.fail("校零失败");
            }
            ChangeParam changeParam = (ChangeParam) parse;
            return R.ok(changeParam);
        }
        else if (flag == "T800") {
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return R.fail("转换xml出错");
            }
            String cmdId = command.getCmdId();
            T800Transition transition = t800xml.get(cmdId);

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,transition);
            t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            Object parse = ResponseParser.parseMeasure( bytes);
            if (parse.equals("false")) {
                return R.fail("解析报文出错");
            }
            ChangeParam changeParam = (ChangeParam) parse;
            return R.ok(changeParam);
            //需要在t800解析中重写一下
//            ChangeParam changeParam = (ChangeParam) parse;
//            return R.ok(changeParam);
        }
        return R.ok();
    }

    /**
     * 时间扫描：设置参数
     * @param list
     * @param deviceIp
     */
    public R<String> timeSweepSet(List<Command> list, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail("false");
        }
        DatagramSocket socket = ans.getData();
        Boolean b = changeXml(T600xmlPath);
        if (!b) {
            return R.fail("false");
        }
        for (Command command : list) {
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int replyNumber = transition.getReplyNumber();
            int timeout = transition.getTimeout();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(),transition);
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, replyNumber, timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse.equals("false")) {
                return R.fail("false");
            }
        }
        return R.ok("true");
    }

    /**
     * 时间扫描: 定位波长
     */
    public void timeWavelengthLocate(Command command, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return;
        }
        DatagramSocket socket = ans.getData();

        if (flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                return;
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int ReplyNumber = transition.getReplyNumber();
            int Timeout = transition.getTimeout();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes, ReplyNumber, Timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse.equals("false")) {
                return;
            }
        }
        else if (flag == "T800") {
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                return;
            }
            String cmdId = command.getCmdId();
            T800Transition transition = t800xml.get(cmdId);

            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,transition);
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Object parse = ResponseParser.Result;
            if (parse.equals("false")) {
                return;
            }
        }

    }

    /**
     * 时间扫描：获取测量数据
     */
    public ChangeParam timeSweepGetMeasureData(Command command, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return null;
        }
        DatagramSocket socket = re.getData();

        if (flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                log.info("转换xml出错");
                return null;
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            int ReplyNumber = transition.getReplyNumber();
            int Timeout = transition.getTimeout();
            Constants.startTime=LocalDateTime.now();
            // 发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.singleSendAndReceive2(socket, deviceIp, cmdId, bytes, ReplyNumber, Timeout);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse==null || parse.equals("false")) {
                return null;
            }
            ChangeParam changeParam = (ChangeParam) parse;
            Constants.timeChangeparam=changeParam;
            return changeParam;
        }

        else if (flag == "T800") {
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                log.info("转换xml出错");
                return null;
            }
            String cmdId = command.getCmdId();
            T800Transition transition = t800xml.get(cmdId);

            // 获取当前时间作为起始时间
            Constants.startTime=LocalDateTime.now();
            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,transition);
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Object parse = ResponseParser.parseMeasure( bytes);
            if (parse==null || parse.equals("false")) {
                return null;
            }
            ChangeParam changeParam = (ChangeParam) parse;
            Constants.timeChangeparam=changeParam;
            return changeParam;
        }
        return null;
    }


    /**
     * 时间扫描： 发送
     */
    public void timeSweepSend(Command command, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return ;
        }
        DatagramSocket socket = re.getData();

        if (flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                log.info("转换xml出错");
                return ;
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            Constants.startTime=LocalDateTime.now();
            // 发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            messageUtil.singleSend(socket, deviceIp, cmdId, bytes);
        }

        else if (flag == "T800") {
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                log.info("转换xml出错");
                return;
            }
            String cmdId = command.getCmdId();
            T800Transition transition = t800xml.get(cmdId);

            // 获取当前时间作为起始时间
            Constants.startTime=LocalDateTime.now();
            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,transition);
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * 时间扫描：接收
     */
    public ChangeParam timeSweepReceive(Command command, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return null;
        }
        DatagramSocket socket = re.getData();

        if (flag == "T600") {
            Boolean b = changeXml(T600xmlPath);
            if (!b) {
                log.info("转换xml出错");
                return null;
            }
            String cmdId = command.getCmdId();
            Transition transition = xml.get(cmdId);
            Constants.startTime=LocalDateTime.now();
            // 接收报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
            List<byte[]> responses = messageUtil.singleReceive(socket, deviceIp, cmdId);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse==null || parse.equals("false")) {
                return null;
            }
            ChangeParam changeParam = (ChangeParam) parse;
            Constants.timeChangeparam=changeParam;
            return changeParam;
        }

        else if (flag == "T800") {
            Boolean b = T800changeXml(T800xmlPath);
            if (!b) {
                log.info("转换xml出错");
                return null;
            }
            String cmdId = command.getCmdId();
            T800Transition transition = t800xml.get(cmdId);

            // 获取当前时间作为起始时间
            Constants.startTime=LocalDateTime.now();
            byte[] bytes = protocolItem.T800calcSendBytes(command.getParams() ,transition);
            try {
                t800MessageUtil.sendCommand(cmdId, bytes, T800defaultIp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Object parse = ResponseParser.parseMeasure( bytes);
            if (parse==null || parse.equals("false")) {
                return null;
            }
            ChangeParam changeParam = (ChangeParam) parse;
            Constants.timeChangeparam=changeParam;
            return changeParam;
        }
        return null;
    }




    /**
     * 判断通信版本
     */
    public static boolean sendBroadcastAndWaitReply(DatagramSocket socket, byte[] data, int port, int timeoutMs) {
        try {
            // 启用广播
            socket.setBroadcast(true);

            // 广播地址
            InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");

            // 组装报文
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddr, port);

            // 发送
            socket.send(packet);
            System.out.println("广播报文已发送");

            // 设置超时（毫秒）
            socket.setSoTimeout(timeoutMs);

            // 等待接收
            byte[] buffer = new byte[4096];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(reply);  // 阻塞直到收到或超时
                byte[] recvBytes = Arrays.copyOf(reply.getData(), reply.getLength());

                System.out.print("收到回复：");
                for (byte b : recvBytes) System.out.printf("%02X ", b);
                System.out.println();

                // 固定报文（T800 特征报文）
                byte[] t800Signature = new byte[]{
                        (byte)0xFF, (byte)0xB5, 0x00, 0x00, 0x00, 0x14, 0x00, 0x28,
                        0x14, (byte)0xFC, 0x00, 0x1B, 0x00, 0x10, 0x21, 0x01,
                        0x01, 0x00, 0x00, 0x30, (byte)0xE0, (byte)0xA6, 0x66, (byte)0x89,
                        0x5A, 0x13, (byte)0x88, (byte)0xC0, (byte)0xA8, 0x01, 0x64, (byte)0xFF,
                        (byte)0xFF, (byte)0xFF, 0x00, (byte)0xC0, (byte)0xA8, 0x01, 0x01, 0x0A, 0x26
                };

                // 比较
                if (Arrays.equals(recvBytes, t800Signature)) {
                    System.out.println("识别到 T800");
                    return true; // true = T800
                } else {
                    System.out.println("收到非 T800 报文，判定为 T600");
                    return false; // false = T600
                }

            } catch (SocketTimeoutException e) {
                System.out.println("超时未收到回复，判定为 T600");
                return false; // 没回复也算 T600
//                return true;// T800
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false; // 出异常也算 T600
        }
    }


    // 确保已加载
    private boolean ensureT800ProtocolsLoaded() {
        if (t800xml != null && !t800xml.isEmpty()) return true;
        try {
            t800xml = protocolItem.loadFormXml(T800xmlPath); // 用 ProtocolItem 的方法加载
            return t800xml != null && !t800xml.isEmpty();
        } catch (Exception e) {
            log.error("加载 T800 协议失败", e);
            return false;
        }
    }

}
