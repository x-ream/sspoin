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

    public static <T> Paras<T> read(
            Errors errors,
            Parsed parsed,
            String filename, InputStream stream) throws Exception {

        if (parsed == null) {
            throw new IllegalArgumentException("parsed can not null");
        }

        if (errors == null) {
            throw new IllegalArgumentException("errors can not null, init errors after parsed");
        }

        List<Field> paraFields = new ArrayList<>();
        List<T> list = read0(errors, parsed, paraFields, filename, stream);

        repeated(parsed, list);

        ErrorAppender.append(errors, list);

        List<String> nonRepeatedProps = parseNonRepeateableProps(parsed);

        return new Paras<T>(list, nonRepeatedProps, paraFields);

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

        if (parsed.isAbortAllRepeated()) {//not easy to update data in db

            for (Map.Entry<Field, Boolean> entry : map.entrySet()) {
                Field field = entry.getKey();
                Boolean nonRepeated = entry.getValue();
                if (nonRepeated) {
                    Map<String, List<Templated>> cvMap = new HashMap<>();
                    for (Object t : list) {
                        Templated template = (Templated) t;
                        try {
                            Object o = field.get(template);
                            String v = String.valueOf(o);
                            List<Templated> tList = cvMap.get(v);
                            if (tList == null) {
                                tList = new ArrayList<>();
                                cvMap.put(v, tList);
                            }
                            tList.add(template);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    for (Map.Entry<String,List<Templated>> cvEntry : cvMap.entrySet()) {
                        String v = cvEntry.getKey();
                        List<Templated> tList = cvEntry.getValue();
                        if (tList.size() > 1) { // repeated
                            String meta = parsed.getMetaMap().get(field);
                            for (Templated t : tList) {
                                t.appendError(meta, v + "," + parsed.getRepeatedError());
                            }
                        }
                    }
                    cvMap.clear();
                }
            }

        }else { //normal
            for (Map.Entry<Field, Boolean> entry : map.entrySet()) {
                Field field = entry.getKey();
                Boolean nonRepeated = entry.getValue();
                if (nonRepeated) {
                    Set<String> set = new HashSet<>();
                    for (Object t : list) {
                        Templated template = (Templated) t;
                        try {
                            Object o = field.get(template);
                            String v = String.valueOf(o);
                            if (!set.add(v)) {
                                String meta = parsed.getMetaMap().get(field);
                                template.appendError(meta, v + "," + parsed.getRepeatedError());
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    set.clear();
                }
            }
        }

    }


    private static <T> List<T> read0(Errors errors, Parsed parsed,
                                     List<Field> paraFields,
                                     String filename, InputStream stream) throws Exception {

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
                        throw new IllegalArgumentException(value + ", " + parsed.getRequiredTag() + " cat not be as prefix, put it as suffix");
                    String[] arr = str.split("---");
                    meta = arr[0].trim();
                    requiredMap.put(i, true);
                } else {
                    meta = str;
                    requiredMap.put(i, false);
                }
                metaMap.put(i, meta);
                errors.getMetas().add(meta);
                Field field = parsed.getFieldByMeta(meta);
                paraFields.add(field);
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
                                double v = cell.getNumericCellValue();
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    Date date = DateUtil.getJavaDate(v);
                                    dataMap.put(j, date);
                                } else {
                                    dataMap.put(j, v);
                                }
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

                Templated template = parsed.getClzz().newInstance();
                list.add((T) template);
                template.setRowNum(startRow);

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
                        template.appendError(meta, meta + parsed.getBlankError());
                    } else {
                        if (field.getType() == String.class) {
                            String str = null;
                            if (value != null) {
                                str = String.valueOf(value);
                            }
                            field.set(template, str);
                        } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                            if (value == null) {
                                field.set(template, false);
                            } else {
                                String str = String.valueOf(value);
                                int bn = Integer.valueOf(str);
                                field.set(template, bn == 0 ? false : true);
                            }
                        } else if (field.getType() == Date.class) {
                            if (value != null && value instanceof Date) {
                                field.set(template, value);
                            } else {
                                try {
                                    String str = "" + value;
                                    Date date = parsed.getDateFormat().parse(str);
                                    field.set(template, date);
                                } catch (Exception e) {
                                    e.printStackTrace();

                                    String s = "";
                                    try {
                                        s = s + value;
                                    } catch (Exception ee) {
                                        ee.printStackTrace();
                                    }
                                    template.appendError(meta, s + " ?|text(" + parsed.getDateFormat().toPattern() + ")");
                                }
                            }
                        } else {
                            BigDecimal bg = BigDecimal.ZERO;
                            String str = null;
                            if (value != null) {
                                str = String.valueOf(value);
                            }
                            if (StringUtils.isNotBlank(str)) {
                                bg = new BigDecimal(str);
                            }
                            if (bg.compareTo(BigDecimal.ZERO) == 0 && isRequired) {
                                template.appendError(meta, meta + parsed.getZeroError());
                            } else {
                                if (field.getType() == Long.class || field.getType() == long.class) {
                                    field.set(template, bg.longValue());
                                } else if (field.getType() == Integer.class || field.getType() == int.class) {
                                    field.set(template, bg.intValue());
                                } else if (field.getType() == Double.class || field.getType() == double.class) {
                                    field.set(template, bg.doubleValue());
                                } else if (field.getType() == BigDecimal.class) {
                                    field.set(template, bg);
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
