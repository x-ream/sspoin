package io.xream.sspoin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Sim
 */
public interface ErrorAppender {

    static void append(Errors errors, List list) {

        Set<Integer> rowNumSet = new HashSet<Integer>();

        for (Object t : list) {
            Templated obj = (Templated) t;
            final List<CellError> cellErrorList = obj.getRowError().getCellErrors();
            if (!cellErrorList.isEmpty()) {
                int rowNum = obj.getRowNum();
                RowError rowErrorExist = null;
                for (RowError rowError : errors.getRowErrors()){
                    if (rowNum == rowError.getRowNum()){
                        rowErrorExist = rowError;
                        break;
                    }
                }
                if (rowErrorExist == null) {
                    rowErrorExist = new RowError();
                    rowErrorExist.setRowNum(rowNum);
                    errors.getRowErrors().add(rowErrorExist);
                }
                rowErrorExist.getCellErrors().addAll(cellErrorList);

                rowNumSet.add(obj.getRowNum());
            }
        }

        Iterator<Templated> iterator = list.iterator();
        while (iterator.hasNext()) {
            Templated obj = iterator.next();
            Integer v = obj.getRowNum();
            if (rowNumSet.contains(v)) {
                iterator.remove();
            }
        }
    }
}
