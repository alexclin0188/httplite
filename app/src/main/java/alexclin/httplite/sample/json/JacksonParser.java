package alexclin.httplite.sample.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.lang.reflect.Type;

import alexclin.httplite.StringParser;

/**
 * JacksonParser
 *
 * @author alexclin  16/1/2 15:28
 */
public class JacksonParser extends StringParser {
    private ObjectMapper mapper;

    public JacksonParser() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,false);
    }

    @Override
    public <T> T parseResponse(String content, Type type) throws Exception{
        return mapper.readValue(content, TypeFactory.defaultInstance().constructType(type));
    }

    @Override
    public boolean isSupported(Type type) {
        return true;
    }
}
