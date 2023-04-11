package io.xream.sspoin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sim
 */
public class RowError {

    private int rowNum;
    private List<CellError> cellErrors = new ArrayList<>();

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public List<CellError> getCellErrors() {
        return cellErrors;
    }

    public void setCellErrors(List<CellError> cellErrors) {
        this.cellErrors = cellErrors;
    }
}
