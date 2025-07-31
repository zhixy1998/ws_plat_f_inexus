package com.persee.workstation.uv_inexus.util.changeUtil;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.instrumentVo.FindDeviceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramSocket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service("DevMessageUtil")
@Slf4j
public class DevMessageUtil {
    @Autowired
    MessageUtil messageUtil;

    @Autowired
    ProtocolItemUtil protocolItemUtil;

//    @Autowired
//    private ExperimentTestService experimentService;

    @Value("${device.default.xmlPath}")
    String xmlPath;

//    String xmlPath = "E:\\product\\ws_plat_inexus\\uv_inexus\\src\\main\\java\\com\\persee\\workstation\\uv_inexus\\util\\changeUtil\\T600.1.xml" ;

    private volatile static String defaultIp = "255.255.255.255";
//    private volatile static String defaultIp = "192.168.32.254";
    private volatile  Map<String, Transition> xml;

    private static String ipEnd = "253";

    /**
     * 连接设备
     * @param list
     * @return
     */
    public R<FindDeviceVO> connectDevice(List<Command> list) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();

        Boolean b = changeXml();
        if (!b) {
            return R.fail("xml文件加载失败");
        }
        FindDeviceVO findDeviceVO = new FindDeviceVO();

        for (Command command : list) {
            String cmdId = command.getCmdId();
            if (cmdId.equals(FindDevEnum.NewIP.getComId())) {
                Object[] param = new Object[]{64, 194, 186, 169, 8, 100, 52, 221, 166, 101, 166, 103, 169, 254, 136, 253};
//                Object[] param = new Object[]{0, 0, 0, 0, 0, 0, 52, 218, 166, 101, 144, 86, 169, 254, 136, 253};
                String address = socket.getLocalAddress().getHostAddress();
                Object[] split = address.split("\\.");
                int length = param.length;
                for (int i = 2; i >= 0; i--) {
                    param[length - i - 2] = split[2-i];
                }
                command.setParams(param);
            }

            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
            List<byte[]> responses = messageUtil.quickSendAndReceive(socket, defaultIp, cmdId, bytes);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);

