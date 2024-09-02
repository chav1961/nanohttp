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
import java.util.function.BiFunction;
import java.util.function.Function;
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

import com.sun.net.httpserver.HttpExchange;

import chav1961.nanohttp.server.exceptions.RestServiceException;
import chav1961.nanohttp.server.interfaces.NanoClassSerializer;
import chav1961.nanohttp.server.interfaces.NanoContentSerializer;
import chav1961.purelib.basic.MimeType;
import chav1961.purelib.basic.URIUtils;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.exceptions.MimeParseException;

public class AnnotationParser<T> {
	private static final Pattern 				PARM_EXTRACTOR = Pattern.compile(".*\\{(.*)\\}");
	private static final MethodHandles.Lookup	LOOKUP = MethodHandles.lookup();
	private static final Set<Class<?>>			AVAILABLE_PARMS = Collections.unmodifiableSet(Set.of(UUID.class, Long.class, long.class, Integer.class, int.class, String.class));
	private static final String					CONTENT_ENCODING = "UTF-8";
	private static final MimeType[]				DEFAULT_MIMES = {MimeType.MIME_HTML_TEXT, MimeType.MIME_PLAIN_TEXT};
	private static final String					ACCEPT_HEADER = "Accept";

	private final T annotated;
	private final Class<T> annotatedClass;
	private final CallDescriptor[][] methods;
	private final String rootPath;
	
