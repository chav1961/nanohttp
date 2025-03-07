package chav1961.nanohttp.server.jmx;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import chav1961.nanohttp.internal.ConsoleParser;
import chav1961.nanohttp.server.NanoServiceWrapper;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

public class JmxManager implements JmxManagerMBean {
	private final NanoServiceWrapper	wrapper;
	private final ConsoleParser			parser;
	
	public JmxManager(final NanoServiceWrapper wrapper, final ConsoleParser parser) {
		if (wrapper == null) {
			throw new NullPointerException("Wrapper can't be null");
		}
		else if (parser == null) {
			throw new NullPointerException("Console parser can't be null");
		}
		else {
			this.wrapper = wrapper;
			this.parser = parser;
		}
	}
	
	@Override
	public void start() throws IOException {
		try {
			parser.processConsoleInput("start", true);
		} catch (CommandLineParametersException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	@Override
	public void suspend() throws IOException {
		try {
			parser.processConsoleInput("suspend", true);
		} catch (CommandLineParametersException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	@Override
	public void resume() throws IOException {
		try {
			parser.processConsoleInput("resume", true);
		} catch (CommandLineParametersException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	@Override
	public void stop() throws IOException {
		try {
			parser.processConsoleInput("stop", true);
		} catch (CommandLineParametersException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	@Override
	public void terminateAndExit() throws IOException {
		try {
			parser.processConsoleInput("exit", true);
		} catch (CommandLineParametersException e) {
			throw new IOException(e.getLocalizedMessage());
		}
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
