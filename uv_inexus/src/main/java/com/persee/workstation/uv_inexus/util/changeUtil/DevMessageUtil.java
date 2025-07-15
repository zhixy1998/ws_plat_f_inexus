package com.persee.workstation.uv_inexus.util.changeUtil;

import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.instrumentVo.FindDeviceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("DevMessageUtil")
@Slf4j
public class DevMessageUtil {
    @Autowired
    MessageUtil messageUtil;

    @Autowired
    ProtocolItemUtil protocolItemUtil;

    @Value("${device.default.xmlPath}")
    String xmlPath;

    private volatile static String defaultIp = "255.255.255.255";
    private volatile static Map<String, Transition> xml;

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
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, defaultIp, cmdId, bytes);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);

            if (parse == null) {
                return R.fail("接收报文解析异常");
            }
            if (cmdId.equals(FindDevEnum.NewIP.getComId())) {
                findDeviceVO.setIp(parse.toString());
                defaultIp = parse.toString();
                continue;
            }

            if (cmdId.equals(FindDevEnum.GetSn.getComId())) {
                findDeviceVO.setDeviceSerialNumber(parse.toString());
                continue;
            }
            if (cmdId.equals(FindDevEnum.GetInitStatus.getComId())) {
                findDeviceVO.setIsInited((Integer) parse);

            }
        }
        return R.ok(findDeviceVO);
    }

    /**
     * 初始化设备，暂放
     *
     * @param list
     * @return
     */
    public R<String> initDevice(List<Command> list) {
        return null;
    }

    /**
     * 光度测量：
     */
    public R<Map<String, String>> photoMetricMeasurement(List<Command> list, String deviceIp) {
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
        Map<String, String> s = new HashMap<>();
        for (Command command : list) {
            String cmdId = command.getCmdId();
            //发送报文组
            byte[] bytes = protocolItemUtil.calcSendBytes(command.getParams(), xml.get(cmdId));
            List<byte[]> responses = messageUtil.singleSendAndReceive(socket, deviceIp, cmdId, bytes);
            Object parse = ResponseDeal.parse(cmdId, responses, bytes);
            if (parse == null) {
                return R.fail("解析报文出错");
            }
            if (cmdId.equals("Measure")) {
                s = (Map<String, String>) parse;
            }
        }
        return R.ok(s);
    }

    private Boolean changeXml() {
        if (!xml.isEmpty()) {
            return true;
        }

        try {
            xml = protocolItemUtil.loadFromXml(xmlPath);
        } catch (Exception e) {
            log.error("转换xml出错");
            return false;
        }
        return true;
    }
}
