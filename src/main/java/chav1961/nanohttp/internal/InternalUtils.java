package chav1961.nanohttp.internal;

import chav1961.purelib.basic.Utils;

public class InternalUtils {
	public static String buildRootPrefix(final String path2deploy, final String pathAnnotation) {
		if (Utils.checkEmptyOrNullString(path2deploy)) {
			throw new IllegalArgumentException("Path to deploy can't be null or empty");
		}
		else if (Utils.checkEmptyOrNullString(pathAnnotation)) {
			return path2deploy;
		}
		else if (path2deploy.endsWith("/") && pathAnnotation.startsWith("/")) {
			return path2deploy+pathAnnotation.substring(1);
		}
		else {
			return path2deploy+pathAnnotation;
		}
	}
	
	public static String[] splitRequestPath(final String path) {
		if (Utils.checkEmptyOrNullString(path)) {
			throw new IllegalArgumentException("Path can't be null or empty");
		}
		else {
			String	temp = path;
			
			while (temp.startsWith("/")) {
				temp = temp.substring(1);
			}
			return temp.split("/");
		}
	}
}
