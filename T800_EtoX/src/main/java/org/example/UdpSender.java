package org.example;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


import java.net.*;
import java.util.Map;

import static java.lang.Thread.sleep;

public class UdpSender {
    private static final String LOCAL_IP = "192.168.51.36";  // 上位机 IP
    private static final int LOCAL_PORT = 6000;              // 上位机端口
    private static final int REMOTE_PORT = 5000;             // 下位机端口

    public static void main(String[] args) {
        try (DynamicCommandService service =
                     new DynamicCommandService(LOCAL_IP, LOCAL_PORT, REMOTE_PORT, "D:\\project\\java\\T800新.xml")) {

            String[] commandIds = {"Measure","SetCellPos"};
            Object[][] paramsArray = {new Object[]{0},new Object[]{2}};

            for (int i = 0; i < commandIds.length; i++) {
                sleep(5000);
                String cmdId = commandIds[i];
                Object[] params = paramsArray[i];
                service.executeCommand(cmdId, params);
            }

            // 阻塞主线程，不让 try 提前结束
            Thread.currentThread().join();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
