package chav1961.nanohttp.server.parser;

import com.google.gson.Gson;

public class Returned {
	static {
		Returned.class.getModule().addOpens(Returned.class.getPackageName(), Gson.class.getModule());
	}
	
	public String value;

	public Returned(final String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Returned other = (Returned) obj;
		if (value == null) {
			if (other.value != null) return false;
		} else if (!value.equals(other.value)) return false;
		return true;
	}

	@Override
	public String toString() {
		return "ReturnedClass [value=" + value + "]";
	}
}
