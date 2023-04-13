package io.xream.sspoin;

import java.util.List;
import java.util.Map;

/**
 * @author Sim
 */
public interface ExistedFinder {

    /**
     * find result from DB or by a remote call
     */
    List<Map<String, Object>> find(Object cond);
    
}
