package org.example;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.aspectj.weaver.ast.Or;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EtoX {

    private static FormulaEvaluator evaluator;

    public static void main(String[] args) throws Exception {
        String excelPath = "C:\\Users\\wangwenbo\\Desktop\\T800.xlsx";
        String outputXml = "D:\\project\\java\\T800.xml";

        FileInputStream fis = new FileInputStream(excelPath);
        Workbook workbook = new XSSFWorkbook(fis);
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        Sheet sheet = workbook.getSheetAt(0);

        Map<String, List<Row>> commandGroups = new LinkedHashMap<>();
        String currentId = null;
        List<Row> currentGroup = null;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String id = getCellString(row, 1);
            if (!id.isEmpty()) {
                if (currentId != null && currentGroup != null) {
                    commandGroups.put(currentId, currentGroup);
                }
                currentId = id;
                currentGroup = new ArrayList<>();
            }
            if (currentGroup != null && isRowContentValid(row)) {
                currentGroup.add(row);
            }
        }
        if (currentId != null && currentGroup != null) {
            commandGroups.put(currentId, currentGroup);
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();
        Element root = doc.createElement("Protocols");
        doc.appendChild(root);

        for (Map.Entry<String, List<Row>> entry : commandGroups.entrySet()) {
            List<Row> rows = entry.getValue();
            Row firstRow = rows.get(0);

            Element item = doc.createElement("ProtocolItem");
            appendElement(doc, item, "ID", getCellString(firstRow, 1));
            appendElement(doc, item, "Describe", getCellString(firstRow, 2));

            //InputString
            Element inputString = doc.createElement("InputString");
            boolean hasValidInput = false;
            for (Row row : rows) {
                String desc = getCellString(row, 3); // 第8列
                Cell cell = row.getCell(4); // 第9列
                String value = getCellString(row, 5); // 第10列

                if (cell != null && !desc.isEmpty()) {
                    String cellAddress = getExcelColumnName(cell.getColumnIndex()) + (cell.getRowIndex() + 1);
                    appendElement(doc, inputString, "string", desc + ":" + cellAddress + ":" + value);
                    hasValidInput = true;
                }
            }
            if (!hasValidInput) {
                appendElement(doc, inputString, "string", "");
            }
            item.appendChild(inputString);


            // SendBytes
            Element sendBytes = doc.createElement("SendBytes");
            List<String> sendList = new ArrayList<>();
            int checksum = 0;

            for (Row row : rows) {
                for (int i = 6; i <= 22; i++) {
                    String val = getCellString(row, i).trim();
                    if (!val.isEmpty() && !val.equalsIgnoreCase("X")) {
                        sendList.add(val);
                    }
                }
            }

            appendElement(doc, sendBytes, "string", String.join(":", sendList));

            item.appendChild(sendBytes);
            root.appendChild(item);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(new DOMSource(doc), new StreamResult(new File(outputXml)));

        workbook.close();
        fis.close();
        System.out.println("生成成功 → " + outputXml);
    }

    public static String getExcelColName(int col) {
        StringBuilder colName = new StringBuilder();
        int temp = col + 1;  // 1-based

        while (temp > 0) {
            int rem = (temp - 1) % 26;
            colName.insert(0, (char)(rem + 'A'));
            temp = (temp - 1) / 26;
        }
        return colName.toString();
    }


    public static String getExcelColumnName(int colIndex) {
        StringBuilder columnName = new StringBuilder();
        while (colIndex >= 0) {
            int rem = colIndex % 26;
            columnName.insert(0, (char) ('A' + rem));
            colIndex = (colIndex / 26) - 1;
        }
        return columnName.toString();
    }

    private static String getCellString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        return getCellString(cell);
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double val = cell.getNumericCellValue();
                return (val == (long) val) ? String.valueOf((long) val) : String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    String formula = cell.getCellFormula().trim();
                    if (formula.startsWith("=")) formula = formula.substring(1);
                    return formula;
                } catch (Exception e) {
                    return "";
                }
            default:
                return "";
        }
    }

    private static boolean isRowContentValid(Row row) {
        if (row == null) return false;
        String desc = getCellString(row, 7);
        String value = getCellString(row, 9);
        String addr = getCellString(row, 14);
        return !(desc.isEmpty() && value.isEmpty() && addr.isEmpty());
    }

    private static void appendElement(Document doc, Element parent, String tag, String text) {
        Element elem = doc.createElement(tag);
        elem.setTextContent(text);
        parent.appendChild(elem);
    }
}
