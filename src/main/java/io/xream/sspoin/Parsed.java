package io.xream.sspoin;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sim
 */
public class Parsed {

    private Class<? extends Templated> clzz;
    private String sheetName;
    private SimpleDateFormat dateFormat;
    private int rowIgnoreIfBlankAt;
    private int metaRow;
    private int startRow;
    private String requiredTag;
    private String blankError;
    private String zeroError;
    private String repeatedError;
    private String existError;
    private String refreshOn;
    private final Map<String, Field> metaFieldMap = new HashMap<>();
    private final Map<Field, Boolean> nonRepeatableMap = new HashMap<>();
    private final Map<Field, String> metaMap = new HashMap<>();
    private final Map<String, String> propMetaMap = new HashMap<>();


    public Class<? extends Templated> getClzz() {
        return clzz;
    }

    public void setClzz(Class<? extends Templated> clzz) {
        this.clzz = clzz;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public int getRowIgnoreIfBlankAt() {
        return rowIgnoreIfBlankAt;
    }

    public void setRowIgnoreIfBlankAt(int rowIgnoreIfBlankAt) {
        this.rowIgnoreIfBlankAt = rowIgnoreIfBlankAt;
    }

    public String getRequiredTag() {
        return requiredTag;
    }

    public void setRequiredTag(String requiredTag) {
        this.requiredTag = requiredTag;
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public int getMetaRow() {
        return metaRow;
    }

    public void setMetaRow(int metaRow) {
        this.metaRow = metaRow;
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public String getBlankError() {
        return blankError;
    }

    public void setBlankError(String blankError) {
        this.blankError = blankError;
    }

    public String getZeroError() {
        return zeroError;
    }

    public void setZeroError(String zeroError) {
        this.zeroError = zeroError;
    }

    public String getRepeatedError() {
        return repeatedError;
    }

    public void setRepeatedError(String repeatedError) {
        this.repeatedError = repeatedError;
    }

    public String getExistError() {
        return existError;
    }

    public void setExistError(String existError) {
        this.existError = existError;
    }

    public Map<String, Field> getMetaFieldMap() {
        return metaFieldMap;
    }

    public void putFieldByMeta(String key, Field field) {
        metaFieldMap.put(key, field);
    }

    public void putFieldByProp(String key, Field field) {
        metaFieldMap.put(key, field);
    }

    public void putMeta(Field field, String meta) {
        metaMap.put(field, meta);
        propMetaMap.put(field.getName(),meta);
    }

    public Field getFieldByMeta(String key) {
        return metaFieldMap.get(key);
    }

    public void putRepeated(Field key, Boolean value) {
        nonRepeatableMap.put(key, value);
    }

    public String getRefreshOn() {
        return refreshOn;
    }

    public void setRefreshOn(String refreshOn) {
        this.refreshOn = refreshOn;
    }

    public Map<Field, Boolean> getNonRepeatableMap() {
        return nonRepeatableMap;
    }

    public Map<Field, String> getMetaMap() {
        return metaMap;
    }

    public String getMetaByProp(String prop) {
        return propMetaMap.get(prop);
    }

    public static Parsed of(Class<? extends Templated> templateClzz) {
        Template template = templateClzz.getAnnotation(Template.class);

        Parsed parsed = new Parsed();
        parsed.setClzz(templateClzz);
        parsed.setSheetName(template.sheetName());
        parsed.setRowIgnoreIfBlankAt(template.rowIgnoreIfBlankAt());
        parsed.setMetaRow(template.metaRow());
        parsed.setStartRow(template.startRow());
        parsed.setDateFormat(new SimpleDateFormat(template.dateFormat()));
        parsed.setRequiredTag(template.requiredTag());
        parsed.setBlankError(template.blankError());
        parsed.setZeroError(template.zeroError());
        parsed.setRepeatedError(template.repeatedError());
        parsed.setExistError(template.existsError());
        parsed.setRefreshOn(template.refreshOn());

        Field[] fields = null;
        Object obj = null;

        try {
            obj = templateClzz.newInstance();
            fields = obj.getClass().getDeclaredFields();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        for (Field field : fields) {
            field.setAccessible(true);
            Template.Row row = field.getAnnotation(Template.Row.class);
            if (row == null) continue;

            if (StringUtils.isNoneBlank(row.meta())) {
                parsed.putFieldByMeta(row.meta(), field);
                parsed.putMeta(field, row.meta());
            }else {
                parsed.putMeta(field,field.getName());
            }

            parsed.putFieldByProp(field.getName(), field);

            parsed.putRepeated(field, row.nonRepeatable());
        }
        return parsed;
    }
}
