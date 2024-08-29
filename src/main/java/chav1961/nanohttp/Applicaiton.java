package chav1961.nanohttp;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import chav1961.nanohttp.server.NanoServiceBuilder;
import chav1961.nanohttp.server.NanoServiceWrapper;
import chav1961.nanohttp.server.jmx.JmxManager;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

public class Applicaiton {
	public static final String		ARG_APP_DIR = "appdir";
	public static final String		ARG_CONFIG_FILE = "conf";
	public static final String		JMX_NAME = "chav1961.nanohttp:type=basic,name=server";
	
	
	public static void main(String[] args) {
		final ApplicationArgParser		ap = new ApplicationArgParser();
		
		try{
			final ArgParser 			parsed = ap.parse(args); 
			final SubstitutableProperties	props = SubstitutableProperties.of(parsed.getValue(ARG_CONFIG_FILE, URI.class).toURL().openStream()); 
			final NanoServiceWrapper	wrapper = NanoServiceBuilder.of(props).build();
			final JmxManager			mgr = new JmxManager(wrapper);
			final ObjectName 			jmxName = new ObjectName(JMX_NAME);
			final MBeanServer 			server = ManagementFactory.getPlatformMBeanServer();
			
			server.registerMBean(mgr, jmxName);
			wrapper.start();
		} catch (CommandLineParametersException exc) {
			System.err.println(exc.getLocalizedMessage());
			System.err.println(ap.getUsage("nanohttp"));
			System.exit(128);
		} catch (IOException | MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
			e.printStackTrace();
			System.exit(129);
		}
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
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