	public AnnotationParser(final T inst, final String rootPrefix) {
		if (inst == null) {
			throw new NullPointerException("Instance to parse can't be null");
		}
		else {
			this.annotated = inst;
			this.annotatedClass = (Class<T>) inst.getClass();
			this.rootPath = rootPrefix + (this.annotatedClass.isAnnotationPresent(Path.class) ? this.annotatedClass.getAnnotation(Path.class).value() : "");
			
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
								temp[rq.ordinal()].add(processDeleteRequest(m, rootPrefix));
								break;
							case GET 		:
								temp[rq.ordinal()].add(processGetRequest(m, rootPrefix));
								break;
							case POST 		:
								temp[rq.ordinal()].add(processPostRequest(m, rootPrefix));
								break;
							case PUT 		:
								temp[rq.ordinal()].add(processPutRequest(m, rootPrefix));
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
	
	public String getRootPath() {
		return rootPath;
	}

	public void processRequest(final HttpExchange ex, final Object... advanced) throws IOException, RestServiceException {
		if (ex == null) {
			throw new NullPointerException("Exchange can't be null");
		}
		else if (advanced == null || Utils.checkArrayContent4Nulls(advanced) >= 0) {
			throw new IllegalArgumentException("Advanced array is null or contains nulls inside");
		}
		else {
			final RequestType	type = RequestType.valueOf(ex.getRequestMethod());
			 
			for (CallDescriptor desc : methods[type.ordinal()]) {
				if (desc.matchAndCall(annotated, type, ex.getRequestURI(), ex, advanced)) {
					return;
				}
			}
			throw new RestServiceException(403);
		}
	}
	
	static CallDescriptor processGetRequest(final Method m, final String rootPath) {
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
					final String 		totalPath = rootPath + buildFullPath(m, path);
					final MethodType 	mt = MethodType.methodType(returned, (Class[])m.getParameterTypes());
					
					return new CallDescriptor(RequestType.GET, 
										totalPath, 
										processParameters(m, totalPath, false), 
										LOOKUP.findVirtual(m.getDeclaringClass(), m.getName(), mt), 
										returned == void.class || returned == Void.class, 
										findResponseSerializer(m));
				} catch (NoSuchMethodException | IllegalAccessException | MimeParseException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
	}

	static CallDescriptor processPostRequest(final Method m, final String rootPath) {
		final Class<?> returned = m.getReturnType();
		final String path = m.isAnnotationPresent(Path.class) ? m.getAnnotation(Path.class).value().trim() : "";
		
		if (path.isEmpty()) {
			throw new IllegalArgumentException("@Path annotation for method ["+m+"] is missing or contains empty string");
		}
		else {
			try {
				final String 		totalPath = rootPath + buildFullPath(m, path);
				final MethodType 	mt = MethodType.methodType(returned, (Class[])m.getParameterTypes());
				
				return new CallDescriptor(RequestType.POST, 
									totalPath, 
									processParameters(m, totalPath, true), 
									LOOKUP.findVirtual(m.getDeclaringClass(), m.getName(), mt), 
									returned == void.class || returned == Void.class, 
									findResponseSerializer(m));
			} catch (NoSuchMethodException | IllegalAccessException | MimeParseException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	static CallDescriptor processPutRequest(final Method m, final String rootPath) {
		final Class<?> returned = m.getReturnType();
		final String path = m.isAnnotationPresent(Path.class) ? m.getAnnotation(Path.class).value().trim() : "";
		
		if (path.isEmpty()) {
			throw new IllegalArgumentException("@Path annotation for method ["+m+"] is missing or contains empty string");
		}
		else {
			try {
				final String 		totalPath = rootPath + buildFullPath(m, path);
				final MethodType 	mt = MethodType.methodType(returned, (Class[])m.getParameterTypes());
				
				return new CallDescriptor(RequestType.PUT, 
									totalPath, 
									processParameters(m, totalPath, true), 
									LOOKUP.findVirtual(m.getDeclaringClass(), m.getName(), mt), 
									returned == void.class || returned == Void.class, 
									findResponseSerializer(m));
			} catch (NoSuchMethodException | IllegalAccessException | MimeParseException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	static CallDescriptor processDeleteRequest(final Method m, final String rootPath) {
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
					final String 		totalPath = rootPath + buildFullPath(m, path);
					final MethodType 	mt = MethodType.methodType(returned, (Class[])m.getParameterTypes());
					
					return new CallDescriptor(RequestType.DELETE, 
										totalPath, 
										processParameters(m, totalPath, false), 
										LOOKUP.findVirtual(m.getDeclaringClass(), m.getName(), mt), 
										returned == void.class || returned == Void.class, 
										findResponseSerializer(m));
				} catch (NoSuchMethodException | IllegalAccessException | MimeParseException e) {
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

	static Extractor[] processParameters(final Method m, final String path, final boolean hasBody) throws MimeParseException {
		final Extractor[] 	parms = new Extractor[m.getParameterCount()];
		final String[] 		pathParameters = extractPathParameters(path);
		
		for(int index = 0; index < parms.length; index++) {
			parms[index] = processParameter(m.getParameters()[index], m, path, pathParameters, hasBody);
		}
		return parms;
	}
	
	static Extractor processParameter(final Parameter parm, final Method m, final String path, final String[] pathParameters, final boolean hasBody) throws MimeParseException, IllegalArgumentException {
		final String 			value = parm.isAnnotationPresent(DefaultValue.class) ? parm.getAnnotation(DefaultValue.class).value() : null;
		final Class<?>			cl = parm.getType();
		final ParameterClass	pc = ParameterClass.forClass(cl);
		
		if (parm.isAnnotationPresent(QueryParam.class)) {
			final String name = parm.getAnnotation(QueryParam.class).value();
			
			switch (pc) {
				case INT : case LONG : case STRING : case UUID :
					return (rq,p,obj)->pc.cast(getOrDefault(URIUtils.parseQuery(rq.getRequestURI().getQuery()).get(name), value));
				case UNSUPPORTED	:
					throw new IllegalArgumentException("Illegal parameter class ["+cl.getCanonicalName()+"] for @QueryParam annotation, only UUID, Long/long, Integer/int and String classes are available"); 
				default:
					throw new UnsupportedOperationException("Parameter class ["+pc+"] is not supported yet");
			}
		}
		else if (parm.isAnnotationPresent(HeaderParam.class)) {
			final String name = parm.getAnnotation(HeaderParam.class).value();
			
			switch (pc) {
				case INT : case LONG : case STRING : case UUID :
					return (rq,p,obj)->pc.cast(getOrDefault(rq.getRequestHeaders().getFirst(name), value));
				case UNSUPPORTED	:
					throw new IllegalArgumentException("Illegal parameter class ["+cl.getCanonicalName()+"] for @HeaderParam annotation, only UUID, Long/long, Integer/int and String classes are available"); 
				default:
					throw new UnsupportedOperationException("Parameter class ["+pc+"] is not supported yet");
			}
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

						switch (pc) {
							case INT : case LONG : case STRING : case UUID :
								return (rq,p,obj)->pc.cast((p.split("/")[currentValue]));
							case UNSUPPORTED	:
								throw new IllegalArgumentException("Illegal parameter class ["+cl.getCanonicalName()+"] for @HeaderParam annotation, only UUID, Long/long, Integer/int and String classes are available"); 
							default:
								throw new UnsupportedOperationException("Parameter class ["+pc+"] is not supported yet");
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
		else if (hasBody) {
			final MimeType[]			mimes = m.isAnnotationPresent(Consumes.class) 
													? MimeType.parseMimes(m.getAnnotation(Consumes.class).value()) 
													: DEFAULT_MIMES;
			final NanoClassSerializer	serializer = findSerializer(cl, mimes); 
			
			return (rq, p, obj)->getValueByClass(rq, cl, obj, serializer);
		}
		else {
			return (rq, p, obj)->getValueByClass(rq, cl, obj);
		}
	}
	
	private static String buildFullPath(final Method m, final String path) {
		final String	tailPath = m.getDeclaringClass().isAnnotationPresent(Path.class) ? m.getDeclaringClass().getAnnotation(Path.class).value() : ""; 
		
		return tailPath+(tailPath.endsWith("/") ? path.substring(1) : path);
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
		throw new RestServiceException(415);
	}
	
	private static <T> T getValueByClass(final HttpExchange rq, final Class<T> cl, final Object[] content, final NanoClassSerializer serializer) throws IOException {
		for(int index = 0; index < content.length; index++) {
			if (cl.isInstance(content[index])) {
				return (T) content[index];
			}
		}
		return serializer.deserialize(rq.getRequestBody(), cl);
	}

	private static void sendResult(final HttpExchange ex, final int rc, Object result, final NanoClassSerializer ser, final OutputStream os) throws IOException, MimeParseException {
		final List<String>	mimes = ex.getRequestHeaders().get(ACCEPT_HEADER);
		final Class<?>		cl = result.getClass();

		for (MimeType mime : mimes != null ? MimeType.parseMimes(mimes) : DEFAULT_MIMES) {
			if (ser.canServe(mime, cl)) {
				ex.sendResponseHeaders(rc, 0);
				ser.serialize(result, os);
				return;
			}
		}
		throw new RestServiceException(500);
	}
	
	private static NanoClassSerializer findResponseSerializer(final Method m) throws MimeParseException {
		final MimeType[]	mimes = m.isAnnotationPresent(Produces.class) 
								? MimeType.parseMimes(m.getAnnotation(Produces.class).value()) 
								: DEFAULT_MIMES; 
		
		return findSerializer(m.getReturnType(), mimes); 
	}
	
	private static NanoClassSerializer findSerializer(final Class<?> clazz, final MimeType[] mimes) {
		for(NanoClassSerializer item : ServiceLoader.load(NanoClassSerializer.class)) {
			for (MimeType type : mimes) {
				if (item.canServe(type, clazz)) {
					return item;
				}
			}
		}
		throw new IllegalArgumentException("No serializer available for "+Arrays.toString(mimes)+" mime types and class ["+clazz.getCanonicalName()+"]. Use appropriative @Produces/@Consumes annotations for it");
	}

	private static enum ParameterClass {
		UUID((x, cl)->java.util.UUID.fromString(x), UUID.class),
		LONG((x, cl)->Long.valueOf(x), Long.class, long.class),
		INT((x, cl)->Integer.valueOf(x), Integer.class, int.class),
		STRING((x, cl)->x, String.class),
		UNSUPPORTED(null);
		
		private final Class<?>[] available;
		final BiFunction<String, Class<?>, ?> convertor;
		
		private ParameterClass(final BiFunction<String, Class<?>, ?> convertor, final Class<?>... available) {
			this.available = available;
			this.convertor = convertor;
		}
		
		public Class<?>[] getAvailableClasses() {
			return available;
		}
		
		public static ParameterClass forClass(final Class<?> awaited) {
			if (awaited == null) {
				throw new NullPointerException("Class to get parameter for can't be null");
			}
			else {
				for(ParameterClass item : ParameterClass.values()) {
					for (Class<?> cl : item.getAvailableClasses()) {
						if (cl == awaited) {
							return item;
						}
					}
				}
				return UNSUPPORTED;
			}
		}
		
		<T> T cast(final String value) {
			return (T) convertor.apply(value, available[0]); 
		}
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
					final Object 		result = mh.invokeWithArguments((Object[])passValues);
					
					switch (type) {
						case DELETE	:
							sendResponse(ex, 200, "");
							break;
						case GET	:
							if (result != null) {
								sendResult(ex, 200, result, responseSerializer, ex.getResponseBody());
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
								sendResult(ex, 201, result, responseSerializer, ex.getResponseBody());
							}
							else {
								sendResponse(ex, 201, "");
							}
							break;
						case PUT	:
							sendResult(ex, 200, result, responseSerializer, ex.getResponseBody());
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
					else if (e instanceof RestServiceException) {
						sendResponse(ex, ((RestServiceException)e).getResponseCode(), e.getMessage());
						return true;
					}
					else {
						throw new IOException("Method call: ["+mh+"] - "+e.getLocalizedMessage(), e);
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
