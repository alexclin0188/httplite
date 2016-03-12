package alexclin.httplite.okhttp3;

import java.nio.charset.Charset;

import okhttp3.MediaType;

/**
 * OkMediaType
 *
 * @author alexclin 16/1/1 14:39
 */
public class OkMediaType implements alexclin.httplite.MediaType {
    private MediaType mediaType;

    public OkMediaType(MediaType mediaType) {
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
        if(type instanceof OkMediaType){
           return ((OkMediaType)type).raw();
        }else
            return MediaType.parse(type.toString());
    }
}
