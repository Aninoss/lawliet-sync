package core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionUtil {

    private ExceptionUtil() {}

    public static Exception generateForStack(Thread t) {
        Exception e = new Exception("Stack Trace");
        e.setStackTrace(t.getStackTrace());
        return e;
    }



}