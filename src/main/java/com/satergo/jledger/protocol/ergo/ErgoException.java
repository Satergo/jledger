package com.satergo.jledger.protocol.ergo;

import java.util.Locale;

/**
 * <a href="https://github.com/tesseract-one/ledger-app-ergo/blob/main/src/sw.h">List of error codes</a>
 *
 * TODO decide if this should be unchecked or checked
 */
public class ErgoException extends RuntimeException {

	private final int id;

	public ErgoException(int id) {
		this.id = id;
	}

	@Override
	public String getMessage() {
		String hex = Integer.toHexString(id).toLowerCase(Locale.ROOT);
		return "0x" + "0".repeat(4 - hex.length()) + hex;
	}

	public int getId() {
		return id;
	}
}
