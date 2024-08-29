package chav1961.nanohttp.server.parser;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import com.sun.net.httpserver.HttpExchange;

import chav1961.nanohttp.server.interfaces.NanoClassSerializer;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.Utils;

public class AnnotationParser<T> {
	private static final Pattern 				PARM_EXTRACTOR = Pattern.compile(".*\\{(.*)\\}");
	private static final MethodHandles.Lookup	LOOKUP = MethodHandles.publicLookup();
	private static final Set<Class<?>>			AVAILABLE_PARMS = Collections.unmodifiableSet(Set.of(UUID.class, Long.class, long.class, Integer.class, int.class, String.class));
	private static final String					CONTENT_ENCODING = "UTF-8";

	private final T annotated;
	private final Class<T> annotatedClass;
	private final CallDescriptor[][] methods;
	private final String rootPath;
	
	public AnnotationParser(final T inst) {
		if (inst == null) {
			throw new NullPointerException("Instance to parse can't be null");
		}
		else {
			this.annotated = inst;
			this.annotatedClass = (Class<T>) inst.getClass();
			this.rootPath = this.annotatedClass.isAnnotationPresent(Path.class) ? this.annotatedClass.getAnnotation(Path.class).value() : "";
			
			final List<CallDescriptor>[]	temp = new List[RequestType.values().length];
			
			for(int index = 0; index < temp.length; index++) {
				temp[index] = new ArrayList<>();
			}
			boolean		anyFound = false;
			
			for(Method m : this.annotatedClass.getMethods()) {
				for (RequestType rq : RequestType.values()) {
					if (m.isAnnotationPresent(rq.getAnnotationAssociated())) {
						switch (rq) {
							case DELETE 	:
								temp[rq.ordinal()].add(processDeleteRequest(m));
								break;
							case GET 		:
								temp[rq.ordinal()].add(processGetRequest(m));
								break;
							case POST 		:
								temp[rq.ordinal()].add(processPostRequest(m));
								break;
							case PUT 		:
								temp[rq.ordinal()].add(processPutRequest(m));
								break;
							case HEAD		:
								temp[rq.ordinal()].add(processHeadRequest(m));
								break;
							case OPTIONS	:
								temp[rq.ordinal()].add(processOptionsRequest(m));
								break;
							default:
								throw new UnsupportedOperationException("Request type ["+rq+"] is not supported yet");
						}
						anyFound = true;
					}
				}
			}
			if (!anyFound) {
				throw new IllegalArgumentException("Class ["+this.annotatedClass.getCanonicalName()+"] doesn't have any methods marked with @GET, @POST, @PUT, @DELETE, @HEAD or @OPTIONS");
			}
			else {
				this.methods = new CallDescriptor[temp.length][];
				
				for(int index = 0; index < this.methods.length; index++) {
					this.methods[index] = temp[index].toArray(new CallDescriptor[temp[index].size()]);
				}
			}
		}
	}

	public void processRequest(final HttpExchange ex, final Object... advanced) throws IOException, WebApplicationException {
		if (ex == null) {
			throw new NullPointerException("Exchange can't be null");
		}
		else if (advanced == null || Utils.checkArrayContent4Nulls(advanced) >= 0) {
			throw new IllegalArgumentException("Advanced array is null or contains nulls inside");
		}
		else {
			final RequestType	type = RequestType.valueOf(ex.getRequestMethod());
			
			for (CallDescriptor desc : methods[type.ordinal()]) {
				if (desc.matchAndCall(desc, type, ex.getRequestURI(), ex, advanced)) {
					return;
				}
			}
			throw new NotFoundException();
		}
	}
	
