package chav1961.nanohttp.server.serializers;


import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import chav1961.nanohttp.server.NanoServiceBuilder;
import chav1961.nanohttp.server.interfaces.NanoContentSerializer;
import chav1961.purelib.basic.MimeType;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.EnvironmentException;
import chav1961.purelib.enumerations.MarkupOutputFormat;
import chav1961.purelib.streams.char2char.CreoleWriter;

public class TextContentSerializer implements NanoContentSerializer {
	private static final MimeType[]	MIMES = {MimeType.MIME_HTML_TEXT, MimeType.MIME_MARKDOWN_TEXT, MimeType.MIME_CREOLE_TEXT};
	private static final List<PairDescriptor> CONVERTORS = new ArrayList<>();

	static {
		CONVERTORS.add(new PairDescriptor(MimeType.MIME_CREOLE_TEXT, MimeType.MIME_HTML_TEXT, (os,prop)->creole2Html(os,prop)));
		CONVERTORS.add(new PairDescriptor(MimeType.MIME_CREOLE_TEXT, MimeType.MIME_PLAIN_TEXT, (os,prop)->creole2Plain(os,prop)));
		CONVERTORS.add(new PairDescriptor(MimeType.MIME_CREOLE_TEXT, MimeType.MIME_ANY_TEXT, (os,prop)->creole2Plain(os,prop)));

		CONVERTORS.add(new PairDescriptor(MimeType.MIME_MARKDOWN_TEXT, MimeType.MIME_HTML_TEXT, (os,prop)->markdown2Html(os,prop)));
		CONVERTORS.add(new PairDescriptor(MimeType.MIME_MARKDOWN_TEXT, MimeType.MIME_PLAIN_TEXT, (os,prop)->markdown2Plain(os,prop)));
		CONVERTORS.add(new PairDescriptor(MimeType.MIME_MARKDOWN_TEXT, MimeType.MIME_ANY_TEXT, (os,prop)->markdown2Plain(os,prop)));

		CONVERTORS.add(new PairDescriptor(MimeType.MIME_HTML_TEXT, MimeType.MIME_PLAIN_TEXT, (os,prop)->html2Plain(os,prop)));
		CONVERTORS.add(new PairDescriptor(MimeType.MIME_HTML_TEXT, MimeType.MIME_ANY_TEXT, (os,prop)->html2Plain(os,prop)));
	}
	
	@Override
	public MimeType[] getMimeTypes() {
		return MIMES;
	}

	@Override
	public boolean canServe(final MimeType mimeFrom, final MimeType mimeTo) {
		if (mimeFrom == null) {
			throw new NullPointerException("From MIME type can't be null");
		}
		else if (mimeTo == null) {
			throw new NullPointerException("To MIME type can't be null");
		}
		else {
			for(PairDescriptor item : CONVERTORS) {
				if (item.sourceMime.equals(mimeFrom) && item.targetMime.equals(mimeTo)) {
					return true;
				}
			}
			return false;
		}
	}
	
	@Override
	public boolean isWriterSupported() {
		return true;
	}

	@Override
	public boolean isOutputStreamSupported() {
		return false;
	}

	@Override
	public Writer serialize(final Writer os, final MimeType mimeFrom, final MimeType mimeTo, final NanoServiceBuilder props) throws IOException {
		if (os == null) {
			throw new NullPointerException("Output stream can't be null");
		}
		else if (mimeFrom == null) {
			throw new NullPointerException("From MIME type can't be null");
		}
		else if (mimeTo == null) {
			throw new NullPointerException("To MIME type can't be null");
		}
		else {
			for(PairDescriptor item : CONVERTORS) {
				if (item.sourceMime.equals(mimeFrom) && item.targetMime.equals(mimeTo)) {
					return item.conv.apply(os, props);
				}
			}
			throw new IllegalArgumentException("Conversion from ["+mimeFrom+"] to ["+mimeTo+"] is not supported");
		}
	}

	@Override
	public OutputStream serialize(final OutputStream os, final MimeType mimeFrom, final MimeType mimeTo, final NanoServiceBuilder props) throws IOException {
		throw new UnsupportedOperationException("Output stream serialization is not supported");
	}
	
	private static Writer creole2Html(final Writer os, final NanoServiceBuilder props) {
		try {
			return new CreoleWriter(os, MarkupOutputFormat.XML2HTML);
		} catch (IOException exc) {
			throw new EnvironmentException(exc);
		}
	}

	private static Writer creole2Plain(final Writer os, final NanoServiceBuilder props) {
		try {
			return new CreoleWriter(os, MarkupOutputFormat.XML2TEXT);
		} catch (IOException exc) {
			throw new EnvironmentException(exc);
		}
	}

	private static Writer markdown2Html(final Writer os, final NanoServiceBuilder props) {
		return new StringWriter() {
			public void close() throws IOException {
				super.close();
				final Parser 		parser = Parser.builder().build();
				final Node 			document = parser.parse(toString());
				final HtmlRenderer	renderer = HtmlRenderer.builder().build();
				
				os.write(renderer.render(document));
				os.flush();
				os.close();
			}
		};
	}

	private static Writer markdown2Plain(final Writer os, final NanoServiceBuilder props) {
		return os;
	}

	private static Writer html2Plain(final Writer os, final NanoServiceBuilder props) {
		return os;
	}
	
	private static class PairDescriptor {
		final MimeType	sourceMime;
		final MimeType	targetMime;
		final BiFunction<Writer, NanoServiceBuilder, Writer> conv;
		
		private PairDescriptor(final MimeType sourceMime, final MimeType targetMime, final BiFunction<Writer, NanoServiceBuilder, Writer> conv) {
			this.sourceMime = sourceMime;
			this.targetMime = targetMime;
			this.conv = conv;
		}
	}
}
