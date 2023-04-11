package io.xream.sspoin;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Sim
 */
public class NonRepeatableSavingFilter {


    public static void filter(
            Errors errors,Parsed parsed, Result result,
            Class poClzz,
            SavedFinder.NonRepeatableSavedCond nonRepeatableSavedCond,
            SavedFinder savedFinder) {

        if (errors == null) {
            throw new IllegalArgumentException("errors can not null");
        }

        if (nonRepeatableSavedCond == null) {
            throw new IllegalArgumentException("nonRepeatableSavedCond can not null");
        }

        if (savedFinder == null) {
            throw new IllegalArgumentException("savedFinder can not null");
        }

        Set<String> poPropSet = poFieldNames(poClzz);

        Object cond = nonRepeatableSavedCond.build(
                buildSelectList(poPropSet,result),
                buildInConditions(poPropSet,result)
        );

        List<Map<String, Object>> poExistList = savedFinder.find(cond);
        if (poExistList.isEmpty())
            return;

        handleRepeated(poPropSet,poExistList,errors,parsed,result);
    }

    private static Set<String> poFieldNames(Class poClzz) {

        if (poClzz == null)
            throw new IllegalArgumentException("PO Class must not be null");

        Field[] arr = poClzz.getDeclaredFields();
        Set<String> propSet = new HashSet<>();
        for (Field field : arr) {
            propSet.add(field.getName());
        }
        return propSet;
    }

    private static List<String> buildSelectList(Set<String> poPropSet, Result result) {
        List<String> selectList = new ArrayList<>();
        for (Object obj : result.getNonRepeatableProps()) {
            if (poPropSet.contains(obj)) {
                selectList.add((String)obj);
            }
        }
        return selectList;
    }

    private static Map<String, List<Object>> buildInConditions (Set<String> poPropSet,
                                                         Result result) {
        Map<String, List<Object>> condMap = new HashMap<>();
        for (Object obj : result.getNonRepeatableProps()) {
            String prop = (String) obj;

            if (poPropSet.contains(prop)) {
                List<Object> inObjList = new ArrayList<Object>();
                for (Object o : result.getList()) {
                    try {
                        Field field = o.getClass().getDeclaredField(prop);
                        field.setAccessible(true);

                        Object v = field.get(o);
                        inObjList.add(v);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
                condMap.put(prop, inObjList);
            }
        }
        return condMap;
    }


     private static void handleRepeated(Set<String> poPropSet, List<Map<String,Object>> poExistList,
                                        Errors errors, Parsed parsed, Result result) {
        Map<String, Set<Object>> exitMap = new HashMap<>();
        for (Map<String, Object> map : poExistList) {
            for (Object obj : result.getNonRepeatableProps()) {//compare exists
                String prop = (String) obj;
                if (poPropSet.contains(prop)) {
                    Set<Object> vSet = exitMap.get(prop);
                    if (vSet == null) {
                        vSet = new HashSet<>();
                        exitMap.put(prop, vSet);
                    }
                    Object v = map.get(prop);
                    if (v != null) {
                        vSet.add(v);
                    }
                }
            }
        }

        for (Object obj : result.getNonRepeatableProps()) {//compare exists
            String prop = (String) obj;
            if (poPropSet.contains(prop)) {

                Set<Object> vSet = exitMap.get(prop);
                if (vSet == null)
                    continue;

                for (Object para : result.getList())
                    try {
                        Field field = parsed.getFieldByMeta(prop);
                        Object v = field.get(para);
                        if (vSet.contains(v)) {
                            Templated template = (Templated) para;
                            CellError error = new CellError();
                            error.setMeta(parsed.getMetaMap().get(field));
                            error.setError(v + "," + parsed.getExistError());
                            template.getRowError().getCellErrors().add(error);
                            continue;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
        }
        ErrorAppender.append(errors,result.getList());
    }
}
