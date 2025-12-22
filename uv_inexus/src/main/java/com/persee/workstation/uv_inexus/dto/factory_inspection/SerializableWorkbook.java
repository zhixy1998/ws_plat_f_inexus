package com.persee.workstation.uv_inexus.dto.factory_inspection;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;

public class SerializableWorkbook implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient Workbook workbook;
    private byte[] workbookData;

    public SerializableWorkbook(Workbook workbook) {
        this.workbook = workbook;
        this.workbookData = workbookToBytes(workbook);
    }

    public Workbook getWorkbook() {
        if (workbook == null && workbookData != null) {
            workbook = bytesToWorkbook(workbookData);
        }
        return workbook;
    }

    private byte[] workbookToBytes(Workbook workbook) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BufferedOutputStream bos = new BufferedOutputStream(baos)) {
            workbook.write(bos);
            bos.flush(); // 确保缓冲区刷新
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize workbook", e);
        }
    }

    private Workbook bytesToWorkbook(byte[] data) {
        if (data == null || data.length == 0) {
            throw new RuntimeException("Workbook data is null or empty");
        }

        // 验证最小XLSX文件大小（至少应包含基本结构）
        if (data.length < 200) {
            throw new RuntimeException("Workbook data too small to be valid XLSX. Size: " + data.length + " bytes");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            // 先验证ZIP文件头
            if (data[0] != 0x50 || data[1] != 0x4B) {
                throw new RuntimeException("Invalid XLSX file header. Not a ZIP file.");
            }

            OPCPackage pkg = OPCPackage.open(bais);
            return new XSSFWorkbook(pkg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize workbook. Data length: " + data.length, e);
        }
    }


}
