package com.satergo.jledger.protocol.ergo;

import com.satergo.jledger.APDUResponse;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Optional;

class Utils {
	private Utils() {}

	static int bip44PathLength(int[] bip44Path) {
		return 1 + bip44Path.length * 4;
	}

	static void putBip44Path(ByteBuffer buffer, int[] path) {
		buffer.put((byte) path.length);
		for (int derivationIndex : path) {
			buffer.putInt(derivationIndex);
		}
	}

	static int optTokenLength(@Nullable Integer optionalAuthToken) {
		return optionalAuthToken != null ? 4 : 0;
	}

	static void putOptToken(ByteBuffer buffer, @Nullable Integer optionalAuthToken) {
		if (optionalAuthToken != null)
			buffer.putInt(optionalAuthToken);
	}

	static APDUResponse checkError(APDUResponse response) throws ErgoLedgerException {
		if (response.getSW() != ErgoProtocol.RESULT_OK) {
			throw new ErgoLedgerException(response.getSW());
		}
		return response;
	}

	static Optional<Integer> emptyOrOneUnsignedByte(APDUResponse apduResponse) throws ErgoLedgerException {
		APDUResponse response = checkError(apduResponse);
		return switch (response.getNr()) {
			case 0 -> Optional.empty();
			case 1 -> Optional.of(response.getDataByte(0) & 0xFF);
			default -> throw new IllegalStateException("Received " + response.getNr() + " bytes (0 or 1 expected)");
		};
	}
}
