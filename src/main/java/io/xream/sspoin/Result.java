package io.xream.sspoin;

import java.util.List;

/**
 * @author Sim
 */
public class Result<T> {

    private List<T> list;
    private List<String> nonRepeatableProps;

    public Result(List<T> list, List<String> nonRepeatableProps) {
        this.list = list;
        this.nonRepeatableProps = nonRepeatableProps;
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
}
