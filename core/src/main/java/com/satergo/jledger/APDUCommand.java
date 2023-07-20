package com.satergo.jledger;

import java.util.Arrays;
import java.util.Objects;

/**
 * A custom alternative to {@link javax.smartcardio.CommandAPDU}
 * <p>
 * It does not allow:
 * - The {@code Ne} parameter
 * - Data longer than 255 bytes
 * <p>
 * but it allows:
 * - Providing Nc 0 for empty data, which in the standard actually means 256 bytes but Ledger does not respect the standard
 * - Reading bytes from the data part without copying the array
 */
public final class APDUCommand {

	private final byte[] apdu;
	private final int nc;

	/** Constructs an empty APDUCommand */
	public APDUCommand(int cla, int ins, int p1, int p2) {
		this(cla, ins, p1, p2, false);
	}

	/** Constructs an empty APDUCommand */
	public APDUCommand(int cla, int ins, int p1, int p2, boolean ncZero) {
		apdu = new byte[ncZero ? 5 : 4];
		apdu[0] = (byte) cla;
		apdu[1] = (byte) ins;
		apdu[2] = (byte) p1;
		apdu[3] = (byte) p2;
		nc = 0;
		if (ncZero) apdu[4] = 0;
	}


	public APDUCommand(int cla, int ins, int p1, int p2, byte[] data) {
		if (data.length > 255)
			throw new IllegalArgumentException("The data is too long");
		apdu = new byte[5 + data.length];
		apdu[0] = (byte) cla;
		apdu[1] = (byte) ins;
		apdu[2] = (byte) p1;
		apdu[3] = (byte) p2;
		nc = data.length;
		apdu[4] = (byte) nc;
		System.arraycopy(data, 0, apdu, 5, data.length);
	}

	public int getCLA() { return apdu[0] & 0xFF; }
	public int getINS() { return apdu[1] & 0xFF; }
	public int getP1() { return apdu[2] & 0xFF; }
	public int getP2() { return apdu[3] & 0xFF; }
	public int getNc() { return nc; }

	public byte[] getBytes() {
		return apdu.clone();
	}

	public byte[] getData() {
		if (nc == 0) return new byte[0];
		return Arrays.copyOfRange(apdu, 5, apdu.length);
	}

	public byte getDataByte(int index) {
		Objects.checkIndex(index, apdu.length - 5);
		return apdu[5 + index];
	}
}
