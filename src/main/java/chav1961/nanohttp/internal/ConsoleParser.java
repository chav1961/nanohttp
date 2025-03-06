package chav1961.nanohttp.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chav1961.nanohttp.server.NanoServiceWrapper;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;

public class ConsoleParser {
	private static final Pattern	DEPLOY_PATTERN = Pattern.compile("\\s*deploy\\s*([a-zA-Z0-9\\._])\\s*to\\s*(([a-zA-Z0-9\\./_]))");
	private static final Pattern	UNDEPLOY_PATTERN = Pattern.compile("\\s*undeploy\\s*from\\s*([a-zA-Z0-9\\._])");
	private static final Pattern	LIST_DEPLOYMENTS_PATTERN = Pattern.compile("\\s*list\\s*");
	private static final Pattern	RESTART_SERVER_PATTERN = Pattern.compile("\\s*restart\\s*");
	private static final Pattern	START_SERVER_PATTERN = Pattern.compile("\\s*start\\s*");
	private static final Pattern	SUSPEND_SERVER_PATTERN = Pattern.compile("\\s*suspend\\s*");
	private static final Pattern	RESUME_SERVER_PATTERN = Pattern.compile("\\s*resume\\s*");
	private static final Pattern	STOP_SERVER_PATTERN = Pattern.compile("\\s*stop\\s*");
	private static final Pattern	STATE_PATTERN = Pattern.compile("\\s*state\\s*");
	private static final Pattern	HELP_PATTERN = Pattern.compile("\\s*help\\s*");
	
	private final NanoServiceWrapper	owner;
	
	public ConsoleParser(final NanoServiceWrapper owner) {
		if (owner == null) {
			throw new NullPointerException("Owner can't be null");
		}
		else {
			this.owner = owner;
		}
	}

	public void processConsoleInput(final String cmd) throws CommandLineParametersException {
		processConsoleInput(cmd, Thread.currentThread().getContextClassLoader());
	}
	
	public synchronized void processConsoleInput(final String cmd, final ClassLoader loader) throws IllegalArgumentException, NullPointerException, CommandLineParametersException {
		if (Utils.checkEmptyOrNullString(cmd)) {
			throw new IllegalArgumentException("Input string can't be null or empty");
		}
		else if (loader == null) {
			throw new NullPointerException("Class laoder can't be null");
		}
		else {
			final List<String>	parms = new ArrayList<>();
			
			try {
				if (isPattern(cmd, DEPLOY_PATTERN, parms)) {
					try {
						owner.deploy(parms.get(1), loader.loadClass(parms.get(0)));
						System.out.println("Deploy completed");
					} catch (ContentException | ClassNotFoundException e) {
						throw new CommandLineParametersException(e.getLocalizedMessage());
					}
				}
				else if (isPattern(cmd, UNDEPLOY_PATTERN, parms)) {
					owner.undeploy(parms.get(0));
					System.out.println("Undeploy completed");
				}
				else if (isPattern(cmd, LIST_DEPLOYMENTS_PATTERN, parms)) {
					final int[] 	count = new int[] {0};
					System.out.println("Deployed items:");
					owner.forEachDeployed((s,o)->{
						try {
							System.out.println("\t"+s+" ("+((o instanceof FileSystemInterface) ? "file system " + ((FileSystemInterface)o).getAbsoluteURI().toString() : "class "+o.getClass().getCanonicalName()) + ")");
							count[0]++;
						} catch (IOException e) {
							System.out.println("\t"+s+" (<I/O error on get>)");				
						}
					});
					System.out.println("Totally deployed ["+count[0]+"] items");
				}
				else if (isPattern(cmd, RESTART_SERVER_PATTERN, parms)) {
					if (!owner.isStarted()) {
						throw new CommandLineParametersException("Server is not started");
					}
					else {
						owner.stop();
						owner.start();
						System.out.println("Server restarted");
					}
				}
				else if (isPattern(cmd, START_SERVER_PATTERN, parms)) {
					if (owner.isStarted()) {
						throw new CommandLineParametersException("Server already started");
					}
					else {
						owner.start();
						System.out.println("Server started");				
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
						System.out.println("Server suspended");				
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
						System.out.println("Server resumed");				
					}
				}
				else if (isPattern(cmd, STOP_SERVER_PATTERN, parms)) {
					if (!owner.isStarted()) {
						throw new CommandLineParametersException("Server is not started");
					}
					else {
						owner.stop();
						System.out.println("Server stopped");
					}
				}
				else if (isPattern(cmd, STATE_PATTERN, parms)) {
					if (owner.isStarted()) {
						System.out.println("Server is started now");
						if (owner.isSuspended()) {
							System.out.println("Server is in suspended state");
						}
					}
					else {
						System.out.println("Server is not started now");
					}
				}
				else if (isPattern(cmd, HELP_PATTERN, parms)) {
					System.out.println("Command list:");
					System.out.println("\tdeploy <class> to <path>");
					System.out.println("\tundelpoy from <path>");
					System.out.println("\tlist");
					System.out.println("\tstart");
					System.out.println("\tsuspend");
					System.out.println("\tresume");
					System.out.println("\tstop");
					System.out.println("\trestart");
					System.out.println("\tstate");
				}
				else {
					throw new CommandLineParametersException("Command not recognized");
				}
			} catch (IOException e) {
				throw new CommandLineParametersException(e.getLocalizedMessage());
			}
		}
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
}
