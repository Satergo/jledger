package com.satergo.jledger;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class EmulatorLedgerDevice implements LedgerDevice {

	private final String host;
	private final int port;

	private Socket socket;

	private final int productId;

	public EmulatorLedgerDevice(String host, int port, int productId) {
		this.host = host;
		this.port = port;
		this.productId = productId;
	}

	@Override
	public int getProductId() {
		return productId;
	}

	@Override
	public boolean open() {
		try {
			socket = new Socket(host, port);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public int write(byte[] bytes) {
		try {
			socket.getOutputStream().write(ByteBuffer.allocate(4).putInt(bytes.length).array());
			socket.getOutputStream().write(bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return bytes.length;
	}

	@Override
	public int read(byte[] bytes) {
		try {
			// The emulator proxy sends the packet length as well, but it is not needed
			socket.getInputStream().skipNBytes(4);
			return socket.getInputStream().read(bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public APDUResponse readAPDU(int dataSize) {
		return LedgerDevice.super.readAPDU(dataSize);
	}
}
