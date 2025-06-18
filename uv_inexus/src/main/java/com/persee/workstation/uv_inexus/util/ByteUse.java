package com.persee.workstation.uv_inexus.util;

public class ByteUse {

    public final static Integer first = 1;

    public final static byte[][] SearchMachineSend = {
            {(byte) 0xB5, (byte) 0xFE, (byte) 0xF1, (byte) 0x03, (byte) 0x00, (byte) 0x40, (byte) 0xC2, (byte) 0xBA, (byte) 0xA9, (byte) 0x08, (byte) 0x64, (byte) 0x78, 2},
    };
    public final static byte[][] SearchMachineReceive = {
            {(byte) 0xFE, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB9},
            {(byte) 0xFE, (byte) 0xB5, (byte) 0xF1, (byte) 0x03,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0xFF, (byte) 0x34, (byte) 0xDA, (byte) 0xA6,
                    (byte) 0x65, (byte) 0x90, (byte) 0x56, (byte) 0xA9,
                    (byte) 0xFE, (byte) 0x6E, (byte) 0x44, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0xA9,
                    (byte) 0xFE, (byte) 0x6E, (byte) 0x01, (byte) 0xF8,
                    (byte) 0xA9, (byte) 0x63, (byte) 0x3E, (byte) 0x06,
                    (byte) 0x62, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xB3}
    };
    public final static byte[][] LinkMachineSend = {{(byte) 0xB5, (byte) 0xFF, (byte) 0xF0, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,

            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xA5, 2}
    };
    public final static byte[][] LinkMachineReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xF0, (byte) 0x01,
                    (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0xA4}
    };
    public final static byte[][] ReadSerialnumberSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x13, (byte) 0xFD,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC4, 4}
    };
    public final static byte[][] ReadSerialnumberReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x13, (byte) 0xFD,
                    (byte) 0x33, (byte) 0x30, (byte) 0x55, (byte) 0x31,
                    (byte) 0x36, (byte) 0x30, (byte) 0x34, (byte) 0x47},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x13, (byte) 0xFD,
                    (byte) 0x2D, (byte) 0x30, (byte) 0x31, (byte) 0x2D,
                    (byte) 0x30, (byte) 0x30, (byte) 0x31, (byte) 0x10},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x13, (byte) 0xFD,
                    (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xF4}
    };
    public final static byte[][] ReadSamplepoolSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x05, (byte) 0x12,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xCB, 2
            }
    };
    public final static byte[][] ReadSamplepoolReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x05, (byte) 0x12,
                    (byte) 0x15, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE0}
    };

    public final static byte[][] InitStatusSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x01, (byte) 0x0B,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC0, 2}
    };

    public final static byte[][] InitStatusReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x01, (byte) 0x0B,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC0}
    };

    public final static byte[][] InitSamplepoolSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x0D,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC1, 2}
    };

    public final static byte[][] InitSamplepoolReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x00, (byte) 0x0D,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC1}
    };

    public final static byte[][] SamplepoolLocationSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x03,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBD, 2}
    };

    public final static byte[][] SamplepoolLocationReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x06, (byte) 0x03,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBD}
    };

    public final static byte[][] InitColorFilterSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x03,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB7, 2}
    };

    public final static byte[][] InitColorFilterReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x00, (byte) 0x03,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB7}
    };

    public final static byte[][] InitSlitMotorSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x04,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB8, 2}
    };

    public final static byte[][] InitSlitMotorReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x00, (byte) 0x04,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB8}
    };

    public final static byte[][] SetSlitWidthSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x02,
                    (byte) 0x14, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xD0, 2}
    };

    public final static byte[][] SetSlitWidthReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x06, (byte) 0x02,
                    (byte) 0x14, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xD0}
    };

    public final static byte[][] InitWavelengthSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x05,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB9, 2}
    };

    public final static byte[][] InitWavelengthReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x00, (byte) 0x05,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB9}
    };

    public final static byte[][] InitLampMotorSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x06,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA, 2}
    };

    public final static byte[][] InitLampMotorReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x00, (byte) 0x06,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA}
    };

    public final static byte[][] SpectrumMeasurementSend = {
            {(byte) 0xb5, (byte) 0xff, 0x13, 0x05,
                    0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, (byte) 0xcc},

    };
    public final static byte[][] SpectrumMeasurementReceive = {
            {(byte) 0xff, (byte) 0xb5, (byte) 0xff, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xba},

    };

    public final static byte[][] WuDengPowerSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x0B,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBF, 0}
    };

    public final static byte[][] DaoDengPowerSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x0C,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC0, 0}
    };

    public final static byte[][] CheckWavelengthSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x0A,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBE, 0}
    };

    //光谱扫描参数设置
    //数组参数规则调整：最后一位是case类型，倒数第二位是接收几条指令
    public final static byte[][] SpectrumScanAllConfig = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x06,
                    (byte) 0x19, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xD9, 2,1},

            {(byte) 0xB5, (byte) 0xFF, (byte) 0x0A, (byte) 0x02,
                    (byte) 0x9C, (byte) 0x72, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xCE, 2,2},

            {(byte) 0xB5, (byte) 0xFF, (byte) 0x0A, (byte) 0x03,
                    (byte) 0x4A, (byte) 0x06, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x11, 2,3},

            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x0c,
                    (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc7, 2,4},

            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x05,
                    (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc2, 2,5},

            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x04,
                    (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBF, 2,6},

            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x07,
                    (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc4, 2,7},
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x07,
                    (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc4, 2,8}
    };

    //设置扫描间隔（0.5）
    public final static byte[][] ScanTimeSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x06,
                    (byte) 0x19, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xD9, 2}
    };
    byte[][] ScanTimeReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x06, (byte) 0x06,
                    (byte) 0x19, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xD9}

            //扫描时间 = data[0][4] / 50
    };

    //设置起始波长(680-640)
    public final static byte[][] StartWavelengthSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x0A, (byte) 0x02,
                    (byte) 0x84, (byte) 0xE9, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2D, 2}
    };
    byte[][] StartWavelengthReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x0A, (byte) 0x02,
                    (byte) 0x84, (byte) 0xE9, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2D}

    };

    //设置结束波长   640
    public final static byte[][] EndWavelengthSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x0A, (byte) 0x03,
                    (byte) 0x7C, (byte) 0xE7, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x24, 2}
    };
    byte[][] EndWavelengthReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x0A, (byte) 0x02,
                    (byte) 0x7C, (byte) 0xE7, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x24}

    };

    //设置扫描速度(中)
    public final static byte[][] SetScanSpeedSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x0c,
                    (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc7, 2}
    };
    byte[][] SetScanSpeedReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x06, (byte) 0x0c,
                    (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC7}
    };

    //设置光度模式(ES)
    public final static byte[][] SetPhotoModeSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x05,
                    (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc2, 2}
    };
    byte[][] SetPhotoModeReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x06, (byte) 0x05,
                    (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC2}
    };

    //设置灯位置(氘灯)
    public final static byte[][] SetLampSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x04,
                    (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBF, 2}
    };
    byte[][] SetLampReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x06, (byte) 0x04,
                    (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBF}
    };

    //设置增益（增益4）
    public final static byte[][] SetGainSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x07,
                    (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc4, 2}
    };
    byte[][] SetGainReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x06, (byte) 0x07,
                    (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc4}
    };


    //未知但是需要有
    public final static byte[][] Send = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x07,
                    (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc4, 2}
    };
    byte[][] Receive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x06, (byte) 0x07,
                    (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xc4}
    };

    //开始扫描
    public final static byte[][] StartScanSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x13, (byte) 0x01,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC8}
    };


}

