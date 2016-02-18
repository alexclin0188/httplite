package alexclin.httplite.url;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * alexclin.httplite.urlconnection
 *
 * shamelessly stolen from okhttp
 * @author alexclin
 * @date 16/1/2 19:32
 */
public class URLMediaType implements alexclin.httplite.MediaType {

    private static final String TOKEN = "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)";
    private static final String QUOTED = "\"([^\"]*)\"";
    private static final Pattern TYPE_SUBTYPE = Pattern.compile(TOKEN + "/" + TOKEN);
    private static final Pattern PARAMETER = Pattern.compile(
            ";\\s*(?:" + TOKEN + "=(?:" + TOKEN + "|" + QUOTED + "))?");

    private final String mediaType;
    private final String type;
    private final String subtype;
    private final String charset;

    private URLMediaType(String mediaType, String type, String subtype, String charset) {
        this.mediaType = mediaType;
        this.type = type;
        this.subtype = subtype;
        this.charset = charset;
    }

    /**
     * Returns a media type for {@code string}, or null if {@code string} is not a well-formed media
     * type.
     */
    public static URLMediaType parse(String string) {
        Matcher typeSubtype = TYPE_SUBTYPE.matcher(string);
        if (!typeSubtype.lookingAt()) return null;
        String type = typeSubtype.group(1).toLowerCase(Locale.US);
        String subtype = typeSubtype.group(2).toLowerCase(Locale.US);

        String charset = null;
        Matcher parameter = PARAMETER.matcher(string);
        for (int s = typeSubtype.end(); s < string.length(); s = parameter.end()) {
            parameter.region(s, string.length());
            if (!parameter.lookingAt()) return null; // This is not a well-formed media type.

            String name = parameter.group(1);
            if (name == null || !name.equalsIgnoreCase("charset")) continue;
            String charsetParameter = parameter.group(2) != null
                    ? parameter.group(2)  // Value is a token.
                    : parameter.group(3); // Value is a quoted string.
            if (charset != null && !charsetParameter.equalsIgnoreCase(charset)) {
                throw new IllegalArgumentException("Multiple different charsets: " + string);
            }
            charset = charsetParameter;
        }

        return new URLMediaType(string, type, subtype, charset);
    }

    /**
     * Returns the high-level media type, such as "text", "image", "audio", "video", or
     * "application".
     */
    public String type() {
        return type;
    }

    /**
     * Returns a specific media subtype, such as "plain" or "png", "mpeg", "mp4" or "xml".
     */
    public String subtype() {
        return subtype;
    }

    /**
     * Returns the charset of this media type, or null if this media type doesn't specify a charset.
     */
    public Charset charset() {
        return charset != null ? Charset.forName(charset) : null;
    }

    /**
     * Returns the charset of this media type, or {@code defaultValue} if this media type doesn't
     * specify a charset.
     */
    public Charset charset(Charset defaultValue) {
        return charset != null ? Charset.forName(charset) : defaultValue;
    }

    /**
     * Returns the encoded media type, like "text/plain; charset=utf-8", appropriate for use in a
     * Content-Type header.
     */
    @Override public String toString() {
        return mediaType;
    }

    @Override public boolean equals(Object o) {
        return o instanceof URLMediaType && ((URLMediaType) o).mediaType.equals(mediaType);
    }

    @Override public int hashCode() {
        return mediaType.hashCode();
    }
}
