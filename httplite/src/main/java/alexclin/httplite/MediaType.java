package alexclin.httplite;

import java.nio.charset.Charset;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 14:29
 */
public interface MediaType {
    /**
     * The "mixed" subtype of "multipart" is intended for use when the body parts are independent and
     * need to be bundled in a particular order. Any "multipart" subtypes that an implementation does
     * not recognize must be treated as being of subtype "mixed".
     */
    String MULTIPART_MIXED = "multipart/mixed";

    /**
     * The "multipart/alternative" type is syntactically identical to "multipart/mixed", but the
     * semantics are different. In particular, each of the body parts is an "alternative" version of
     * the same information.
     */
    String MULTIPART_ALTERNATIVE = "multipart/alternative";

    /**
     * This type is syntactically identical to "multipart/mixed", but the semantics are different. In
     * particular, in a digest, the default {@code Content-Type} name for a body part is changed from
     * "text/plain" to "message/rfc822".
     */
    String MULTIPART_DIGEST = "multipart/digest";

    /**
     * This type is syntactically identical to "multipart/mixed", but the semantics are different. In
     * particular, in a parallel entity, the order of body parts is not significant.
     */
    String MULTIPART_PARALLEL = "multipart/parallel";

    /**
     * The media-type multipart/form-data follows the rules of all multipart MIME data streams as
     * outlined in RFC 2046. In forms, there are a series of fields to be supplied by the user who
     * fills out the form. Each field has a name. Within a given form, the names are unique.
     */
    String MULTIPART_FORM = "multipart/form-data";

    String APPLICATION_FORM = "application/x-www-form-urlencoded";

    String APPLICATION_STREAM = "application/octet-stream";

    String TEXT_PLAIN = "text/plain";

    String APPLICATION_JSON = "application/json";
    String type();
    String subtype();
    Charset charset();
    Charset charset(Charset defaultValue);
    String toString();
}
