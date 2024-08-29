package chav1961.nanohttp.server.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import chav1961.nanohttp.server.interfaces.NanoClassSerializer;

public class MultipartClassSerializer implements NanoClassSerializer {

	@Override
	public String[] getMimeTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> boolean canServe(String mime, Class<T> awaited) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> T deserialize(InputStream is, Class<T> awaited) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> void serialize(T content, OutputStream os) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
