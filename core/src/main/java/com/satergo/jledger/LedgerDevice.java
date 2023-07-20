package com.satergo.jledger;

import java.util.Map;

public interface LedgerDevice {

	int VENDOR_ID = 0x2c97;

	Map<Integer, String> PRODUCT_IDS = Map.of(
			0x0011, "Ledger Blue",
			0x1011, "Ledger Nano S",
			0x4011, "Ledger Nano X",
			0x5011, "Ledger Nano S Plus",
			0x6011, "Ledger Stax",
			0x7011, "Ledger Flex"
	);

	static boolean isLedgerDevice(int vendorId, int productId) {
		return vendorId == VENDOR_ID && PRODUCT_IDS.containsKey(productId);
	}

	int getProductId();

	void open();

	void close();

	void writeAPDU(APDUCommand apdu);

	APDUResponse readAPDU();

	/**
	 * Sends a command to the device and reads a response from it.
	 * @implSpec The method must not exchange any other commands while the response for a previous one has not been received
	 */
	APDUResponse exchange(APDUCommand apdu);
}
