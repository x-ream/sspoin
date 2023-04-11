package io.xream.sspoin;

import java.util.List;
import java.util.Map;

/**
 * @author Sim
 */
public interface SavedFinder {

    List<Map<String, Object>> find(Object cond);

    interface NonRepeatableSavedCond {

        Object build(List<String> selectList, Map<String,List<Object>> inCondMap);

    }
}
