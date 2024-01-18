package sc.fiji.samj.communication.model;

import org.scijava.log.Logger;
import sc.fiji.samj.communication.PromptsToNetAdapter;

/**
 * A common ground for various placeholder classes to inform
 * the system that even this network may be available/installed.
 * It is, however, not creating/instancing any connection to any
 * (Python) network code or whatsoever, that happens only after the
 * {@link SAMModel#instantiate(Logger)} is called, and reference
 * to such connection is returned.
 */
public interface SAMModel {

	String getName();
	String getDescription();
	boolean isInstalled();

	/** Returns null if it is no installed. */
	PromptsToNetAdapter instantiate(final Logger useThisLoggerForIt);
}