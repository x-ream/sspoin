package io.xream.sspoin;

import java.lang.annotation.*;

/**
 * @author Sim
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Template {

    String sheetName() default "";
    int rowIgnoreIfBlankAt() default 0;
    int metaRow() default 0;
    int startRow() default 1;
    String dateFormat() default "yyyy-MM-dd";
    String requiredTag() default "*";
    String blankError() default "can not be blank";
    String zeroError() default "can not be zero";
    String repeatedError() default "can not be repeated";
    String existsError() default "exists already";

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface Row {
        String meta() default "";
        boolean nonRepeatable() default false;
    }
}
