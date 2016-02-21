package alexclin.httplite.url.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class PoolingByteArrayInputSream extends ByteArrayInputStream {
    private ByteArrayPool pool;
    public PoolingByteArrayInputSream(byte[] buf,ByteArrayPool pool) {
        super(buf);
        this.pool = pool;
    }

    @Override
    public void close() throws IOException {
        super.close();
        pool.returnBuf(this.buf);
    }
}
