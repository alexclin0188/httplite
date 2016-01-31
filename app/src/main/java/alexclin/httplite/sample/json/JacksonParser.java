package alexclin.httplite.sample.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.lang.reflect.Type;

import alexclin.httplite.StringParser;

/**
 * alexclin.httplite.sample
 *
 * @author alexclin
 * @date 16/1/2 15:28
 */
public class JacksonParser extends StringParser {
    private ObjectMapper mapper;

    public JacksonParser() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,false);
    }

    @Override
    public <T> T praseResponse(String content, Type type) throws Exception{
//        Class clazz = Class.forName(getTypeName(type));
//        return (T)mapper.readValue(content,clazz);
        return mapper.readValue(content, TypeFactory.defaultInstance().constructType(type));
    }

    @Override
    public boolean isSupported(Type type) {
        return true;
    }

    private String getTypeName(Type type){
        String name = type.toString();
        int index = name.indexOf(" ");
        if(index>=0){
            name = name.substring(index+1);
        }
        return name;
    }
}
