package be.nabu.libs.smtp.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.authentication.impl.BasicPrincipalImpl;
import be.nabu.libs.nio.api.MessageParser;
import be.nabu.libs.nio.api.MessagePipeline;
import be.nabu.libs.nio.impl.MessagePipelineImpl;
import be.nabu.libs.resources.memory.MemoryItem;
import be.nabu.utils.codec.TranscoderUtils;
import be.nabu.utils.codec.impl.Base64Decoder;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.CharBuffer;
import be.nabu.utils.io.api.DelimitedCharContainer;
import be.nabu.utils.io.api.PushbackContainer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;
import be.nabu.utils.io.buffers.bytes.ByteBufferFactory;
import be.nabu.utils.io.buffers.bytes.DynamicByteBuffer;
import be.nabu.utils.io.containers.chars.ReadableStraightByteToCharContainer;
import be.nabu.utils.mime.api.Part;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeParser;
import be.nabu.utils.mime.impl.ParsedMimePart;

public class SMTPMessageParser implements MessageParser<Part> {

	private DynamicByteBuffer initialBuffer;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private MessagePipeline<Part, String> pipeline;
	private String remoteName;
	private String from;
	private List<String> to = new ArrayList<String>();
	private Part message;
	
	private boolean isClosed, done, streaming;
	private boolean identified = true;

	private SMTPMessageParserFactory factory;
	
	private boolean is8Bit;
	private Long actualSize;

	private Token token;

	private String authorizationId;
	
	public SMTPMessageParser(SMTPMessageParserFactory factory, MessagePipeline<Part, String> pipeline) {
		this.factory = factory;
		this.pipeline = pipeline;
		initialBuffer = new DynamicByteBuffer();
		initialBuffer.mark();
	}
	
	@Override
	public void close() throws IOException {
		pipeline.close();
	}