	static CallDescriptor processGetRequest(final Method m) {
		final Class<?> returned = m.getReturnType();
		
		if (returned == void.class) {
			throw new IllegalArgumentException("@GET annotation for void method ["+m+"] is not allowed");
		}
		else {
			final String path = m.isAnnotationPresent(Path.class) ? m.getAnnotation(Path.class).value().trim() : "";
			
			if (path.isEmpty()) {
				throw new IllegalArgumentException("@Path annotation for method ["+m+"] is missing or contains empty string");
			}
			else {
				try {
					final String 		totalPath = buildFullPath(m, path);
					final MethodType 	mt = MethodType.methodType(returned, (Class[])m.getParameterTypes());
					
					return new CallDescriptor(RequestType.GET, 
										totalPath, 
										processParameters(m, totalPath, false), 
										LOOKUP.findVirtual(m.getDeclaringClass(), m.getName(), mt), 
										returned == void.class || returned == Void.class, 
										findResponseSerializer(m));
				} catch (NoSuchMethodException | IllegalAccessException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
	}

	static CallDescriptor processPostRequest(final Method m) {
		final Class<?> returned = m.getReturnType();
		final String path = m.isAnnotationPresent(Path.class) ? m.getAnnotation(Path.class).value().trim() : "";
		
		if (path.isEmpty()) {
			throw new IllegalArgumentException("@Path annotation for method ["+m+"] is missing or contains empty string");
		}
		else {
			try {
				final String 		totalPath = buildFullPath(m, path);
				final MethodType 	mt = MethodType.methodType(returned, (Class[])m.getParameterTypes());
				
				return new CallDescriptor(RequestType.POST, 
									totalPath, 
									processParameters(m, totalPath, true), 
									LOOKUP.findVirtual(m.getDeclaringClass(), m.getName(), mt), 
									returned == void.class || returned == Void.class, 
									findResponseSerializer(m));
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	static CallDescriptor processPutRequest(final Method m) {
		final Class<?> returned = m.getReturnType();
		final String path = m.isAnnotationPresent(Path.class) ? m.getAnnotation(Path.class).value().trim() : "";
		
		if (path.isEmpty()) {
			throw new IllegalArgumentException("@Path annotation for method ["+m+"] is missing or contains empty string");
		}
		else {
			try {
				final String 		totalPath = buildFullPath(m, path);
				final MethodType 	mt = MethodType.methodType(returned, (Class[])m.getParameterTypes());
				
				return new CallDescriptor(RequestType.PUT, 
									totalPath, 
									processParameters(m, totalPath, true), 
									LOOKUP.findVirtual(m.getDeclaringClass(), m.getName(), mt), 
									returned == void.class || returned == Void.class, 
									findResponseSerializer(m));
			} catch (NoSuchMethodException | IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	static CallDescriptor processDeleteRequest(Method m) {
		final Class<?> returned = m.getReturnType();
		
		if (returned != void.class) {
			throw new IllegalArgumentException("@DELETE annotation can be used for for void method ["+m+"]");
		}
		else {
			final String path = m.isAnnotationPresent(Path.class) ? m.getAnnotation(Path.class).value().trim() : "";
			
			if (path.isEmpty()) {
				throw new IllegalArgumentException("@Path annotation for method ["+m+"] is missing or contains empty string");
			}
			else {
				try {
					final String 		totalPath = buildFullPath(m, path);
					final MethodType 	mt = MethodType.methodType(returned, (Class[])m.getParameterTypes());
					
					return new CallDescriptor(RequestType.DELETE, 
										totalPath, 
										processParameters(m, totalPath, false), 
										LOOKUP.findVirtual(m.getDeclaringClass(), m.getName(), mt), 
										returned == void.class || returned == Void.class, 
										findResponseSerializer(m));
				} catch (NoSuchMethodException | IllegalAccessException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
	}

	static CallDescriptor processHeadRequest(final Method m) {
		throw new UnsupportedOperationException("@HEAD request is not implemented yet");
	}

	static CallDescriptor processOptionsRequest(final Method m) {
		throw new UnsupportedOperationException("@OPTIONS request is not implemented yet");
	}

	static Extractor[] processParameters(final Method m, final String path, final boolean hasBody) {
		final Extractor[] 	parms = new Extractor[m.getParameterCount()];
		final String[] 		pathParameters = extractPathParameters(path);
		
		for(int index = 0; index < parms.length; index++) {
			parms[index] = processParameter(m.getParameters()[index], m, path, pathParameters, hasBody);
		}
		return parms;
	}
	
	static Extractor processParameter(final Parameter parm, final Method m, final String path, final String[] pathParameters, final boolean hasBody) {
		final String value = parm.isAnnotationPresent(DefaultValue.class) ? parm.getAnnotation(DefaultValue.class).value() : null;
		
		if (parm.isAnnotationPresent(QueryParam.class)) {
			final String name = parm.getAnnotation(QueryParam.class).value();
			
			return (rq,p,obj)->getOrDefault(URIUtils.parseQuery(rq.getRequestURI().getQuery()).get(name), value);
		}
		else if (parm.isAnnotationPresent(HeaderParam.class)) {
			final String name = parm.getAnnotation(HeaderParam.class).value();
			
			return (rq,p,obj)->getOrDefault(rq.getRequestHeaders().getFirst(name), value);
		}
		else if (parm.isAnnotationPresent(PathParam.class)) {
			final String name = parm.getAnnotation(PathParam.class).value();
			
			for(String parmName : pathParameters) {
				if (parmName.equals(name)) {
					if (!AVAILABLE_PARMS.contains(parm.getType())) {
						throw new IllegalArgumentException("@PathParam annotation for ["+parm+"] must have "+AVAILABLE_PARMS+" class only");
					}
					else {
						final int currentValue = pathTemplate2Index(path, name);
						
						if (parm.getType() == UUID.class) {
							return (rq,p,obj)->UUID.fromString(p.split("/")[currentValue]);
						}
						else if (parm.getType() == long.class || parm.getType() == Long.class) {
							return (rq,p,obj)->Long.valueOf(p.split("/")[currentValue]);
						}
						else if (parm.getType() == int.class || parm.getType() == Integer.class) {
							return (rq,p,obj)->Integer.valueOf(p.split("/")[currentValue]);
						}
						else {
							return (rq,p,obj)->p.split("/")[currentValue];
						}
					}
				}
			}
			throw new IllegalArgumentException("@PathParam annotation for ["+parm+"] references to unknown path item name ["+name+"]");
		}
		else if (parm.isAnnotationPresent(BeanParam.class)) {
			throw new UnsupportedOperationException("@BeanParam option is not implemented yet");
		}
		else if (parm.isAnnotationPresent(CookieParam.class)) {
			throw new UnsupportedOperationException("@CookieParam option is not implemented yet");
		}
		else if (parm.isAnnotationPresent(FormParam.class)) {
			throw new UnsupportedOperationException("@FormParam option is not implemented yet");
		}
		else if (parm.isAnnotationPresent(MatrixParam.class)) {
			throw new UnsupportedOperationException("@MatrixParam option is not implemented yet");
		}
		else {
			final Class<?> cl = parm.getType();
			
			if (hasBody) {
				final NanoClassSerializer	serializer = findSerializer(cl, m.isAnnotationPresent(Consumes.class) ? m.getAnnotation(Consumes.class).value() : new String[] {"text/plain", "text/html"}); 
				
				return (rq, p, obj)->getValueByClass(rq, cl, obj, serializer);
			}
			else {
				return (rq, p, obj)->getValueByClass(rq, cl, obj);
			}
		}
	}
	
	private static String buildFullPath(final Method m, final String path) {
		final String	rootPath = m.getDeclaringClass().isAnnotationPresent(Path.class) ? m.getDeclaringClass().getAnnotation(Path.class).value() : ""; 
		
		return rootPath+(rootPath.endsWith("/") ? path.substring(1) : path);
	}

	private static String[] extractPathParameters(final String path) {
		final List<String> result = new ArrayList<>();
		final Matcher m = PARM_EXTRACTOR.matcher(path);
		
		while (m.find()) {
			result.add(m.group(1));
		}
		return result.toArray(new String[result.size()]);
	}

	private static int pathTemplate2Index(final String path, final String parmName) {
		final String temp = "{"+parmName+"}";
		final String[] pieces = path.split("/");
		
		for(int index = 0; index < pieces.length; index++) {
			if (pieces[index].length() > 0 && pieces[index].charAt(0) == '{' && pieces[index].equals(temp)) {
				return index;
			}
		}
		throw new IllegalArgumentException("Path parameter name ["+parmName+"] not found in the path ["+path+"]");
	}
	
	private static String getOrDefault(final String[] value, final String defaultValue) {
		return value == null || value.length == 0 ? defaultValue : value[0];
	}
	
	private static String getOrDefault(final String value, final String defaultValue) {
		return value == null ? defaultValue : value;
	}

	private static void sendResponse(final HttpExchange ex, final int rc, final String message) throws IOException {
		final byte[]	content = message.getBytes(CONTENT_ENCODING);
		
		ex.getResponseHeaders().add("Content-Type", "text/plain; charset="+CONTENT_ENCODING);
		ex.sendResponseHeaders(rc, content.length);
		try(final OutputStream	os = ex.getResponseBody()) {
			os.write(content);
		}
	}

	private static <T> T getValueByClass(final HttpExchange rq, final Class<T> cl, final Object[] content) throws IOException {
		for(int index = 0; index < content.length; index++) {
			if (cl.isInstance(content[index])) {
				return (T) content[index];
			}
		}
		throw new IllegalArgumentException("Class ["+cl+"] can't be found anywhere and no body in the request");
	}
	
	private static <T> T getValueByClass(final HttpExchange rq, final Class<T> cl, final Object[] content, final NanoClassSerializer serializer) throws IOException {
		for(int index = 0; index < content.length; index++) {
			if (cl.isInstance(content[index])) {
				return (T) content[index];
			}
		}
		return serializer.deserialize(rq.getRequestBody(), cl);
	}

	private static void sendResult(HttpExchange ex, final int rc, Object result) {
		// TODO Auto-generated method stub
		
	}
	
	private static NanoClassSerializer findResponseSerializer(final Method m) {
		return findSerializer(m.getReturnType(), m.isAnnotationPresent(Produces.class) ? m.getAnnotation(Produces.class).value() : new String[] {"text/plain", "text/html"}); 
	}
	
	private static NanoClassSerializer findSerializer(final Class<?> clazz, final String[] mimes) {
		for(NanoClassSerializer item : ServiceLoader.load(NanoClassSerializer.class)) {
			for (String type : mimes) {
				if (item.canServe(type, clazz)) {
					return item;
				}
			}
		}
		throw new IllegalArgumentException("No serializer available for "+Arrays.toString(mimes)+" mime types");
	}
	
	private static class CallDescriptor {
		final RequestType 		type;
		final String[] 			pathTemplate;
		final MethodHandle 		mh;
		final Extractor[] 		parms;
		final boolean 			noResponse;
		final NanoClassSerializer	responseSerializer;
		
		private CallDescriptor(final RequestType type, final String pathTemplate, final Extractor[] parms, final MethodHandle mh, final boolean noResponse, final NanoClassSerializer responseSerializer) {
			this.type = type;
			this.pathTemplate = pathTemplate.split("/");
			this.mh = mh;
			this.parms = parms;
			this.noResponse = noResponse;
			this.responseSerializer = responseSerializer;
		}
		
		private boolean matchAndCall(final Object inst, final RequestType type, final URI uri, final HttpExchange ex, final Object... advanced) throws IOException {
			final String	path = uri.getPath();
			
			if (matches(path)) {
				final Object[] passValues = new Object[parms.length + 1];
				
				passValues[0] = inst;
				for(int index = 0; index < parms.length; index++) {
					passValues[index + 1] = parms[index].extract(ex, path, advanced);
				}
				try {
					System.err.println("--- CALL "+mh);
					final Object result = mh.invokeWithArguments((Object[])passValues);
					
					switch (type) {
						case DELETE	:
							sendResult(ex, 200, result);
							break;
						case GET	:
							if (result != null) {
								sendResult(ex, 200, result);
							}
							else {
								sendResponse(ex, 404, "");
							}
							break;
						case POST	:
							if (noResponse) {
								sendResponse(ex, 204, "");
							}
							else if (result != null) {
								sendResult(ex, 201, result);
							}
							else {
								sendResponse(ex, 201, "");
							}
							break;
						case PUT	:
							sendResult(ex, 200, result);
							break;
						case HEAD : case OPTIONS :
						default:
							throw new UnsupportedOperationException("Request type ["+type+"] is not supported yet");
					}
					return true;
				} catch (Throwable e) { 
					if (e instanceof ProcessingException) {
						sendResponse(ex, 415, e.getMessage());
						return true;
					}
					else if (e instanceof WebApplicationException) {
						sendResponse(ex, ((WebApplicationException)e).getResponse().getStatus(), e.getMessage());
						return true;
					}
					else {
						throw new IOException(e);
					}
				}
			}
			else {
				return false;
			}
		}
		
		private boolean matches(final String path) {
			final String[] piece = path.split("/");
			final String[] template = pathTemplate;
			
			System.err.println("--- Matches "+path+" and "+String.join("/", template));
			
			if (piece.length != template.length) {
				return false;
			}
			else {
				for(int index = 0; index < piece.length; index++) {
					if (template[index].length() > 0) {
						if (template[index].charAt(0) == '{') {
							if (!isUUID(piece[index]) && !isLong(piece[index]) && !isString(piece[index]) ) {
								return false;
							}
						}
						else if (!piece[index].equals(template[index])) {
							return false;
						}
					}
				}
				return true;
			}
		}
		
		private boolean isUUID(final CharSequence seq) {
			if (seq.length() != 36 || seq.charAt(8) != '-' || seq.charAt(13) != '-' || seq.charAt(18) != '-' || seq.charAt(23) != '-') {
				return false;
			}
			else {
				return true;
			}
		}

		private boolean isLong(final CharSequence seq) {
			for(int index = 0, maxIndex = seq.length(); index < maxIndex; index++) {
				if (!Character.isDigit(seq.charAt(index))) {
					return false;
				}
			}
			return true;
		}

		private boolean isString(final CharSequence seq) {
			return !seq.isEmpty();
		}
	}
	
	@FunctionalInterface
	private static interface Extractor {
		Object extract(HttpExchange rq, String path, Object... parameter) throws IOException;
	}
}
