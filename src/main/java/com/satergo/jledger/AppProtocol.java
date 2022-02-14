package com.satergo.jledger;

import java.util.function.Function;

public abstract class AppProtocol {
	protected final LedgerDevice device;

	public AppProtocol(LedgerDevice device) {
		this.device = device;
	}


	// Validations
	protected static void requireLength(byte[] array, int length) {
		if (array.length != length) throw new IllegalArgumentException("must be of length " + length);
	}

	/**
	 * Reads an empty APDU and throws an exception if the SW is not the expected one
	 */
	protected final <EX extends RuntimeException>void voidCheckError(int expectedSW, Function<Integer, EX> function) throws EX {
		APDUResponse response = device.readAPDU(0);
		if (response.getSW() != expectedSW) {
			throw function.apply(response.getSW());
		}
	}

	/**
	 * Checks if the response has an error and throws it in that case. Otherwise, it returns the response
	 */
	protected final <EX extends RuntimeException>APDUResponse checkError(int expectedSW, APDUResponse response, Function<Integer, EX> function) throws EX {
		if (response.getSW() != expectedSW) {
			throw function.apply(response.getSW());
		}
		return response;
	}
}
