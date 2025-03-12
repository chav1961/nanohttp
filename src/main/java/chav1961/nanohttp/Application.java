package chav1961.nanohttp;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.SecureRandom;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import chav1961.nanohttp.internal.ConsoleParser;
import chav1961.nanohttp.internal.DeploymentMode;
import chav1961.nanohttp.internal.DeploymentRepo;
import chav1961.nanohttp.internal.ModeList;
import chav1961.nanohttp.server.NanoServiceBuilder;
import chav1961.nanohttp.server.NanoServiceWrapper;
import chav1961.nanohttp.server.interfaces.NanoSPIPlugin;
import chav1961.nanohttp.server.jmx.JmxManager;
import chav1961.nanohttp.server.jmx.JmxManagerMBean;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.SystemErrLoggerFacade;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;

public class Application {
	public static final String			ARG_MODE = "mode";
	public static final String			ARG_APP_DIR = "appdir";
	public static final String			ARG_JMX_ENABLE = "jmx";
	public static final String			ARG_DEBUG = "d";
	public static final String			ARG_CONFIG_FILE = "conf";
	public static final String			JMX_NAME = "chav1961.nanohttp:type=basic,name=server";

	private static final String			DEPLOYER_NAME = "App dir deployer";
	private static final String			CONSOLE_DEPLOYER_NAME = "Console deploy thread";
	private static final SecureRandom	RANDOM = new SecureRandom();
	
	public static void main(String[] args) {
		final ApplicationArgParser		ap = new ApplicationArgParser();
		final File						tempDir = new File(new File(System.getProperty("java.io.tmpdir")), "nanohttp"+RANDOM.nextLong()+".tmp");
		
		try{
			final ArgParser 				parsed = ap.parse(args); 
			final SubstitutableProperties	props = SubstitutableProperties.of(parsed.getValue(ARG_CONFIG_FILE, URI.class).toURL().openStream()); 
			final ObjectName 				jmxName = new ObjectName(JMX_NAME);

			tempDir.mkdirs();			
			if (!parsed.isTyped(ARG_MODE)) {
				final LoggerFacade			logger = new SystemErrLoggerFacade();
				final NanoServiceWrapper	wrapper = NanoServiceBuilder.of(props).setTraceOn(parsed.getValue(ARG_DEBUG, boolean.class)).build(logger);
				final CountDownLatch		latch = new CountDownLatch(1);
				final ConsoleParser			cp = new ConsoleParser(wrapper, logger, wrapper.isTraceTurnedOn(), latch);
				final JmxManager			mgr = new JmxManager(wrapper, cp);
				final MBeanServer 			server = ManagementFactory.getPlatformMBeanServer();
				final Thread				deployThread = new Thread(()->processInput(wrapper, cp, System.in));

				Runtime.getRuntime().addShutdownHook(new Thread(()->{
					print("Termination request started, please wait...");
					terminate(wrapper);
				}));
				if (parsed.getValue(ARG_JMX_ENABLE, boolean.class)) {
					server.registerMBean(mgr, jmxName);
					if (wrapper.isTraceTurnedOn()) {
						wrapper.getLogger().message(Severity.debug, "JMX server started, JMX name is ["+JMX_NAME+"]");
					}
				}
				wrapper.start();
				
				walkServices(Thread.currentThread().getContextClassLoader(), (p)->deploy(cp, p, Thread.currentThread().getContextClassLoader()));

				deployThread.setName(CONSOLE_DEPLOYER_NAME);
				deployThread.setDaemon(true);
				deployThread.start();

				if (parsed.isTyped(ARG_APP_DIR)) {
					startAppDirListener(cp, wrapper.isTraceTurnedOn(), parsed.getValue(ARG_APP_DIR, File.class), tempDir);
					wrapper.getLogger().message(Severity.debug, "Application directory listener started");
				}
				
				try {
					latch.await();
				} catch (InterruptedException e) {
					throw new IOException(e);
				} finally {
					if (parsed.getValue(ARG_JMX_ENABLE, boolean.class)) {
						server.unregisterMBean(jmxName);
						if (wrapper.isTraceTurnedOn()) {
							wrapper.getLogger().message(Severity.debug, "JMX server closed");
						}
					}
					terminate(wrapper);
					System.in.close();
				}
			}
			else {
				final ModeList				mode = parsed.getValue(ARG_MODE, ModeList.class); 
				final VirtualMachine 		vm = getVM();
				final String 				jmxUrl = vm.startLocalManagementAgent();
				final JMXServiceURL 		url = new JMXServiceURL(jmxUrl);
				final JMXConnector 			connector = JMXConnectorFactory.connect(url);
				final MBeanServerConnection conn = connector.getMBeanServerConnection();				
				final JmxManagerMBean		mbeanProxy = JMX.newMBeanProxy(conn, jmxName, JmxManagerMBean.class, true);

				switch (mode) {
					case resume		:
						mbeanProxy.resume();
						break;
					case start		:
						mbeanProxy.start();
						break;
					case stop		:
						mbeanProxy.stop();
						break;
					case suspend	:
						mbeanProxy.suspend();
						break;
					case terminateAndExit	:
						mbeanProxy.terminateAndExit();
						break;
					default:
						throw new UnsupportedOperationException("Service mode ["+parsed.getValue(ARG_MODE, ModeList.class)+"] is not supported yet");
				}
				print("Command completed");
			}
		} catch (CommandLineParametersException exc) {
			print(exc.getLocalizedMessage());
			print(ap.getUsage("nanohttp"));
			System.exit(128);
		} catch (IOException | MalformedObjectNameException | InstanceNotFoundException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException | AttachNotSupportedException e) {
			e.printStackTrace();
			System.exit(129);
		} finally {
			Utils.deleteDir(tempDir);
		}
	}

