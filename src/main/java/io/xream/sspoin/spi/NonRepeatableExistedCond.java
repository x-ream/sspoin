package io.xream.sspoin.spi;

import java.util.List;
import java.util.Map;

/**
 * @author Sim
 */
public interface NonRepeatableExistedCond {

    Object build(Class poClzz, List<String> selectList, Map<String,List<Object>> inCondMap);

}
