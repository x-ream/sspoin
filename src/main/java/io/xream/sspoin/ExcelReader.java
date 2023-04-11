package io.xream.sspoin;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author Sim
 */
public class ExcelReader {

    public static <T> Result<T> read(
            Errors errors,
            Parsed parsed,
            String filename, InputStream stream) throws Exception {

        if (parsed == null) {
            throw new IllegalArgumentException("parsed can not null");
        }

        if (errors == null) {
            throw new IllegalArgumentException("errors can not null, init errors after parsed");
        }

        List<T> list = read0(errors,parsed, filename, stream);

        repeated(parsed, list);

        ErrorAppender.append(errors, list);

        List<String> nonRepeatedProps = parseNonRepeateableProps(parsed);

        return new Result<T>(list, nonRepeatedProps);

    }


    private static List<String> parseNonRepeateableProps(Parsed parsed) {
        List<String> list = new ArrayList<String>();
        for (Map.Entry<Field, Boolean> entry : parsed.getNonRepeatableMap().entrySet()) {
            Field field = entry.getKey();
            Boolean nonRepeated = entry.getValue();
            if (nonRepeated) {
                list.add(field.getName());
            }
        }
        return list;
    }

    private static void repeated(Parsed parsed, List list) {

        Map<Field, Boolean> map = parsed.getNonRepeatableMap();

        for (Map.Entry<Field, Boolean> entry : map.entrySet()) {
            Field field = entry.getKey();
            Boolean nonRepeated = entry.getValue();
            if (nonRepeated) {
                Set<String> set = new HashSet<>();
                for (Object t : list) {
                    Templated obj = (Templated) t;
                    try {
                        Object o = field.get(obj);
                        String v = String.valueOf(o);
                        if (!set.add(v)) {
                            String meta = parsed.getMetaMap().get(field);
                            CellError error = new CellError();
                            error.setMeta(meta);
                            error.setError(v + "," + parsed.getRepeatedError());
                            obj.getRowError().getCellErrors().add(error);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }


    private static <T> List<T> read0(Errors errors, Parsed parsed, String filename, InputStream stream) throws Exception {

        List<T> list = new ArrayList<>();

        Workbook workbook = null;
        try {
            boolean isXls = filename.toLowerCase().endsWith(".xls");
            if (isXls) {
                workbook = new HSSFWorkbook(stream);
            } else if (filename.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(stream);
            }

            Sheet sheet = StringUtils.isBlank(parsed.getSheetName()) ? workbook.getSheetAt(0) : workbook.getSheet(parsed.getSheetName());

            int i = 0;
            Map<Integer, String> metaMap = new HashMap<>();
            Map<Integer, Boolean> requiredMap = new HashMap<>();
            Row row = sheet.getRow(parsed.getMetaRow());
            if (row == null) {
                throw new IllegalArgumentException("META not found");
            }
            Iterator<Cell> cellIte = row.cellIterator();
            while (cellIte.hasNext()) {
                Cell cell = cellIte.next();
                String value = cell.getStringCellValue();
                if (StringUtils.isBlank(value))
                    continue;
                String str = value.trim();
                String meta = null;
                if (str.contains(parsed.getRequiredTag())) {
                    str = str.replace(parsed.getRequiredTag(), "---");
                    if (str.startsWith("---"))
                        throw new IllegalArgumentException(value +", "+parsed.getRequiredTag() + " cat not be as prefix, put it as suffix");
                    String[] arr = str.split("---");
                    meta = arr[0].trim();
                    requiredMap.put(i, true);
                } else {
                    meta = str;
                    requiredMap.put(i, false);
                }
                metaMap.put(i, meta);
                errors.getMetas().add(meta);
                i++;
            }

            int cLen = metaMap.size();
            int lastRow = sheet.getLastRowNum();
            for (int startRow = parsed.getStartRow(); startRow <= lastRow; startRow++) {
                row = sheet.getRow(startRow);
                // read to map
                Map<Integer, Object> dataMap = new HashMap<>();
                for (int j = 0; j < cLen; j++) {
                    Cell cell = row.getCell(j);
                    if (cell == null) {
                        dataMap.put(j, null);
                    } else {
                        CellType type = cell.getCellTypeEnum();
                        switch (type) {
                            case STRING:
                                dataMap.put(j, cell.getStringCellValue().trim());
                                break;
                            case NUMERIC:
                                dataMap.put(j, cell.getNumericCellValue());
                                break;
                            case BOOLEAN:
                                dataMap.put(j, cell.getBooleanCellValue());
                                break;
                            case BLANK:
                                dataMap.put(j, null);
                                break;
                        }
                    }
                }

                Object notBlankObj = dataMap.get(parsed.getRowIgnoreIfBlankAt());
                if (notBlankObj == null
                        || (notBlankObj instanceof String && StringUtils.isBlank(String.valueOf(notBlankObj))))
                    continue;

                Templated obj = parsed.getClzz().newInstance();
                list.add((T) obj);
                obj.setRowNum(startRow);

                // map to templated object
                for (Map.Entry<Integer, Object> entry : dataMap.entrySet()) {

                    Integer j = entry.getKey();

                    String meta = metaMap.get(j);

                    Field field = parsed.getFieldByMeta(meta);

                    if (sheet == null) {
                        throw new IllegalArgumentException("sheet meta not same as annotation: " + meta);
                    }

                    Object value = entry.getValue();

                    final boolean isRequired = requiredMap.get(j);

                    if (isRequired &&
                            (value == null || StringUtils.isBlank(value + ""))
                    ) { //BLANK
                        CellError error = new CellError();
                        error.setMeta(meta);
                        error.setError(meta + parsed.getBlankError());
                        obj.getRowError().getCellErrors().add(error);
                    } else {
                        String str = String.valueOf(value);
                        if (field.getType() == String.class) {
                            field.set(obj, str);
                        } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                            int bn = Integer.valueOf(str);
                            field.set(obj, bn == 0 ? false : true);
                        } else if (field.getType() == Date.class) {
                            try {
                                Date date = parsed.getDateFormat().parse(str);
                                field.set(obj, date);
                            } catch (Exception e) {
                                if (isRequired) {
                                    CellError error = new CellError();
                                    error.setMeta(meta);
                                    error.setError(str + " ?|(" + parsed.getDateFormat().toPattern() + ")");
                                    obj.getRowError().getCellErrors().add(error);
                                }
                            }
                        } else {
                            BigDecimal bg = new BigDecimal(str);
                            if (bg.compareTo(BigDecimal.ZERO) == 0 && isRequired) {
                                CellError error = new CellError();
                                error.setMeta(meta);
                                error.setError(meta + parsed.getZeroError());
                                obj.getRowError().getCellErrors().add(error);
                            } else {
                                if (field.getType() == Long.class || field.getType() == long.class) {
                                    field.set(obj, bg.longValue());
                                } else if (field.getType() == Integer.class || field.getType() == int.class) {
                                    field.set(obj, bg.intValue());
                                } else if (field.getType() == Double.class || field.getType() == double.class) {
                                    field.set(obj, bg.doubleValue());
                                } else if (field.getType() == BigDecimal.class) {
                                    field.set(obj, bg);
                                } else {
                                    throw new IllegalStateException("not supported field type: " + field.getName() + "," + field.getType());
                                }
                            }
                        }
                    }

                    i++;
                }

            }
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;
    }


}
