package chav1961.nanohttp;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import javax.management.InstanceAlreadyExistsException;
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
import chav1961.nanohttp.internal.ModeList;
import chav1961.nanohttp.server.NanoServiceBuilder;
import chav1961.nanohttp.server.NanoServiceWrapper;
import chav1961.nanohttp.server.jmx.JmxManager;
import chav1961.nanohttp.server.jmx.JmxManagerMBean;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

public class Application {
	public static final String		ARG_MODE = "mode";
	public static final String		ARG_APP_DIR = "appdir";
	public static final String		ARG_DEBUG = "d";
	public static final String		ARG_CONFIG_FILE = "conf";
	public static final String		JMX_NAME = "chav1961.nanohttp:type=basic,name=server";

	public static void main(String[] args) {
		final ApplicationArgParser		ap = new ApplicationArgParser();
		
		try{
			final ArgParser 				parsed = ap.parse(args); 
			final SubstitutableProperties	props = SubstitutableProperties.of(parsed.getValue(ARG_CONFIG_FILE, URI.class).toURL().openStream()); 
			final ObjectName 				jmxName = new ObjectName(JMX_NAME);

			if (!parsed.isTyped(ARG_MODE)) {
				final NanoServiceWrapper	wrapper = NanoServiceBuilder.of(props).setTraceOn(parsed.getValue(ARG_DEBUG, boolean.class)).build();
				final CountDownLatch		latch = new CountDownLatch(1);
				final JmxManager			mgr = new JmxManager(wrapper, latch);
				final MBeanServer 			server = ManagementFactory.getPlatformMBeanServer();
				final Thread				deployThread = new Thread(()->processInput(wrapper, System.in));
				
				server.registerMBean(mgr, jmxName);
				Runtime.getRuntime().addShutdownHook(new Thread(()->{
					try{wrapper.stop();
						wrapper.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}));
				wrapper.start();
				deployThread.setDaemon(true);
				deployThread.start();
				
				try {
					latch.await();
				} catch (InterruptedException e) {
					throw new IOException(e);
				} finally {
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
				System.err.println("Command completed");
			}
		} catch (CommandLineParametersException exc) {
			System.err.println(exc.getLocalizedMessage());
			System.err.println(ap.getUsage("nanohttp"));
			System.exit(128);
		} catch (IOException | MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException | AttachNotSupportedException e) {
			e.printStackTrace();
			System.exit(129);
		}
	}

	private static void processInput(final NanoServiceWrapper owner, final InputStream in) {
		try(final Reader			rdr = new InputStreamReader(in);
			final BufferedReader	brdr = new BufferedReader(rdr)) {
			final ConsoleParser		cp = new ConsoleParser(owner);
			String 	line;
			
			while ((line = brdr.readLine())  != null) {
				try {
					cp.processConsoleInput(line);
				} catch (CommandLineParametersException e) {
					System.err.println(e.getLocalizedMessage());
				}
			}
		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
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

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new EnumArg<ModeList>(ARG_MODE, ModeList.class, false, true, "Service control mode. Must be used after service startup only. To startup service, do not type this argument"),
			new ConfigArg(ARG_CONFIG_FILE, true, false, "Config file location. Can be absolute/relative file path or any URI"),
			new FileArg(ARG_APP_DIR, true, true, "Application directory"),
			new BooleanArg(ARG_DEBUG, false, "Turn on debug trace", false)
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
