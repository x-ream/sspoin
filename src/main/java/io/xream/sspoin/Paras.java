package io.xream.sspoin;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Sim
 */
public class Paras<T> {

    private List<T> list;
    private List<String> nonRepeatableProps;
    private List<Field> paraFields;

    public Paras(List<T> list, List<String> nonRepeatableProps, List<Field> paraFields) {
        this.list = list;
        this.nonRepeatableProps = nonRepeatableProps;
        this.paraFields = paraFields;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public List<String> getNonRepeatableProps() {
        return nonRepeatableProps;
    }

    public void setNonRepeatableProps(List<String> nonRepeatableProps) {
        this.nonRepeatableProps = nonRepeatableProps;
    }

    public List<Field> getParaFields() {
        return paraFields;
    }

    public void setParaFields(List<Field> paraFields) {
        this.paraFields = paraFields;
    }
}
