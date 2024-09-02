package chav1961.nanohttp;

import com.sun.tools.attach.VirtualMachine;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final String	name = Test.class.getModule().getName()+"/"+Test.class.getCanonicalName();
		
		System.err.println("Name="+name);
		System.err.println(VirtualMachine.list());
		System.err.println("X="+VirtualMachine.list().get(0).displayName());
	}

}
