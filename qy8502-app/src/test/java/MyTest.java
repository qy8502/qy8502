import java.nio.ByteBuffer;

import org.junit.Test;

public class MyTest {

	@Test
	public void test() {
		System.out.println(toStringMongod(Long.MAX_VALUE));
		System.out.println(toStringMongod(1));
	}

	public byte[] toByteArray(long _inc) {
		byte b[] = new byte[8];
		ByteBuffer bb = ByteBuffer.wrap(b);
		// by default BB is big endian like we need
		bb.putLong(_inc);
		return b;
	}

	public String toStringMongod(long _inc) {
		byte b[] = toByteArray(_inc);

		StringBuilder buf = new StringBuilder(16);

		for (int i = 0; i < b.length; i++) {
			int x = b[i] & 0xFF;
			String s = Integer.toHexString(x);
			if (s.length() == 1)
				buf.append("0");
			buf.append(s);
		}

		return buf.toString();
	}

}