	@Override
	public void push(PushbackContainer<ByteBuffer> content) throws ParseException, IOException {
		if (!streaming) {
			initialBuffer.reset();
			ByteBuffer limitedBuffer = ByteBufferFactory.getInstance().limit(initialBuffer, null, factory.getValidator().getMaxLineSize() - initialBuffer.remainingData());
			long read = content.read(limitedBuffer);
			isClosed |= read == -1;
			ReadableContainer<CharBuffer> data = new ReadableStraightByteToCharContainer(initialBuffer);
			DelimitedCharContainer delimit = IOUtils.delimit(data, "\n");
			String request = IOUtils.toString(delimit);
			logger.debug(request);
			if (!delimit.isDelimiterFound()) {
				// if we have reached the maximum size for the request and not found one, throw an exception
				if (request.length() >= factory.getValidator().getMaxLineSize()) {
					throw new SMTPException(523);
				}
			}
			else {
				initialBuffer.remark();
				if (request.endsWith("\r")) {
					request = request.substring(0, request.length() - 1);
				}
				if (request.startsWith("HELO ")) {
					// if we already have a name, use that
					if (remoteName != null) {
						throw new SMTPException(500);
					}
					else {
						String remoteName = request.substring(5);
						if (!factory.getValidator().acceptConnection(factory.getServerName(), factory.getRealm(), remoteName, pipeline.getSourceContext().getSocketAddress())) {
							throw new SMTPException(500, "Connection from '" + remoteName + "' not accepted");
						}
						this.remoteName = remoteName;
						factory.setRemoteName(remoteName);
						pipeline.getResponseQueue().offer("250 Join us " + remoteName + ", resistance is futile");
					}
				}
				else if (request.startsWith("MAIL FROM:")) {
					// the from could be this:
					// MAIL FROM:<alex@nabu.be> BODY=8BITMIME SIZE=365
					from = request.substring("MAIL FROM:".length()).trim();
					if (from.contains("BODY=8BITMIME")) {
						is8Bit = true;
						from = from.replace("BODY=8BITMIME", "").trim();
					}
					if (from.contains("SIZE=")) {
						actualSize = Long.parseLong(from.replaceAll(".*SIZE=([0-9]+).*", "$1"));
						from = from.replaceAll("SIZE=[0-9]+", "").trim();
					}
					if (from.contains("<")) {
						from = extractEmail(from);
					}
					pipeline.getResponseQueue().offer("250 Ok");
				}
				else if (request.startsWith("RCPT TO:")) {
					to.add(extractEmail(request.substring("RCPT TO:".length()).trim()));
					pipeline.getResponseQueue().offer("250 Ok");
				}
				else if (request.startsWith("AUTH ")) {
					if (factory.getAuthenticator() == null) {
						throw new SMTPException(500);
					}
					request = request.substring(5);
					// currently only plain
					if (!request.startsWith("PLAIN ")) {
						throw new SMTPException(500);
					}
					request = request.substring(6);
					ReadableContainer<ByteBuffer> transcodeBytes = TranscoderUtils.transcodeBytes(IOUtils.wrap(request.getBytes(), true), new Base64Decoder());
					byte[] decoded = IOUtils.toBytes(transcodeBytes);
					List<Integer> offsets = findOffsets(decoded, '\0');
					authorizationId = null;
					// the username
					String authenticationId = null;
					// the password
					String password = null;
					if (offsets.size() == 2) {
						authorizationId = new String(Arrays.copyOfRange(decoded, 0, offsets.get(0)), Charset.forName("UTF-8"));
						authenticationId = new String(Arrays.copyOfRange(decoded, offsets.get(0) + 1, offsets.get(1)), Charset.forName("UTF-8"));
						password = new String(Arrays.copyOfRange(decoded, offsets.get(1) + 1, decoded.length), Charset.forName("UTF-8"));
					}
					else if (offsets.size() == 1) {
						authenticationId = new String(Arrays.copyOfRange(decoded, 0, offsets.get(0)), Charset.forName("UTF-8"));
						password = new String(Arrays.copyOfRange(decoded, offsets.get(0) + 1, decoded.length), Charset.forName("UTF-8"));
					}
					else {
						throw new SMTPException(500);
					}
					token = factory.getAuthenticator().authenticate(factory.getRealm(), new BasicPrincipalImpl(authenticationId, password));
					if (token == null) {
						pipeline.getResponseQueue().offer("535 Wrong user/password");
					}
					else {
						pipeline.getResponseQueue().offer("235 2.7.0 Authentication successful");
					}
				}
				else if (request.equals("DATA")) {
					if (!factory.getValidator().acceptMail(factory.getServerName(), factory.getRealm(), remoteName, from, to, token)) {
						throw new SMTPException(500, "Data not accepted");
					}
					streaming = true;
					pipeline.getResponseQueue().offer("354 End data with <CR><LF>.<CR><LF>");
				}
				else if (request.equals("QUIT")) {
					pipeline.getResponseQueue().offer("221 Bye");
					isClosed = true;
				}
				else if (request.equals("STARTTLS")) {
					if (factory.getContext() == null) {
						pipeline.getResponseQueue().offer("500 STARTTLS is not supported");
					}
					else {
						pipeline.getResponseQueue().offer("220 Ready to start TLS");
						((MessagePipelineImpl<Part, String>) pipeline).startTls(factory.getContext(), factory.getSslServerMode());
					}
				}
				else if (request.equals("RSET")) {
					from = null;
					to.clear();
				}
				else if (request.equals("VRFY")) {
					// this command is not supported for security reasons
					pipeline.getResponseQueue().offer("250 I'm sorry, Dave. I'm afraid I can't do that");
				}
				else if (request.equals("NOOP")) {
					pipeline.getResponseQueue().offer("250 I see dead connections!");
				}
				else if (request.startsWith("EHLO ")) {
					// if we already have a name, use that
					if (remoteName != null) {
						throw new SMTPException(500);
					}
					else {
						String remoteName = request.substring(5);
						if (!factory.getValidator().acceptConnection(factory.getServerName(), factory.getRealm(), remoteName, pipeline.getSourceContext().getSocketAddress())) {
							throw new SMTPException(500, "Connection from '" + remoteName + "' not accepted");
						}
						this.remoteName = remoteName;
						factory.setRemoteName(remoteName);
						pipeline.getResponseQueue().offer("250-" + factory.getServerName() + " Join us " + remoteName + ", resistance is futile");
						pipeline.getResponseQueue().offer("250-8BITMIME");
						if (factory.isEnablePipelining()) {
							pipeline.getResponseQueue().offer("250-PIPELINING");
						}
						if (factory.getAuthenticator() != null) {
							pipeline.getResponseQueue().offer("250-AUTH PLAIN");
						}
						if (factory.getContext() != null) {
							pipeline.getResponseQueue().offer("250-STARTTLS");
						}
						pipeline.getResponseQueue().offer("250 SIZE " + factory.getValidator().getMaxDataSize());
					}
				}
				else {
					throw new SMTPException(500, request);
				}
			}
		}
		else {
			// reset so we see all the data
			initialBuffer.reset();
			ByteBuffer limitedBuffer = ByteBufferFactory.getInstance().limit(initialBuffer, null, factory.getValidator().getMaxDataSize() - initialBuffer.remainingData());
			long read = content.read(limitedBuffer);
			isClosed |= read == -1;
			ReadableContainer<CharBuffer> data = new ReadableStraightByteToCharContainer(initialBuffer);
			DelimitedCharContainer delimit = IOUtils.delimit(data, "\r\n.\r\n");
			String request = IOUtils.toString(delimit);
			if (!delimit.isDelimiterFound()) {
				// if we have reached the maximum size for the request and not found one, throw an exception
				if (request.length() >= factory.getValidator().getMaxDataSize()) {
					throw new SMTPException(523);
				}
			}
			else {
				initialBuffer.remark();
				MimeParser parser = new MimeParser();
				MemoryItem resource = new MemoryItem("mail");
				WritableContainer<ByteBuffer> writable = resource.getWritable();
				writable.write(IOUtils.wrap(request.getBytes(Charset.forName("ASCII")), true));
				writable.close();
				ParsedMimePart parse = parser.parse(resource);
				parse.setHeader(new MimeHeader("X-Original-From", from));
				for (String to : this.to) {
					parse.setHeader(new MimeHeader("X-Original-To", to));
				}
				String identifier = UUID.randomUUID().toString().replace("-",  "");
				parse.setHeader(new MimeHeader("X-Generated-Id", identifier));
				if (actualSize != null) {
					parse.setHeader(new MimeHeader("X-Reported-Size", actualSize.toString()));
				}
				if (token != null) {
					parse.setHeader(new SimpleAuthenticationHeader(token));
				}
				if (authorizationId != null && !authorizationId.trim().isEmpty()) {
					parse.setHeader(new MimeHeader("X-Requested-Authorization-Id", authorizationId));
				}
				parse.setHeader(new MimeHeader("Received", "from " + remoteName + " by " + factory.getServerName(), getDateFormatter().format(new Date())));
				message = parse;
				done = true;
				pipeline.getResponseQueue().offer("250 Ok, generated id: " + identifier);
			}
		}
		if (isDone() && initialBuffer.remainingData() > 0) {
			content.pushback(initialBuffer);
		}
	}
	
	private static ThreadLocal<SimpleDateFormat> dateParser = new ThreadLocal<SimpleDateFormat>();
	
	private static SimpleDateFormat getDateFormatter() {
		if (dateParser.get() == null) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateParser.set(simpleDateFormat);
		}
		return dateParser.get();
	}

	private List<Integer> findOffsets(byte[] decoded, char c) {
		List<Integer> offsets = new ArrayList<Integer>();
		for (int i = 0; i < decoded.length; i++) {
			if ((char) decoded[i] == c) {
				offsets.add(i);
			}
		}
		return offsets;
	}

	private String extractEmail(String email) {
		return email.replaceAll(".*<([^>]+)>.*", "$1").trim();
	}

	@Override
	public boolean isIdentified() {
		return identified;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public Part getMessage() {
		return message;
	}

	public boolean isMimeBit() {
		return is8Bit;
	}
}
