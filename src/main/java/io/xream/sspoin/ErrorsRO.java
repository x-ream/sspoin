package io.xream.sspoin;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Sim
 */
public class ErrorsRO {

    private String fileName;
    private int rowOffset;
    private List<String> metas;
    private List<RowError> rowErrors;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getRowOffset() {
        return rowOffset;
    }

    public void setRowOffset(int rowOffset) {
        this.rowOffset = rowOffset;
    }

    public List<String> getMetas() {
        return metas;
    }

    public void setMetas(List<String> metas) {
        this.metas = metas;
    }

    public List<RowError> getRowErrors() {
        return rowErrors;
    }

    public void setRowErrors(List<RowError> rowErrors) {
        this.rowErrors = rowErrors;
    }


    public byte[] toBuffer() throws IOException {
        if (rowErrors.isEmpty())
            throw new IllegalArgumentException("Missing Errors");
        if (metas.isEmpty())
            throw new IllegalArgumentException("Missing Metas");

        final String fileNamePrex = "ERROR_DATA";

        List<String> headerList = new ArrayList<>();
        headerList.add("Error_num");
        headerList.addAll(metas);

        ByteArrayOutputStream os = null;
        SXSSFWorkbook wb = new SXSSFWorkbook();
        try {
            createExcel(wb, fileNamePrex, headerList.toArray(), rowErrors, rowOffset);
            os = new ByteArrayOutputStream();
            wb.write(os);
            return os.toByteArray();
        } finally {
            if (wb != null) {
                wb.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }


    private void createExcel(SXSSFWorkbook workbook, String sheetName, Object[] headers, List<RowError> rowErrors, int rowOffset) {

        SXSSFSheet sheet = workbook.createSheet(sheetName);

        int i = 0;
        SXSSFRow row = sheet.createRow(i++);
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);

        for (int j = 0; j < headers.length; j++) {
            Cell cell = row.createCell(j);
            cell.setCellValue((String) headers[j]);
            cell.setCellStyle(style);
        }

        rowErrors.sort(Comparator.comparing(RowError::getRowNum));

        for (RowError rowError : rowErrors) {
            row = sheet.createRow(i++);
            int j = 0;
            row.createCell(j++).setCellValue(rowError.getRowNum() + rowOffset);

            for (; j < headers.length; j++) {
                String meta = (String) headers[j];
                Cell cell = row.createCell(j);
                for (CellError error : rowError.getCellErrors()) {
                    if (meta.equals(error.getMeta())) {
                        cell.setCellValue(error.getError());
                        break;
                    }
                }
            }
        }

    }

}
