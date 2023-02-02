package com.satergo.jledger;

import java.util.Arrays;

public interface LedgerDevice {

	int VENDOR_ID = 0x2c97;

	int BLUE_PRODUCT_ID = 0x0011;
	int NANO_S_PRODUCT_ID = 0x1011;
	int NANO_X_PRODUCT_ID = 0x4011;
	int NANO_S_PLUS_PRODUCT_ID = 0x5011;

	static boolean isLedgerDevice(int vendorId, int productId) {
		return vendorId == VENDOR_ID && (productId == BLUE_PRODUCT_ID || productId == NANO_S_PRODUCT_ID || productId == NANO_X_PRODUCT_ID || productId == NANO_S_PLUS_PRODUCT_ID);
	}

	int getProductId();

	boolean open();

	int write(byte[] bytes);

	/**
	 * @return number of bytes that were read. after (returnValue-1), all bytes are left untouched (0 if the array provided was empty)
	 */
	int read(byte[] bytes);

	default void writeAPDU(APDUCommand apdu) {
		write(apdu.getBytes());
	}

	default APDUResponse readAPDU(int dataSize) {
		byte[] result = new byte[dataSize + 2];
		int read = read(result);
		if (read < result.length) {
			result = Arrays.copyOf(result, read);
		}
		return new APDUResponse(result);
	}
}
