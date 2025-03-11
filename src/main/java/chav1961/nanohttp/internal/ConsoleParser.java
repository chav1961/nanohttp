package chav1961.nanohttp.internal;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chav1961.nanohttp.server.interfaces.NanoService;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

public class ConsoleParser {
	private static final Pattern	DEPLOY_PATTERN = Pattern.compile("\\s*deploy\\s*(([a-zA-Z0-9\\._])+)\\s*to\\s*(([a-zA-Z0-9\\./_])+)");
	private static final Pattern	UNDEPLOY_PATTERN = Pattern.compile("\\s*undeploy\\s*from\\s*(([a-zA-Z0-9\\./_])+)");
	private static final Pattern	LIST_DEPLOYMENTS_PATTERN = Pattern.compile("\\s*list\\s*");
	private static final Pattern	RESTART_SERVER_PATTERN = Pattern.compile("\\s*restart\\s*");
	private static final Pattern	START_SERVER_PATTERN = Pattern.compile("\\s*start\\s*");
	private static final Pattern	SUSPEND_SERVER_PATTERN = Pattern.compile("\\s*suspend\\s*");
	private static final Pattern	RESUME_SERVER_PATTERN = Pattern.compile("\\s*resume\\s*");
	private static final Pattern	STOP_SERVER_PATTERN = Pattern.compile("\\s*stop\\s*");
	private static final Pattern	STATE_PATTERN = Pattern.compile("\\s*state\\s*");
	private static final Pattern	HELP_PATTERN = Pattern.compile("\\s*help\\s*");
	private static final Pattern	EXIT_PATTERN = Pattern.compile("\\s*exit\\s*");
	
	private final NanoService		owner;
	private final CountDownLatch	latch;
	
	public ConsoleParser(final NanoService owner, final CountDownLatch latch) {
		if (owner == null) {
			throw new NullPointerException("Owner can't be null");
		}
		else if (latch == null) {
			throw new NullPointerException("Latch can't be null");
		}
		else {
			this.owner = owner;
			this.latch = latch;
		}
	}

	public void processConsoleInput(final String cmd) throws CommandLineParametersException {
		processConsoleInput(cmd, getClassLoader(), false);
	}

	public void processConsoleInput(final String cmd, final boolean echo) throws CommandLineParametersException {
		processConsoleInput(cmd, getClassLoader(), echo);
	}
	
	public void processConsoleInput(final String cmd, final ClassLoader loader) throws IllegalArgumentException, NullPointerException, CommandLineParametersException {
		processConsoleInput(cmd, loader, false);
	}	
	
