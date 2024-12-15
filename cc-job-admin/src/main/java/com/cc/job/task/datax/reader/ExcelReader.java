package com.cc.job.task.datax.reader;

import cn.hutool.json.JSONObject;
import com.cc.job.task.datax.BaseRW;
import com.cc.job.task.model.datax.DataXParams;

public class ExcelReader implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
//        try {
//            // 生成 Excel reader 配置
//            //把路径的最后.xsl删除然后加上excel.csv后缀。形成新的路径
//            String csvFilePath =dataX.getFilePath().substring(0, dataX.getFilePath().lastIndexOf(".")) + "Excel2CSV.csv";
//            excelToCSV(dataX.getFilePath(), csvFilePath);
//
//            // 生成 CSV reader 配置
//            JSONObject readerConfig = new JSONObject();
//            readerConfig.put("name", "txtfilereader");
//            JSONObject parameter = new JSONObject();
//            parameter.put("path", csvFilePath);
//            parameter.put("column", new JSONObject[]{/* column definitions */});
//            readerConfig.put("parameter", parameter);
//            return readerConfig;
//        } catch (IOException e) {
//            throw new RuntimeException();
//        }
        return null;
    }

//    public void excelToCSV(String excelFilePath, String csvFilePath) throws IOException {
//        try (FileInputStream file = new FileInputStream(new File(excelFilePath));
//             Workbook workbook = WorkbookFactory.create(file);
//             FileOutputStream fos = new FileOutputStream(new File(csvFilePath));
//             OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
//             BufferedWriter bw = new BufferedWriter(osw)) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            for (Row row : sheet) {
//                StringBuilder sb = new StringBuilder();
//                for (Cell cell : row) {
//                    switch (cell.getCellType()) {
//                        case STRING:
//                            sb.append('"').append(cell.getStringCellValue()).append('"');
//                            break;
//                        case NUMERIC:
//                            if (DateUtil.isCellDateFormatted(cell)) {
//                                sb.append(cell.getDateCellValue());
//                            } else {
//                                sb.append(cell.getNumericCellValue());
//                            }
//                            break;
//                        case BOOLEAN:
//                            sb.append(cell.getBooleanCellValue());
//                            break;
//                        case FORMULA:
//                            sb.append(cell.getCellFormula());
//                            break;
//                        default:
//                            sb.append("");
//                    }
//                    sb.append(',');
//                }
//                if (sb.length() > 0) {
//                    bw.write(sb.substring(0, sb.length() - 1)); // remove last comma
//                }
//                bw.newLine();
//            }
//        }
//    }
}
