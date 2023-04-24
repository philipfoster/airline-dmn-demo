import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugLogger {

    private static final String LOGGER_NAME = "DMN_RULES";
    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    public static String log(String str) {

        logger.info(str);

        return "";
    }

}
