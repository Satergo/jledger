package com.satergo.jledger.transport.speculos;

import com.satergo.jledger.APDUCommand;
import com.satergo.jledger.APDUResponse;
import com.satergo.jledger.LedgerDevice;
import org.jspecify.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.Socket;
import java.nio.ByteOrder;
import java.util.concurrent.locks.ReentrantLock;

public class SpeculosLedgerDevice implements LedgerDevice {

	private final String host;
	private final int port;

	private @Nullable Socket socket;

	private final int productId;

	public SpeculosLedgerDevice(String host, int port, int productId) {
		this.host = host;
		this.port = port;
		this.productId = productId;
	}

	@Override
	public int getProductId() {
		return productId;
	}

	@Override
	public void open() {
		try {
			socket = new Socket(host, port);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		if (socket == null)
			throw new IllegalStateException("Not open");
		try {
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);

	@Override
	public void writeAPDU(APDUCommand apdu) {
		if (socket == null)
			throw new IllegalStateException("Not open");
		try {
			byte[] bytes = apdu.getBytes();
			byte[] length = new byte[4];
			INT.set(length, 0, bytes.length);
			socket.getOutputStream().write(length);
			socket.getOutputStream().write(bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public APDUResponse readAPDU() {
		if (socket == null)
			throw new IllegalStateException("Not open");
		try {
			byte[] length = new byte[4];
			if (socket.getInputStream().read(length) < 4)
				throw new EOFException();
			int dataLength = (int) INT.get(length, 0);
			byte[] data = new byte[dataLength + 2]; // size does not include the status code, so + 2
			if (socket.getInputStream().read(data) < dataLength)
				throw new EOFException();
			return new APDUResponse(data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private final ReentrantLock lock = new ReentrantLock();

	@Override
	public APDUResponse exchange(APDUCommand apdu) {
		lock.lock();
		try {
			writeAPDU(apdu);
			return readAPDU();
		} finally {
			lock.unlock();
		}
	}
}
