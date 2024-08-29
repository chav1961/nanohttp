package chav1961.nanohttp.server.jmx;

import java.io.IOException;

public interface JmxManagerMBean {
	void start() throws IOException;
	void suspend() throws IOException;
	void resume() throws IOException;
	void stop() throws IOException;
	void terminateAndExit() throws IOException;
	boolean isStarted();
	boolean isSuspended();
}
