package alexclin.httplite.retrofit;

import android.util.Pair;

import java.lang.annotation.Annotation;
import java.util.List;

import alexclin.httplite.Request;

/**
 * ParamMiscProcessor
 *
 * @author alexclin
 * @date 16/1/31 13:38
 */
public interface ParamMiscProcessor extends AbsParamProcessor{
    void process(Request request,Annotation[][] annotations,List<Pair<Integer,Integer>> list,Object... args);
}
