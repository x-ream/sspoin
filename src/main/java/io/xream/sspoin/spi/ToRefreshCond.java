package io.xream.sspoin.spi;

import java.util.List;
import java.util.Map;

/**
 * @author Sim
 */
public interface ToRefreshCond {

    Object buildExistsCond(Class poClzz, String refreshOn, List<Object> values);
    Object buildRefreshCond(Class poClzz, Map<String,Object> map , String refreshOn, Object condValue);

}
