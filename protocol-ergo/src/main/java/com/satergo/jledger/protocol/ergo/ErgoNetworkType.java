package com.satergo.jledger.protocol.ergo;

public enum ErgoNetworkType {

	MAINNET((byte) 0x00), TESTNET((byte) 0x10);

	final byte id;

	ErgoNetworkType(byte id) {
		this.id = id;
	}
}
