    /*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.nettyUtil.EtoX;


    import org.apache.poi.ss.usermodel.*;
    import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
            String excelPath = "C:\\Users\\wangwenbo\\Desktop\\T600.xlsx";
            String outputXml = "D:\\project\\java\\T600.1.xml";

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
                appendElement(doc, item, "Timeout", getCellString(firstRow, 3));
                appendElement(doc, item, "ACK", getCellString(firstRow, 6));

                // InputString
                Element inputString = doc.createElement("InputString");
                boolean hasValidInput = false;
                for (Row row : rows) {
                    String desc = getCellString(row, 7); // 第8列
                    Cell cell = row.getCell(8); // 第9列
                    String value = getCellString(row, 9); // 第10列

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

                // OutputString
                Element outputString = doc.createElement("OutputString");
                boolean hasValidOutput = false;
                for (Row row : rows) {
                    String desc = getCellString(row, 34); // 第35列
                    Cell addrCell = row.getCell(35);      // 第27列
                    String value = getCellString(row, 36); // 第37列

                    if (addrCell != null && !desc.isEmpty()) {
                        String addrExcelPos = getExcelColumnName(addrCell.getColumnIndex()) + (addrCell.getRowIndex() + 1);
                        appendElement(doc, outputString, "string", desc + ":" + addrExcelPos + ":" + value);
                        hasValidOutput = true;
                    }
                }
                if (!hasValidOutput) {
                    appendElement(doc, outputString, "string", "");
                }
                item.appendChild(outputString);

                // SendBytes
                Element sendBytes = doc.createElement("SendBytes");
                List<String> sendList = new ArrayList<>();
                for (Row row : rows) {
                    for (int i = 10; i <= 20; i++) {
                        String val = getCellString(row, i).trim();
                        if (!val.isEmpty() && !val.equalsIgnoreCase("X")) {
                            sendList.add(val);
                        }
                    }
                    String check = getCellString(row, 21);
                    if (!check.isEmpty() && !check.equalsIgnoreCase("X")) {
                        sendList.add(check);
                    }
                }
                appendElement(doc, sendBytes, "string", String.join(":", sendList));
                item.appendChild(sendBytes);

                // ReadBytes
                Element readBytes = doc.createElement("ReadBytes");
                List<String> readList = new ArrayList<>();

                int rowIndex = 0;

                for (Row row : rows) {
                    List<String> tempList = new ArrayList<>();

                    // -----------------------------
                    // 处理第22列到第32列
                    // -----------------------------
                    if (rowIndex == 0) {
                        // 第一行：需要判断连续空格
                        int consecutiveEmpty = 0;
                        for (int i = 22; i <= 32; i++) {
                            String val = getCellString(row, i).trim();

                            if (!val.isEmpty() && !val.equalsIgnoreCase("X")) {
                                // 如果前面有一个空，则写入空位置
                                if (consecutiveEmpty == 1) {
                                    String cellPos = getExcelColName(i - 1) + (row.getRowNum() + 1);
                                    tempList.add(cellPos);
                                }
                                consecutiveEmpty = 0;
                                tempList.add(val);
                            } else {
                                consecutiveEmpty++;
                                if (consecutiveEmpty >= 2) {
                                    // 遇到两个或以上连续空，不写
                                }
                            }
                        }

                        // 检查最后一个是否是单空
                        if (consecutiveEmpty == 1) {
                            String cellPos = getExcelColName(32) + (row.getRowNum() + 1);
                            tempList.add(cellPos);
                        }

                        // 第33列（Check）处理
                        String check = getCellString(row, 33).trim();
                        if (!check.isEmpty() && !check.equalsIgnoreCase("X")) {
                            tempList.add(check);
                        } else {
                            tempList.add(getExcelColName(33) + (row.getRowNum() + 1));
                        }

                    } else {
                        // 非第一行：正常处理
                        for (int i = 22; i <= 32; i++) {
                            String val = getCellString(row, i).trim();
                            if (!val.isEmpty() && !val.equalsIgnoreCase("X")) {
                                tempList.add(val);
                            }
                        }

                        String check = getCellString(row, 33).trim();
                        if (!check.isEmpty() && !check.equalsIgnoreCase("X")) {
                            tempList.add(check);
                        }
                    }

                    readList.addAll(tempList);
                    rowIndex++;
                }

    // 最终写入XML
                appendElement(doc, readBytes, "string", String.join(":", readList));
                item.appendChild(readBytes);




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