            if (parse == null) {
                return R.fail("接收报文解析异常");
            }
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
                    findDeviceVO.setIsInited(2);
                }


            }
        }
        return R.ok(findDeviceVO);
    }

    /**
     * 初始化设备，暂放
     *
     * @param
     * @return
     */
    public R<String> initDevice(Command  command, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();

        Boolean b = changeXml();
        if (!b) {
            return R.fail("xml文件加载失败");
        }
        String cmdId = command.getCmdId();
        //发送报文组
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
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
        Boolean b = changeXml();
        if (!b) {
            return R.fail("     转换xml出错");
        }
        String cmdId = command.getCmdId();
        Transition transition = xml.get(cmdId);
        //发送报文组
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        List<byte[]> responses = messageUtil.quickSendAndReceive(socket, defaultIp, cmdId, bytes);
        Object parse = ResponseDeal.parse(cmdId, responses, bytes);
        if (parse.equals("false")) {
            return R.fail("校零失败");
        }
        return R.ok(parse.toString());
    }

    /**
     * 光谱扫描:设置参数
     */
    public R<String> spectrumScanSet(List<Command> list, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(ans.getMessage());
        }
        DatagramSocket socket = ans.getData();
        Boolean b = changeXml();
        if (!b) {
            return R.fail("转换xml出错");
        }
        for (Command command : list) {
            String cmdId = command.getCmdId();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse.equals("false")) {
                return R.fail("解析报文出错");
            }
        }
        return R.ok("true");
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
        Boolean b = changeXml();
        if (!b) {
            return Result.fail("转换xml出错");
        }
        String cmdId = command.getCmdId();
        //发送报文组
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, defaultIp, cmdId, bytes);
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
        Boolean b = changeXml();
        if (!b) {
            return R.fail("转换xml出错");
        }
        String cmdId = command.getCmdId();
        //发送报文组
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
        Object parse = ResponseDeal.parse(cmdId, responses, bytes);
        if (parse.equals("false")) {
            return R.fail("基线失败");
        }
        List<ChangeParam> changeParam = (List<ChangeParam>) parse;
        return R.ok(changeParam);
    }

    /**
     * 定位波长
     */
    public void wavelengthLocate(Command command, String deviceIp) {
        R<DatagramSocket> ans = messageUtil.getSocket();
        if (!ans.getSuccess()) {
            log.info("绑定socket失败");
            return;
        }
        DatagramSocket socket = ans.getData();
        Boolean b = changeXml();
        if (!b) {
            return;
        }
        String cmdId = command.getCmdId();
        Transition transition = xml.get(cmdId);
        //发送报文组
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
        Object parse = ResponseDeal.parse(cmdId, responses, bytes);
        if (parse.equals("false")) {
            return;
        }
    }

    /**
     * 光度测量：
     */
    public R<ChangeParam> photoMetricMeasurement(List<Command> list, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(re.getMessage());
        }
        DatagramSocket socket = re.getData();

        Boolean b = changeXml();
        if (!b) {
            return R.fail("转换xml出错");
        }
        ChangeParam changeParam = null;
        for (Command command : list) {
            String cmdId = command.getCmdId();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
            List<byte[]> responses = messageUtil.quickSendAndReceive(socket, deviceIp, cmdId, bytes);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse == null || parse.equals("false")) {
                return R.fail("解析报文出错");
            }
            if (cmdId.equals("Measure")) {
                changeParam = (ChangeParam) parse;
            }
        }
        return R.ok(changeParam);
    }

    /**
     * 光谱扫描: 获取测量数据
     */
    public void getMeasureData(Command command, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return ;
        }
        DatagramSocket socket = re.getData();

        Boolean b = changeXml();
        if (!b) {
            log.info("转换xml出错");
            return;
        }
        String cmdId = command.getCmdId();
        Transition transition = xml.get(cmdId);
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        Constants.startTime = LocalDateTime.now();
        messageUtil.scanSendAndDeal(socket, deviceIp, cmdId, bytes);
    }


    /**
     * 转换xml为map
     * @return
     */
    private Boolean changeXml() {
        // 判断xml是否为空
        if (xml!=null&&!xml.isEmpty()) {
            // 如果不为空，则返回true
            return true;
        }

        try {
            // 从xmlPath路径加载xml
            xml = protocolItemUtil.loadFromXml(xmlPath);
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
    public R<ChangeParam> zero(Command command, String deviceIp) {
        R<DatagramSocket> re = messageUtil.getSocket();
        if (!re.getSuccess()) {
            log.info("绑定socket失败");
            return R.fail(re.getMessage());
        }
        DatagramSocket socket = re.getData();
        Boolean b = changeXml();
        if (!b) {
            return R.fail("转换xml出错");
        }
        String cmdId = command.getCmdId();
        Transition transition = xml.get(cmdId);
        //发送报文组
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
        Object parse = ResponseDeal.parse(cmdId, responses, bytes);
        if (parse.equals("false")) {
            return R.fail("校零失败");
        }
        ChangeParam changeParam = (ChangeParam) parse;
        return R.ok(changeParam);
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
        Boolean b = changeXml();
        if (!b) {
            return R.fail("false");
        }
        for (Command command : list) {
            String cmdId = command.getCmdId();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
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
        Boolean b = changeXml();
        if (!b) {
            return;
        }
        String cmdId = command.getCmdId();
        Transition transition = xml.get(cmdId);
        //发送报文组
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
        Object parse = ResponseDeal.parse(cmdId, responses, bytes);
        if (parse.equals("false")) {
            return;
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

        Boolean b = changeXml();
        if (!b) {
            log.info("转换xml出错");
            return null;
        }
        String cmdId = command.getCmdId();
        Transition transition = xml.get(cmdId);
        // 发送报文组
        byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), transition);
        List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
        Object parse = ResponseDeal.parse(cmdId, responses, bytes);
        if (parse==null || parse.equals("false")) {
            return null;
        }
        ChangeParam changeParam = (ChangeParam) parse;
        return changeParam;
    }
}
