package chav1961.nanohttp.server.jmx;

import java.io.IOException;

import chav1961.nanohttp.server.NanoServiceWrapper;

public class JmxManager implements JmxManagerMBean {
	private final NanoServiceWrapper	wrapper;
	
	public JmxManager(final NanoServiceWrapper wrapper) {
		if (wrapper == null) {
			throw new NullPointerException("Wrapper can't be null");
		}
		else {
			this.wrapper = wrapper;
		}
	}
	
	@Override
	public void start() throws IOException {
		getWrapper().start();
	}

	@Override
	public void suspend() throws IOException {
		getWrapper().suspend();
	}

	@Override
	public void resume() throws IOException {
		getWrapper().resume();
	}

	@Override
	public void stop() throws IOException {
		getWrapper().stop();
	}

	@Override
	public void terminateAndExit() throws IOException {
		getWrapper().close();
		System.exit(0);
	}

	@Override
	public boolean isStarted() {
		return getWrapper().isStarted();
	}

	@Override
	public boolean isSuspended() {
		return getWrapper().isSuspended();
	}

	private NanoServiceWrapper getWrapper() {
		return wrapper;
	}
}
