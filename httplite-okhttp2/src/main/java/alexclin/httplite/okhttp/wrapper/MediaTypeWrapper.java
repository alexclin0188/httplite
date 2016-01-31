package alexclin.httplite.okhttp.wrapper;

import com.squareup.okhttp.MediaType;

import java.nio.charset.Charset;

/**
 * alexclin.httplite.okhttp.wrapper
 *
 * @author alexclin
 * @date 16/1/1 14:39
 */
public class MediaTypeWrapper implements alexclin.httplite.MediaType {
    private MediaType mediaType;

    public MediaTypeWrapper(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public String type() {
        return mediaType.type();
    }

    @Override
    public String subtype() {
        return mediaType.subtype();
    }

    @Override
    public Charset charset() {
        return mediaType.charset();
    }

    @Override
    public Charset charset(Charset defaultValue) {
        return mediaType.charset();
    }

    public MediaType raw() {
        return mediaType;
    }

    public static MediaType wrapperLite(alexclin.httplite.MediaType type){
        if(type == null) return null;
        if(type instanceof MediaTypeWrapper){
           return ((MediaTypeWrapper)type).raw();
        }else
            return MediaType.parse(type.toString());
    }
}
