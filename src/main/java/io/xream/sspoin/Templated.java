package io.xream.sspoin;

/**
 * @author Sim
 */
public interface Templated {

    void setRowNum(int row);
    int getRowNum();
    RowError getRowError();

    default void appendError(String meta, String error) {
        CellError cellError = new CellError();
        cellError.setMeta(meta);
        cellError.setError(error);
        getRowError().getCellErrors().add(cellError);
    }
}