	private static void deploy(final ConsoleParser cp, final NanoSPIPlugin p, final ClassLoader loader) {
		try {
			cp.processConsoleInput("deploy "+p.getPlugin().getClass().getName()+" to "+p.getPath(), loader);
		} catch (CommandLineParametersException e) {
			cp.getLogger().message(Severity.error, e.getLocalizedMessage());
		}
	}

	private static void undeploy(final ConsoleParser cp, final NanoSPIPlugin p, final ClassLoader loader) {
		try {
			cp.processConsoleInput("undeploy from "+p.getPath(), loader);
		} catch (CommandLineParametersException e) {
			cp.getLogger().message(Severity.error, e.getLocalizedMessage());
		}
	}
	
	private static void terminate(final NanoServiceWrapper wrapper) {
		try{if (wrapper.isStarted()) {
				wrapper.stop();
			}
			wrapper.close();
		} catch (IOException e) {
			wrapper.getLogger().message(Severity.error, e.getLocalizedMessage());
		}
	}
	
	private static void startAppDirListener(final ConsoleParser cp, final boolean traceRequired, final File dir, final File tempDir) throws IOException {
		final WatchService 		watcher = FileSystems.getDefault().newWatchService();
		final Thread			t = new Thread(()->{
									
									try (final DeploymentRepo	repo = new DeploymentRepo(dir, tempDir, cp.getLogger(), traceRequired)) {
										final File[]	content = dir.listFiles();
										WatchKey		key;
										
										if (content != null) {
											for(File f : content) {
												if (f.getName().endsWith(".jar")) {
													processDeployment(cp, repo, f, DeploymentMode.CREATE);
												}												
											}
										}
										
										while ((key = watcher.take()) != null) {
											for(WatchEvent<?> event : key.pollEvents()) {
												final WatchEvent<Path> ev = (WatchEvent<Path>)event;
												
												if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
													continue;
												}
												else {
													final File	f = ev.context().toFile();
													
													if (f.getName().endsWith(".jar")) {
														if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
															processDeployment(cp, repo, f, DeploymentMode.CREATE);
														}
														else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
															processDeployment(cp, repo, f, DeploymentMode.MODIFY);
														}
														else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
															processDeployment(cp, repo, f, DeploymentMode.DELETE);
														}
													}
												}
											}
											key.reset();
										}
									} catch (InterruptedException | IOException exc) {
										exc.printStackTrace();
									}
								});

		dir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		t.setName(DEPLOYER_NAME);
		t.setDaemon(true);
		t.start();					
	}
	
	private static void processDeployment(final ConsoleParser cp, final DeploymentRepo repo, final File file, final DeploymentMode mode) throws IOException {
		switch (mode) {
			case CREATE	:
				final ClassLoader	cla = repo.addClassLoader(file); 
				
				walkServices(cla, (p)->{deploy(cp, p, cla);});
				break;
			case DELETE	:
				final ClassLoader	cld = repo.getClassLoader(file);
				
				walkServices(cld, (p)->{undeploy(cp, p, cld);});
				repo.removeClassLoader(file);
				break;
			case MODIFY	:
				processDeployment(cp, repo, file, DeploymentMode.DELETE);
				processDeployment(cp, repo, file, DeploymentMode.CREATE);
				break;
			default:
				throw new UnsupportedOperationException("Deployment mode ["+mode+"] is not supported yet");
		}
	}

	private static void walkServices(final ClassLoader loader, final Consumer<NanoSPIPlugin> consumer) {
		for(NanoSPIPlugin item : ServiceLoader.<NanoSPIPlugin>load(NanoSPIPlugin.class, loader)) {
			consumer.accept(item);
		}
	}
	
	private static void processInput(final NanoServiceWrapper owner, final ConsoleParser cp, final InputStream in) {
		try(final Reader			rdr = new InputStreamReader(in);
			final BufferedReader	brdr = new BufferedReader(rdr)) {
			String 	line;
			
			while ((line = brdr.readLine()) != null) {
				final String	trimmed = line.trim();
				
				if (!Utils.checkEmptyOrNullString(trimmed)) {
					try {
						cp.processConsoleInput(line);
					} catch (CommandLineParametersException e) {
						owner.getLogger().message(Severity.error, e.getLocalizedMessage());
					}
				}
				else {
					print("Empty input, type command you wish or 'help'");
				}
			}
		} catch (IOException e) {
			owner.getLogger().message(Severity.error, e.getLocalizedMessage());
		}
	}

	private static VirtualMachine getVM() throws AttachNotSupportedException, IOException {
		final String	name = Application.class.getProtectionDomain().getCodeSource().getLocation().toString();
		final String	tail = name.substring(name.lastIndexOf('/')+1);
				
		for(VirtualMachineDescriptor item : VirtualMachine.list()) {
			if (item.displayName().startsWith(tail) && !modeListPresents(item.displayName())) {
				return VirtualMachine.attach(item);
			}
		}
		throw new AttachNotSupportedException("No any nanohttp servers detected in this computer");
	}

	private static boolean modeListPresents(final String displayName) {
		for(ModeList item : ModeList.values()) {
			if (displayName.contains(" "+item.name()+" ")) {
				return true;
			}
		}
		return false;
	}
	
	private static void print(final String message) {
		System.err.println(message);
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new EnumArg<ModeList>(ARG_MODE, ModeList.class, false, true, "Service control mode. Can be used after service startup only. To startup service, do not type this argument"),
			new ConfigArg(ARG_CONFIG_FILE, true, false, "Config file location. Can be absolute/relative file path or any URI"),
			new FileArg(ARG_APP_DIR, FileType.DIRECTORY_ONLY, false, false, "Application directory to keep plugins"),
			new BooleanArg(ARG_JMX_ENABLE, false, "Turn on JMX to control the service", false),
			new BooleanArg(ARG_DEBUG, false, "Turn on debug trace", false),
		};
		
		private ApplicationArgParser() {
			super(KEYS);
		}
		
		@Override
		protected String finalValidation(final ArgParser parser) throws CommandLineParametersException {
			final File	f = new File(parser.getValue(ARG_CONFIG_FILE, String.class));

			if (!f.exists() || !f.isFile() || !f.canRead()) {
				return "Config file ["+f.getAbsolutePath()+"] not exists, is not a file or can be accessed for you"; 
			}
			else {
				return super.finalValidation(parser);
			}
		}
	}
}
