package com.satergo.jledger;

public class Expect {

	private static final int UNSIGNED_BYTE_MAX_EXC = (int) Math.pow(2, 8);
	private static final int UNSIGNED_SHORT_MAX_EXC = (int) Math.pow(2, 16);
	private static final long UNSIGNED_INT_MAX_EXC = (long) Math.pow(2L, 32L);

	public static void ubyte(int i) {
		if (i < 0 || i >= UNSIGNED_BYTE_MAX_EXC)
			throw new IllegalArgumentException("unsigned byte expected (0-" + (UNSIGNED_BYTE_MAX_EXC - 1) + " inc.)");
	}

	public static void ushort(int i) {
		if (i < 0 || i >= UNSIGNED_SHORT_MAX_EXC)
			throw new IllegalArgumentException("unsigned short expected (0-" + (UNSIGNED_SHORT_MAX_EXC - 1) + " inc.)");
	}

	public static void uint(long l) {
		if (l < 0L || l >= UNSIGNED_INT_MAX_EXC)
			throw new IllegalArgumentException("unsigned int expected (0-" + (UNSIGNED_INT_MAX_EXC - 1) + " inc.)");
	}
}
