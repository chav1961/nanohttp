package chav1961.nanohttp;

import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;

public class Applicaiton {
	public static final String	ARG_APP_DIR = "appdir";
	public static final String	ARG_CONFIG_FILE = "conf";
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final ApplicationArgParser	ap = new ApplicationArgParser();
		
		try{
			final ArgParser 	parsed = ap.parse(args); 
			
			
		} catch (CommandLineParametersException exc) {
			System.err.println(exc.getLocalizedMessage());
			System.err.println(ap.getUsage("nanohttp"));
			System.exit(128);
		}
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new ConfigArg(ARG_CONFIG_FILE, true, false, "Config file location"),
			new FileArg(ARG_APP_DIR, true, true, "Applicaiton directory")
		};
		
		private ApplicationArgParser() {
			super(KEYS);
		}
	}
}
