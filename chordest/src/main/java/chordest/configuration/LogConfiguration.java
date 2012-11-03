package chordest.configuration;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Class to update log configuration when chordest is already running.
 * @author Nikolay
 *
 */
public class LogConfiguration {

	/**
	 * Adds new log appender to log to a file located in a given directory.
	 * This method was added to be able to pass log file directory as a command
	 * line argument.
	 * @param dir
	 */
	public static void setLogFileDirectory(String dir) {
	    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

	    FileAppender fileAppender = new FileAppender();
	    fileAppender.setContext(loggerContext);
	    fileAppender.setName("LOG_FILE");
	    // set the file name
	    fileAppender.setFile(dir + "chordest.log");

	    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
	    encoder.setContext(loggerContext);
	    encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
	    encoder.start();

	    fileAppender.setEncoder(encoder);
	    fileAppender.start();

	    // attach the rolling file appender to the logger of your choice
	    Logger logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
	    logbackLogger.addAppender(fileAppender);

	    // OPTIONAL: print logback internal status messages
	    StatusPrinter.print(loggerContext);
	}

}
