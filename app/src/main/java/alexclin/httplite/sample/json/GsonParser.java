package alexclin.httplite.sample.json;

import com.google.gson.Gson;

import java.lang.reflect.Type;

import alexclin.httplite.StringParser;

/**
 * alexclin.httplite.sample.json
 *
 * @author alexclin
 * @date 16/1/2 16:46
 */
public class GsonParser extends StringParser {
    private Gson gson;

    public GsonParser() {
        gson = new Gson();
    }

    @Override
    public <T> T praseResponse(String content, Type type) throws Exception {
        return gson.fromJson(content,type);
    }

    @Override
    public boolean isSupported(Type type) {
        return true;
    }
}
