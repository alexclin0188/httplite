/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alexclin.httplite.url.cache;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/** Junk drawer of utility methods. */
final class IOUtil {
	static final Charset US_ASCII = Charset.forName("US-ASCII");
	static final Charset UTF_8 = Charset.forName("UTF-8");

	private IOUtil() {
	}

	static String readFully(Reader reader) throws IOException {
		try {
			StringWriter writer = new StringWriter();
			char[] buffer = new char[1024];
			int count;
			while ((count = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, count);
			}
			return writer.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * Deletes the contents of {@code dir}. Throws an IOException if any file
	 * could not be deleted, or if {@code dir} is not a readable directory.
	 */
	static void deleteContents(File dir) throws IOException {
		File[] files = dir.listFiles();
		if (files == null) {
			throw new IOException("not a readable directory: " + dir);
		}
		for (File file : files) {
			if (file.isDirectory()) {
				deleteContents(file);
			}
			if (!file.delete()) {
				throw new IOException("failed to delete file: " + file);
			}
		}
	}

	static void closeQuietly(/*Auto*/Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (RuntimeException rethrown) {
				throw rethrown;
			} catch (Exception ignored) {
			}
		}
	}

	public static int read(InputStream is) throws IOException {
		int b = is.read();
		if (b == -1) {
			throw new EOFException();
		}
		return b;
	}

	public static void writeInt(OutputStream os, int n) throws IOException {
		os.write( n & 0xff);
		os.write((n >> 8) & 0xff);
		os.write((n >> 16) & 0xff);
		os.write((n >> 24) & 0xff);
	}

	public static int readInt(InputStream is) throws IOException {
		int n = 0;
		n |= (read(is));
		n |= (read(is) << 8);
		n |= (read(is) << 16);
		n |= (read(is) << 24);
		return n;
	}

	public static void writeLong(OutputStream os, long n) throws IOException {
		os.write((byte) n);
		os.write((byte)(n >>> 8));
		os.write((byte)(n >>> 16));
		os.write((byte)(n >>> 24));
		os.write((byte)(n >>> 32));
		os.write((byte)(n >>> 40));
		os.write((byte)(n >>> 48));
		os.write((byte)(n >>> 56));
	}

	static long readLong(InputStream is) throws IOException {
		long n = 0;
		n |= ((read(is) & 0xFFL));
		n |= ((read(is) & 0xFFL) << 8);
		n |= ((read(is) & 0xFFL) << 16);
		n |= ((read(is) & 0xFFL) << 24);
		n |= ((read(is) & 0xFFL) << 32);
		n |= ((read(is) & 0xFFL) << 40);
		n |= ((read(is) & 0xFFL) << 48);
		n |= ((read(is) & 0xFFL) << 56);
		return n;
	}

	public static void writeString(OutputStream os, String s) throws IOException {
		byte[] b = s.getBytes("UTF-8");
		writeLong(os, b.length);
		os.write(b, 0, b.length);
	}

	public static String readString(InputStream is) throws IOException {
		int n = (int) readLong(is);
		byte[] b = streamToBytes(is, n,null);
		return new String(b, "UTF-8");
	}

	/**
	 * Reads the contents of an InputStream into a byte[].
	 * */
	public static byte[] streamToBytes(InputStream in, int length,ByteArrayPool pool) throws IOException {
		byte[] bytes = null;
		if(pool==null)
			bytes = new byte[length];
		else
			bytes = pool.getBuf(length);
		int count;
		int pos = 0;
		while (pos < length && ((count = in.read(bytes, pos, length - pos)) != -1)) {
			pos += count;
		}
		if (pos != length) {
			throw new IOException("Expected " + length + " bytes, read " + pos + " bytes");
		}
		return bytes;
	}

	public static void writeHeaders(OutputStream out,Map<String,List<String>> headers){
		//TODO
	}

	public static Map<String,List<String>> readHeaders(InputStream in){
		//TODO
		return null;
	}
}