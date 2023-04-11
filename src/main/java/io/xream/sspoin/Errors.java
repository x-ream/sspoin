package io.xream.sspoin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sim
 */
public class Errors {

    private String fileName;
    private int rowOffset;
    private List<String> metas = new ArrayList<>();
    private List<RowError> rowErrors;

    private Errors(Parsed parsed,String fileName) {
        this.fileName = fileName;
        this.rowOffset = parsed.getMetaRow();
        this.rowErrors = new ArrayList<>();
    }

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

    public List<RowError> getRowErrors() {
        return rowErrors;
    }

    public void setRowErrors(List<RowError> rowErrors) {
        this.rowErrors = rowErrors;
    }

    public static Errors of(Parsed parsed, String fileName) {
        if (parsed == null){
            throw new IllegalArgumentException("parsed before, then init errors");
        }
        return new Errors(parsed,fileName);
    }

}
