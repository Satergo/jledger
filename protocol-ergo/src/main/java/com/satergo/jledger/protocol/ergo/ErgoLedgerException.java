package com.satergo.jledger.protocol.ergo;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * TODO decide if this should be unchecked or checked
 */
public class ErgoLedgerException extends RuntimeException {

	private final int sw;

	public ErgoLedgerException(int sw) {
		this.sw = sw;
	}

	@Override
	public String getMessage() {
		String hex = Integer.toHexString(sw).toLowerCase(Locale.ROOT);
		return "0x" + "0".repeat(4 - hex.length()) + hex + " - " + getName(sw);
	}

	public int getSW() {
		return sw;
	}

	public static final int SW_OK = 0x9000;
	/** Status word for denied by user. */
	public static final int SW_DENY = 0x6985;
	/** Status word for incorrect P1 or P2. */
	public static final int SW_WRONG_P1P2 = 0x6A86;
	/** Status word for either wrong Lc or length of APDU command less than 5. */
	public static final int SW_WRONG_APDU_DATA_LENGTH = 0x6A87;
	/** Status word for unknown command with this INS. */
	public static final int SW_INS_NOT_SUPPORTED = 0x6D00;
	/** Status word for instruction class is different from CLA.*/
	public static final int SW_CLA_NOT_SUPPORTED = 0x6E00;
	/** Status word for busy state. */
	public static final int SW_BUSY = 0xB000;
	/** Status word for wrong response length (buffer too small or too big). */
	public static final int SW_WRONG_RESPONSE_LENGTH = 0xB001;

	public static final int SW_BAD_SESSION_ID = 0xB002;

	public static final int SW_WRONG_SUBCOMMAND = 0xB003;

	public static final int SW_SCREENS_BUFFER_OVERFLOW = 0xB004;

	public static final int SW_BAD_STATE = 0xB0FF;

	public static final int SW_BAD_TOKEN_ID = 0xE001;
	public static final int SW_BAD_TOKEN_VALUE = 0xE002;
	public static final int SW_BAD_CONTEXT_EXTENSION_SIZE = 0xE003;
	public static final int SW_BAD_DATA_INPUT = 0xE004;
	public static final int SW_BAD_BOX_ID = 0xE005;
	public static final int SW_BAD_TOKEN_INDEX = 0xE006;
	public static final int SW_BAD_FRAME_INDEX = 0xE007;
	public static final int SW_BAD_INPUT_COUNT = 0xE008;
	public static final int SW_BAD_OUTPUT_COUNT = 0xE009;
	public static final int SW_TOO_MANY_TOKENS = 0xE00A;
	public static final int SW_TOO_MANY_INPUTS = 0xE00B;
	public static final int SW_TOO_MANY_DATA_INPUTS = 0xE00C;
	public static final int SW_TOO_MANY_INPUT_FRAMES = 0xE00D;
	public static final int SW_TOO_MANY_OUTPUTS = 0xE00E;
	public static final int SW_HASHER_ERROR = 0xE00F;
	public static final int SW_BUFFER_ERROR = 0xE010;
	public static final int SW_U64_OVERFLOW = 0xE011;
	public static final int SW_BIP32_BAD_PATH = 0xE012;
	public static final int SW_INTERNAL_CRYPTO_ERROR = 0xE013;
	public static final int SW_NOT_ENOUGH_DATA = 0xE014;
	public static final int SW_TOO_MUCH_DATA = 0xE015;
	public static final int SW_ADDRESS_GENERATION_FAILED = 0xE016;
	public static final int SW_SCHNORR_SIGNING_FAILED = 0xE017;
	public static final int SW_BAD_FRAME_SIGNATURE = 0xE018;
	public static final int SW_BAD_NET_TYPE_VALUE = 0xE019;
	public static final int SW_SMALL_CHUNK = 0xE01A;

	public static final int SW_BIP32_FORMATTING_FAILED = 0xE101;
	public static final int SW_ADDRESS_FORMATTING_FAILED = 0xE102;

	private static @Nullable String getName(int sw) {
		try {
			for (Field field : ErgoLedgerException.class.getFields()) {
				if (field.getName().startsWith("SW_") && ((int) field.get(null)) == sw)
					return field.getName().substring(3);
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}
}
