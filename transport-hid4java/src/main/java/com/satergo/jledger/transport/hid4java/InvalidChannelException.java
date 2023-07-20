package com.satergo.jledger.transport.hid4java;

/**
 * Thrown when an incorrect channel is received from the Ledger device,
 * it is known to be 0 when the device is locked.
 */
public class InvalidChannelException extends IllegalStateException {
	public final int received;

	public InvalidChannelException(String s, int received) {
		super(s);
		this.received = received;
	}
}
