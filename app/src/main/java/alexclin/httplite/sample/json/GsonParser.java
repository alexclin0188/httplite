package alexclin.httplite.sample.json;

import com.google.gson.Gson;

import java.lang.reflect.Type;

import alexclin.httplite.impl.StringParser;

/**
 * GsonParser
 *
 * @author alexclin 16/1/2 16:46
 */
public class GsonParser extends StringParser {
    private Gson gson;

    public GsonParser() {
        gson = new Gson();
    }

    @Override
    public <T> T parseResponse(String content, Type type) throws Exception {
        return gson.fromJson(content,type);
    }

    @Override
    public boolean isSupported(Type type) {
        return true;
    }
}
