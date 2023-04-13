package io.xream.sspoin;

import java.util.List;

/**
 * @author Sim
 */
public interface ExistedToRefresh {

    List<Object> existsValues(Object cond);
    boolean refresh(Object cond);

}
