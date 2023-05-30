package io.xream.sspoin;

import io.xream.sspoin.spi.NonRepeatableExistedCond;
import io.xream.sspoin.spi.ToRefreshCond;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Sim
 */
public class NonRepeatableFilter {

    /**
     * if config refreshOn and has the meta in sheet, then can invoke the method
     */
    public static void refreshFirst(String refreshOn,
                                    List<Field> paraFields,
                                    List paraList,
                                    Class poClzz,
                                    ToRefreshCond toRefreshCond,
                                    ExistedToRefresh existedToRefresh) {

        if (poClzz == null) {
            throw new IllegalArgumentException("poClzz can not null");
        }

        if (toRefreshCond == null) {
            throw new IllegalArgumentException("refreshingCond can not null");
        }

        if (existedToRefresh == null) {
            throw new IllegalArgumentException("existedRefresh can not null");
        }

        //if sheet has the refreshOn;
        if (StringUtils.isBlank(refreshOn))
            return;

        boolean hasRefreshOn = false;
        for (Field f : paraFields){
            if (f.getName().equals(refreshOn)) {
                hasRefreshOn = true;
                break;
            }
        }

        if (!hasRefreshOn)
            return;

        Object existsCond = toRefreshCond.buildExistsCond(poClzz,refreshOn,
                buildInCondition(refreshOn,paraList));

        List<Object> existsValues = existedToRefresh.existsValues(existsCond);

        for (Object value : existsValues) {

            Object cond = toRefreshCond.buildRefreshCond(poClzz,
                    buildToRefreshMap(paraFields,refreshOn,value,paraList),
                    refreshOn,
                    value);

            existedToRefresh.refresh(cond);

        }

        filterRefreshed(refreshOn,existsValues,paraList);
    }


    public static void beforeCreate(
            Errors errors, Parsed parsed,
            List paraList,
            Class poClzz,
            NonRepeatableExistedCond nonRepeatableExistedCond,
            ExistedFinder existedFinder) {


        if (errors == null) {
            throw new IllegalArgumentException("errors can not null");
        }

        if (poClzz == null) {
            throw new IllegalArgumentException("poClzz can not null");
        }

        if (nonRepeatableExistedCond == null) {
            throw new IllegalArgumentException("nonRepeatableSavedCond can not null");
        }

        if (existedFinder == null) {
            throw new IllegalArgumentException("savedFinder can not null");
        }

        Set<String> poPropSet = poFieldNames(poClzz);

        List<String> nonRepeatableProps = parsed.getNonRepeatableProps();

        Object cond = nonRepeatableExistedCond.build(
                poClzz,
                buildSelectList(poPropSet, nonRepeatableProps),
                buildInConditions(poPropSet,nonRepeatableProps,paraList)
        );

        List<Map<String, Object>> poExistList = existedFinder.find(cond);
        if (poExistList.isEmpty())
            return;

        handleRepeated(poPropSet,poExistList,errors,parsed,nonRepeatableProps,paraList);
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

    private static List<String> buildSelectList(Set<String> poPropSet, List<String> nonRepeatableProps) {
        List<String> selectList = new ArrayList<>();
        for (String prop : nonRepeatableProps) {
            if (poPropSet.contains(prop)) {
                selectList.add(prop);
            }
        }
        return selectList;
    }

    private static List<Object> buildInCondition (String prop, List paraList) {

        List<Object> inObjList = new ArrayList<Object>();
        for (Object o : paraList) {

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

        return inObjList;
    }


    private static Map<String,Object> buildToRefreshMap(List<Field> paraFieldList, String refreshOn, Object value, List paraList) {


        for (Object o : paraList) {

            try {
                Field field = o.getClass().getDeclaredField(refreshOn);
                field.setAccessible(true);

                Object v = field.get(o);
                if (String.valueOf(value).equals(String.valueOf(v))){
                    Map<String,Object> map = new HashMap<>();
                    for (Field f : paraFieldList){
                        f.setAccessible(true);
                        String key = f.getName();
                        if (key.equals(refreshOn))
                            continue;
                        map.put(key, f.get(o));
                    }

                   return map;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }

        }

        return null;
    }

    private static void filterRefreshed(String refreshOn, List<Object> existsValues, List list) {

        Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            Object o = iterator.next();
            try {
                Field field = o.getClass().getDeclaredField(refreshOn);
                field.setAccessible(true);
                Object v = field.get(o);
                for (Object ev : existsValues) {
                    if (String.valueOf(v).equals(String.valueOf(ev))) {
                        iterator.remove();
                        break;
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }

        }

    }


    private static Map<String, List<Object>> buildInConditions (Set<String> poPropSet,
                                                                List<String> nonRepeatableProps,
                                                                List paraList) {
        Map<String, List<Object>> condMap = new HashMap<>();
        for (Object obj : nonRepeatableProps) {
            String prop = (String) obj;

            if (poPropSet.contains(prop)) {
                List<Object> inObjList = new ArrayList<Object>();
                for (Object o : paraList) {
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
                                        Errors errors, Parsed parsed,
                                        List<String> nonRepetableProps,
                                        List paraList) {
        Map<String, Set<Object>> exitMap = new HashMap<>();
        for (Map<String, Object> map : poExistList) {
            for (Object obj : nonRepetableProps) {//compare exists
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

        for (Object obj : nonRepetableProps) {//compare exists
            String prop = (String) obj;
            if (poPropSet.contains(prop)) {

                Set<Object> vSet = exitMap.get(prop);
                if (vSet == null)
                    continue;

                for (Object para : paraList) {
                    try {
                        Field field = parsed.getFieldByMeta(prop);
                        Object v = field.get(para);
                        if (vSet.contains(v)) {
                            Templated template = (Templated) para;
                            template.appendError(
                                    parsed.getMetaByProp(prop),
                                    v + "," + parsed.getExistError()
                            );
                            continue;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        ErrorAppender.append(errors,paraList);
    }
}
