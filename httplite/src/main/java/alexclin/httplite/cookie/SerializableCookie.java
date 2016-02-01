package alexclin.httplite.cookie;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpCookie;

public class SerializableCookie implements Serializable {
    private static final long serialVersionUID = 6374381828722046732L;

    private transient final HttpCookie cookie;
    private transient HttpCookie clientCookie;

    public SerializableCookie(HttpCookie cookie) {
        this.cookie = cookie;
    }

    public HttpCookie getCookie() {
        HttpCookie bestCookie = cookie;
        if (clientCookie != null) {
            bestCookie = clientCookie;
        }
        return bestCookie;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(cookie.getName());
        out.writeObject(cookie.getValue());

        out.writeObject(cookie.getComment());
        out.writeObject(cookie.getCommentURL());
        out.writeObject(cookie.getDomain());
        out.writeObject(cookie.getPath());

        out.writeLong(cookie.getMaxAge());
        out.writeInt(cookie.getVersion());

        out.writeBoolean(cookie.getSecure());
        out.writeBoolean(cookie.getDiscard());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String name = (String) in.readObject();
        String value = (String) in.readObject();
        clientCookie = new HttpCookie(name, value);

        clientCookie.setComment((String) in.readObject());
        clientCookie.setCommentURL((String) in.readObject());
        clientCookie.setDomain((String) in.readObject());
        clientCookie.setPath((String) in.readObject());

        clientCookie.setMaxAge(in.readLong());
        clientCookie.setVersion(in.readInt());

        clientCookie.setSecure(in.readBoolean());
        clientCookie.setDiscard(in.readBoolean());
    }
}
