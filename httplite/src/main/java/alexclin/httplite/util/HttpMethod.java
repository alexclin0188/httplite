package alexclin.httplite.util;

/**
 * Method http method
 *
 * @author alexclin at 16/2/1 19:53
 */
public enum HttpMethod {
    GET(false,false),POST(true,true),PUT(true,true),DELETE(true,false),HEAD(false,false),PATCH(true,true);

    public final boolean permitsRequestBody;
    public final boolean requiresRequestBody;

    HttpMethod(boolean permitsRequestBody, boolean requiresRequestBody) {
        this.permitsRequestBody = permitsRequestBody;
        this.requiresRequestBody = requiresRequestBody;
    }

    public static boolean permitsRequestBody(HttpMethod method) {
        return requiresRequestBody(method)
                || method.name().equals("OPTIONS")
                || method.name().equals("DELETE")    // Permitted as spec is ambiguous.
                || method.name().equals("PROPFIND")  // (WebDAV) without body: call <allprop/>
                || method.name().equals("MKCOL")     // (WebDAV) may contain a body, but behaviour is unspecified
                || method.name().equals("LOCK");     // (WebDAV) body: create lock, without body: refresh lock
    }

    public static boolean requiresRequestBody(HttpMethod method) {
        return method.name().equals("POST")
                || method.name().equals("PUT")
                || method.name().equals("PATCH")
                || method.name().equals("PROPPATCH") // WebDAV
                || method.name().equals("REPORT");   // CalDAV/CardDAV (defined in WebDAV Versioning)
    }
}

//    /**
//     *
//     */
//    public enum Protocol {
//        HTTP_1_0("http/1.0"),
//        HTTP_1_1("http/1.1"),
//        SPDY_3("spdy/3.1"),
//        HTTP_2("h2");
//
//        private final String protocol;
//
//        Protocol(String protocol) {
//            this.protocol = protocol;
//        }
//
//        @Override public String toString() {
//            return protocol;
//        }
//
//        /**
//         * Returns the protocol identified by {@code protocol}.
//         *
//         * @throws IOException if {@code protocol} is unknown.
//         */
//        public static Protocol get(String protocol) throws IOException {
//            // Unroll the loop over values() to save an allocation.
//            if (protocol.equals(HTTP_1_0.protocol)) return HTTP_1_0;
//            if (protocol.equals(HTTP_1_1.protocol)) return HTTP_1_1;
//            if (protocol.equals(HTTP_2.protocol)) return HTTP_2;
//            if (protocol.equals(SPDY_3.protocol)) return SPDY_3;
//            throw new IOException("Unexpected protocol: " + protocol);
//        }
//    }