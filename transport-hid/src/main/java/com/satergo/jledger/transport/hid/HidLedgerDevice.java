package com.satergo.jledger.transport.hid;

import com.satergo.jledger.LedgerDevice;
import org.hid4java.HidDevice;

public class HidLedgerDevice implements LedgerDevice {

	private final HidDevice hidDevice;

	public HidLedgerDevice(HidDevice hidDevice) {
		this.hidDevice = hidDevice;
	}

	@Override
	public int getProductId() {
		return hidDevice.getProductId();
	}

	@Override
	public boolean open() {
		return hidDevice.open();
	}

	@Override
	public void close() {
		hidDevice.close();
	}

	@Override
	public int write(byte[] bytes) {
		return hidDevice.write(bytes, bytes.length, (byte) 0);
	}

	@Override
	public int read(byte[] bytes) {
		return hidDevice.read(bytes);
	}
}
