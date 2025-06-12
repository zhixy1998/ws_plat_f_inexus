package com.persee.workstation.uv_inexus.util;

public class ByteUse {
    public final static byte[][] SearchMachineSend = {
            {(byte) 0xB5 ,(byte) 0xFE, (byte) 0xF1 ,(byte) 0x03 ,(byte) 0x01 ,(byte) 0x40 ,(byte) 0xC2 ,(byte) 0xBA ,(byte) 0xA9 ,(byte) 0x08 ,(byte) 0x64 , (byte)0x78, 2},
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
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xA5,2}
    };
    public final static byte[][] LinkMachineReceive = {
            {   (byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xF0, (byte) 0x01,
                    (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0xA4}
    };
    public final static byte[][] ReadSerialnumberSend ={
            {    (byte) 0xB5, (byte) 0xFF, (byte) 0x13, (byte) 0xFD,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC4,4}
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
            {       (byte) 0xB5, (byte) 0xFF, (byte) 0x05, (byte) 0x12,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xCB,2
            }
    };
    public final static byte[][] ReadSamplepoolReceive = {
            { (byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x05, (byte) 0x12,
                    (byte) 0x15, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE0}
    };

    public final static byte[][] InitStatusSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x01, (byte) 0x0B,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC0,2}
    };

    public final static byte[][] InitStatusReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x01, (byte) 0x0B,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC0}
    };

    public final static byte [][]InitSamplepoolSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x0D,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC1,2}
    };

    public final static byte [][]InitSamplepoolReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x00, (byte) 0x0D,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC1}
    };

    public final static byte [][]SamplepoolLocationSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x03,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBD,2}
    };

    public final static byte [][]SamplepoolLocationReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            { (byte) 0xFF,(byte) 0xB5, (byte) 0x06, (byte) 0x03,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBD}
    };

    public final static byte [][]InitColorFilterSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x03,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB7,2}
    };

    public final static byte [][]InitColorFilterReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF,  (byte) 0xB5,  (byte) 0x00, (byte) 0x03,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB7}
    };

    public final static byte [][] InitSlitMotorSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x04,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB8,2}
    };

    public final static byte [][] InitSlitMotorReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            { (byte) 0xFF,(byte) 0xB5, (byte) 0x00, (byte) 0x04,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB8}
    };

    public final static byte [][] SetSlitWidthSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x06, (byte) 0x02,
                    (byte) 0x14, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xD0,2}
    };

    public final static byte [][] SetSlitWidthReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x06, (byte) 0x02,
                    (byte) 0x14, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xD0}
    };

    public final static byte [][] InitWavelengthSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x05,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB9,2}
    };

    public final static byte [][] InitWavelengthReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            {(byte) 0xFF, (byte) 0xB5, (byte) 0x00, (byte) 0x05,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB9}
    };

    public final static byte [][] InitLampMotorSend = {
            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x06,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA,2}
    };

    public final static byte [][] InitLampMotorReceive = {
            {(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            { (byte) 0xFF,(byte) 0xB5, (byte) 0x00, (byte) 0x06,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA}
    };

//    byte [][] CheckWuPowerSend = {
//            {(byte) 0xB5, (byte) 0xFF, (byte) 0x00, (byte) 0x0B,
//                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBF}
//    };
//
//    byte [][] CheckWuPowerReceive = {
//
//    };
}

