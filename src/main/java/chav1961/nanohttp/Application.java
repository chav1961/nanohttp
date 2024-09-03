package chav1961.nanohttp;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;

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
	public static final String		ARG_CONFIG_FILE = "conf";
	public static final String		JMX_NAME = "chav1961.nanohttp:type=basic,name=server";
	
	
	public static void main(String[] args) {
		final ApplicationArgParser		ap = new ApplicationArgParser();
		
		try{
			final ArgParser 				parsed = ap.parse(args); 
			final SubstitutableProperties	props = SubstitutableProperties.of(parsed.getValue(ARG_CONFIG_FILE, URI.class).toURL().openStream()); 
			final NanoServiceWrapper		wrapper = NanoServiceBuilder.of(props).build();
			final ObjectName 				jmxName = new ObjectName(JMX_NAME);

			if (!parsed.isTyped(ARG_MODE)) {
				final JmxManager		mgr = new JmxManager(wrapper);
				final MBeanServer 		server = ManagementFactory.getPlatformMBeanServer();
				
				server.registerMBean(mgr, jmxName);
				wrapper.start(); 
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

	private static VirtualMachine getVM() throws AttachNotSupportedException, IOException {
		final String	name = Application.class.getModule().getName()+"/"+Application.class.getCanonicalName();
				
		for(VirtualMachineDescriptor item : VirtualMachine.list()) {
			if (item.displayName().equals(name)) {
				return VirtualMachine.attach(item);
			}
		}
		throw new AttachNotSupportedException("No any nanohttp servers detected in this computer");
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new EnumArg<ModeList>(ARG_MODE, ModeList.class, false, true, "Service control mode. Must be used after service startup only. To startup service, do not type this argument"),
			new ConfigArg(ARG_CONFIG_FILE, true, false, "Config file location"),
			new FileArg(ARG_APP_DIR, true, true, "Application directory")
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
