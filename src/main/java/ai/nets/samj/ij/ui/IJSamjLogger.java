package ai.nets.samj.ij.ui;

import ai.nets.samj.ui.SAMJLogger;
import org.scijava.log.Logger;

public class IJSamjLogger implements SAMJLogger {

	final private Logger log;
	final private String prefix;

	public IJSamjLogger(Logger scijavaLogger) {
		this(scijavaLogger,"");
	}
	public IJSamjLogger(Logger scijavaLogger, String loggedPrefix) {
		this.log = scijavaLogger;
		this.prefix = loggedPrefix;
	}

	@Override
	public void info(String text) {
		log.info(prefix+text);
	}

	@Override
	public void warn(String text) {
		log.warn(prefix+text);
	}

	@Override
	public void error(String text) {
		log.error(prefix+text);
	}
}
