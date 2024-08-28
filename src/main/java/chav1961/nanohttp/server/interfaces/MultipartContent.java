package chav1961.nanohttp.server.interfaces;

import java.io.InputStream;
import java.util.Properties;

public interface MultipartContent {
	int getPartCount();
	String[] getPartNames();
	boolean hasPartName(String partName);
	Properties getPartProperties(String partName);
	InputStream getPartContent(String partName);
}
