package alexclin.httplite.sample.json;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Type;

import alexclin.httplite.impl.StringParser;

/**
 * FastJsonParser
 *
 * @author alexclin  16/1/2 16:37
 */
public class FastJsonParser extends StringParser {
    @Override
    public <T> T parseResponse(String content, Type type) throws Exception {
        return JSON.parseObject(content,type);
    }

    @Override
    public boolean isSupported(Type type) {
        return true;
    }
}