	public synchronized void processConsoleInput(final String cmd, final ClassLoader loader, final boolean echo) throws IllegalArgumentException, NullPointerException, CommandLineParametersException {
		if (Utils.checkEmptyOrNullString(cmd)) {
			throw new IllegalArgumentException("Input string can't be null or empty");
		}
		else if (loader == null) {
			throw new NullPointerException("Class loader can't be null");
		}
		else {
			final List<String>	parms = new ArrayList<>();
			
			try {
				if (echo) {
					printErr("> "+cmd);
				}
				
				if (isPattern(cmd, DEPLOY_PATTERN, parms)) {
					try {
						final String			path = parms.get(2); 
						final Class<?>			clazz = loader.loadClass(parms.get(0));
						final Constructor<?>	c = clazz.getConstructor();
						final Object			inst = c.newInstance();
						
						if (!path.startsWith("/")) {
							throw new CommandLineParametersException("Path to deploy must be started with '/'");
						}
						else {
							owner.deploy(path, inst);
							print("Deploy completed");
						}
					} catch (ContentException exc) {
						throw new CommandLineParametersException(exc.getLocalizedMessage());
					} catch (ClassNotFoundException exc) {
						throw new CommandLineParametersException("Class to deploy ["+parms.get(0)+"] not found anywhere");
					} catch (NoSuchMethodException | SecurityException exc) {
						throw new CommandLineParametersException("Class to deploy ["+parms.get(0)+"] doesn't have public default constructor");
					} catch (InstantiationException | IllegalAccessException | InvocationTargetException exc) {
						throw new CommandLineParametersException("Error creating instance for class ["+parms.get(0)+"]", exc);
					}
				}
				else if (isPattern(cmd, UNDEPLOY_PATTERN, parms)) {
					owner.undeploy(parms.get(0));
					print("Undeploy completed");
				}
				else if (isPattern(cmd, LIST_DEPLOYMENTS_PATTERN, parms)) {
					final int[] 	count = new int[] {0};
					
					print("Deployed items:");
					owner.forEachDeployed((s,o)->{
						try {
							print("\t"+s+" ("+((o instanceof FileSystemInterface) ? "file system " + ((FileSystemInterface)o).getAbsoluteURI().toString() : "class "+o.getClass().getCanonicalName()) + ")");
							count[0]++;
						} catch (IOException e) {
							print("\t"+s+" (<I/O error on get>)");				
						}
					});
					print("Totally deployed ["+count[0]+"] items");
				}
				else if (isPattern(cmd, RESTART_SERVER_PATTERN, parms)) {
					if (!owner.isStarted()) {
						throw new CommandLineParametersException("Server is not started");
					}
					else {
						owner.stop();
						owner.start();
						print("Server restarted");
					}
				}
				else if (isPattern(cmd, START_SERVER_PATTERN, parms)) {
					if (owner.isStarted()) {
						throw new CommandLineParametersException("Server already started");
					}
					else {
						owner.start();
						print("Server started");				
					}
				}
				else if (isPattern(cmd, SUSPEND_SERVER_PATTERN, parms)) {
					if (!owner.isStarted()) {
						throw new CommandLineParametersException("Server is not started");
					}
					else if (owner.isSuspended()) {
						throw new CommandLineParametersException("Server already suspended");
					}
					else {
						owner.suspend();
						print("Server suspended");				
					}
				}
				else if (isPattern(cmd, RESUME_SERVER_PATTERN, parms)) {
					if (!owner.isStarted()) {
						throw new CommandLineParametersException("Server is not started");
					}
					else if (!owner.isSuspended()) {
						throw new CommandLineParametersException("Server not suspended");
					}
					else {
						owner.resume();
						print("Server resumed");				
					}
				}
				else if (isPattern(cmd, STOP_SERVER_PATTERN, parms)) {
					if (!owner.isStarted()) {
						throw new CommandLineParametersException("Server is not started");
					}
					else {
						owner.stop();
						print("Server stopped");
					}
				}
				else if (isPattern(cmd, STATE_PATTERN, parms)) {
					if (owner.isStarted()) {
						print("Server is started now");
						if (owner.isSuspended()) {
							print("Server is in suspended state");
						}
					}
					else {
						print("Server is not started now");
					}
				}
				else if (isPattern(cmd, EXIT_PATTERN, parms)) {
					print("Completed");
					latch.countDown();
				}
				else if (isPattern(cmd, HELP_PATTERN, parms)) {
					print("Command list:");
					print("\tdeploy <class> to <path>");
					print("\tundelpoy from <path>");
					print("\tlist");
					print("\tstart");
					print("\tsuspend");
					print("\tresume");
					print("\tstop");
					print("\trestart");
					print("\tstate");
					print("\texit");
				}
				else {
					printErr("Command not recognized");
					throw new CommandLineParametersException("Command not recognized");
				}
			} catch (IOException | RuntimeException e) {
				printErr(e.getLocalizedMessage());
				throw new CommandLineParametersException(e.getLocalizedMessage());
			}
		}
	}
	
	protected void print(final String content) {
		System.out.println(content);
	}

	protected void printErr(final String content) {
		System.err.println(content);
	}
	
	private boolean isPattern(final String cmd, final Pattern pattern, final List<String> groups) {
		final Matcher	m = pattern.matcher(cmd);
		
		if (!m.find()) {
			return false;
		}
		else {
			groups.clear();
			for(int index = 1; index <= m.groupCount(); index++) {
				groups.add(m.group(index));
			}
			return true;
		}
	}
	
	private ClassLoader getClassLoader() {
		ClassLoader	result = Thread.currentThread().getContextClassLoader();
		
		if (result == null) {
			result = this.getClass().getClassLoader();
		}
		return result;
	}
}
