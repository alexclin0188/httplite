package alexclin.httplite;

/**
 * Method http method
 *
 * @author alexclin
 * @date 16/2/1 19:53
 */
public enum Method{
    GET,POST,PUT,DELETE,HEAD,PATCH
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