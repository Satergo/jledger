package com.satergo.jledger.protocol.ergo;

import com.satergo.jledger.APDUCommand;
import com.satergo.jledger.APDUResponse;
import com.satergo.jledger.AppProtocol;
import com.satergo.jledger.LedgerDevice;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Lowest-level protocol access. Abstractions can be built on top to for example re-run methods with limited parameter lengths.
 * Many int parameters are actually cast to other types, check their javadoc.
 *
 * Check <a href="https://docs.google.com/document/d/1z8nIlRmPhwcKzyZ2jZYYkaFDtLt8pgvnRWrT5zJJ_Tw/edit?pli=1#">"Ergo Ledger App Protocol v5"</a> for documentation.
 */
@SuppressWarnings("unused")
public final class ErgoProtocol extends AppProtocol {

	public static final int CLA = 0xE0;
	public static final int RESULT_OK = 0x9000;

	private static void putBip32Path(ByteBuffer buffer, long[] bip32Path) {
		buffer.put((byte) bip32Path.length);
		for (long derivationIndex : bip32Path) {
			buffer.putInt((int) derivationIndex);
		}
	}
	
	public ErgoProtocol(LedgerDevice device) {
		super(device);
	}

	public record TokenId(byte[] bytes) {
		public TokenId(byte[] bytes) {
			if (bytes.length != 32) throw new IllegalArgumentException();
			this.bytes = bytes.clone();
		}
		@Override public byte[] bytes() {
			return bytes.clone();
		}
	}

	public ErgoResponse.Version getVersion() {
		device.writeAPDU(new APDUCommand(CLA, 0x01, 0x00, 0x00, true));
		APDUResponse apdu = device.readAPDU(4);
		return new ErgoResponse.Version(
				apdu.getDataByte(0) & 0xFF,
				apdu.getDataByte(1) & 0xFF,
				apdu.getDataByte(2) & 0xFF,
				apdu.getDataByte(3) == 0x01);
	}

	/**
	 * @param bip32Path unsigned integers, 2-10 inclusive
	 */
	public ErgoResponse.ExtendedPublicKey getExtendedPublicKey(Integer optionalAuthToken, long... bip32Path) throws ErgoException {
		if (bip32Path.length < 2 || bip32Path.length > 10) throw new IllegalArgumentException("2-10 inc.");
		ByteBuffer buffer = ByteBuffer.allocate(1 + bip32Path.length * 4 + (optionalAuthToken != null ? 4 : 0));
		putBip32Path(buffer, bip32Path);
		if (optionalAuthToken != null) buffer.putInt(optionalAuthToken);
		device.writeAPDU(new APDUCommand(CLA, 0x10, optionalAuthToken != null ? 0x02 : 0x01, 0x00, buffer.array()));
		APDUResponse apduResponse = checkError(device.readAPDU(65));
		return new ErgoResponse.ExtendedPublicKey(apduResponse.getDataRange(0, 33), apduResponse.getDataRange(33, 65));
	}

	public enum DerivationAction {
		RETURN(0x01),
		DISPLAY(0x02);

		private final int code; DerivationAction(int code) { this.code = code; }
	}

	/**
	 * @param networkType unsigned byte
	 * @param bip32Path unsigned integers, 5-10 inclusive
	 */
	public byte[] deriveAddress(
			DerivationAction action,
			int networkType, Integer optionalAuthToken, long... bip32Path) throws ErgoException {
		if (bip32Path.length < 5 || bip32Path.length > 10) throw new IllegalArgumentException("5-10 inc.");
		ByteBuffer buffer = ByteBuffer.allocate(2 + bip32Path.length * 4 + (optionalAuthToken != null ? 4 : 0))
				.put((byte) networkType);
		putBip32Path(buffer, bip32Path);
		if (optionalAuthToken != null) buffer.putInt(optionalAuthToken);
		device.writeAPDU(new APDUCommand(CLA, 0x11, action.code, optionalAuthToken != null ? 0x02 : 0x01, buffer.array()));
		return checkError(device.readAPDU(action == DerivationAction.RETURN ? 38 : 0))
				.getData();
	}

	/**
	 * @param boxIndex unsigned short
	 * @param tokensCount unsigned byte
	 * @return Random session ID
	 */
	public int boxStart(byte[] transactionId, int boxIndex, long boxValue, int ergoTreeSize, int creationHeight, int tokensCount, int additionalRegistersSizeBytes, Integer optionalAuthToken) throws ErgoException {
		requireLength(transactionId, 32);
		ByteBuffer buffer = ByteBuffer.allocate(optionalAuthToken == null ? 55 : 59);
		buffer.put(transactionId)
				.putShort((short) boxIndex)
				.putLong(boxValue)
				.putInt(ergoTreeSize)
				.putInt(creationHeight)
				.put((byte) tokensCount)
				.putInt(additionalRegistersSizeBytes);
		if (optionalAuthToken != null) buffer.putInt(optionalAuthToken);
		device.writeAPDU(new APDUCommand(CLA, 0x20, 0x01, optionalAuthToken != null ? 0x02 : 0x01, buffer.array()));
		return checkError(device.readAPDU(1))
				.getDataByte(0) & 0xFF;
	}

	public Optional<Integer> addErgoTreeChunk(int sessionId, byte[] ergoTreeChunk) throws ErgoException {
		if (ergoTreeChunk.length > 255) throw new IllegalArgumentException("max length 255");
		device.writeAPDU(new APDUCommand(CLA, 0x20, 0x02, sessionId, ergoTreeChunk));
		return emptyOrOneUnsignedByte();
	}

	public Optional<Integer> addTokens(int sessionId, LinkedHashMap<byte[], Long> tokens) throws ErgoException {
		if (tokens.size() > 6) throw new IllegalArgumentException("max tokens size 6");
		ByteBuffer buffer = ByteBuffer.allocate(tokens.size() * 32);
		for (Map.Entry<byte[], Long> token : tokens.entrySet()) {
			requireLength(token.getKey(), 32);
			buffer.put(token.getKey());
			buffer.putLong(token.getValue());
		}
		device.writeAPDU(new APDUCommand(CLA, 0x20, 0x03, sessionId, buffer.array()));
		return emptyOrOneUnsignedByte();
	}

	public Optional<Integer> addRegistersChunk(int sessionId, byte[] registersChunk) throws ErgoException {
		if (registersChunk.length > 255) throw new IllegalArgumentException("max length 255");
		device.writeAPDU(new APDUCommand(CLA, 0x20, 0x04, sessionId, registersChunk));
		return emptyOrOneUnsignedByte();
	}

	/**
	 * @param frameId unsigned byte
	 */
	public ErgoResponse.AttestedBoxFrame getAttestedBoxFrame(int sessionId, int frameId) throws ErgoException {
		device.writeAPDU(new APDUCommand(CLA, 0x20, 0x05, sessionId, new byte[] { (byte) frameId }));
		byte[] bytes = new byte[257];
		int read = device.read(bytes);
		int sw = ((bytes[read - 2] & 0xFF) << 8) | (bytes[read - 1] & 0xFF);
		if (sw != RESULT_OK) throw new ErgoException(sw);
		ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, read - 2);
		byte[] boxId = new byte[32];
		buffer.get(boxId);
		int framesCount = buffer.get() & 0xFF;
		int frameIndex = buffer.get() & 0xFF;
		long amount = buffer.getLong();
		int tokenCount = buffer.get() & 0xFF;
		LinkedHashMap<TokenId, Long> tokens = new LinkedHashMap<>();
		for (int i = 0; i < tokenCount; i++) {
			byte[] tokenId = new byte[32];
			buffer.get(tokenId);
			tokens.put(new TokenId(tokenId), buffer.getLong());
		}
		byte[] attestation = new byte[16];
		buffer.get(attestation);
		return new ErgoResponse.AttestedBoxFrame(boxId, framesCount, frameIndex, amount, Collections.unmodifiableMap(tokens), attestation);
	}

	// SIGN TRANSACTION (0x21)

	/**
	 * @param bip32Path unsigned integers, 5-10 inclusive
	 * @return signature (56 bytes)
	 */
	public int startP2PKSigning(Integer optionalAuthToken, long... bip32Path) throws ErgoException {
		if (bip32Path.length < 5 || bip32Path.length > 10) throw new IllegalArgumentException("5-10 inc.");
		ByteBuffer buffer = ByteBuffer.allocate(1 + bip32Path.length * 4 + (optionalAuthToken != null ? 4 : 0));
		putBip32Path(buffer, bip32Path);
		if (optionalAuthToken != null) buffer.putInt(optionalAuthToken);
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x01, optionalAuthToken != null ? 0x02 : 0x01, buffer.array()));
		return checkError(device.readAPDU(1))
				.getDataByte(0);
	}


	/**
	 * @param txInputAmount unsigned short
	 * @param txDataInputAmount unsigned short
	 * @param txDistinctTokenIdAmount unsigned byte
	 * @param txOutputAmount unsigned short
	 */
	public void startTransaction(int sessionId, int txInputAmount, int txDataInputAmount, int txDistinctTokenIdAmount, int txOutputAmount) throws ErgoException {
		ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 1 + 2)
				.putShort((short) txInputAmount)
				.putShort((short) txDataInputAmount)
				.put((byte) txDistinctTokenIdAmount)
				.putShort((short) txOutputAmount);
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x10, sessionId, buffer.array()));
		voidCheckSuccess();
	}

	public void addTokenIds(int sessionId, byte[][] tokenIds) throws ErgoException {
		if (tokenIds.length > 7) throw new IllegalArgumentException("max token ids 7");
		ByteBuffer buffer = ByteBuffer.allocate(tokenIds.length * 32);
		for (byte[] tokenId : tokenIds) {
			requireLength(tokenId, 32);
			buffer.put(tokenId);
		}
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x11, sessionId, buffer.array()));
		voidCheckSuccess();
	}

	/**
	 * @param framesCount unsigned byte
	 * @param frameIndex unsigned byte
	 * @param tokens tokenId&lt;-&gt;tokenAmount
	 */
	public void addInputBoxFrame(int sessionId, byte[] boxId, int framesCount, int frameIndex, long amount, LinkedHashMap<byte[], Long> tokens, byte[] attestation, int contentExtensionLength) throws ErgoException {
		requireLength(boxId, 32);
		ByteBuffer buffer = ByteBuffer.allocate(32 + 1 + 1 + 8 + 1 + tokens.size() * (32 + 8) + 16 + 4)
				.put(boxId)
				.put((byte) framesCount)
				.put((byte) frameIndex)
				.putLong(amount);
		if (tokens.size() > 4) throw new IllegalArgumentException("max tokens 4");
		buffer.put((byte) tokens.size());
		for (Map.Entry<byte[], Long> token : tokens.entrySet()) {
			requireLength(token.getKey(), 32);
			buffer.put(token.getKey());
			buffer.putLong(token.getValue());
		}
		requireLength(attestation, 16);
		buffer.put(attestation);
		// The protocol says that this can only be sent with Frame 0, but should it be 0 or not present at all in other frames?
		buffer.putInt(contentExtensionLength);
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x12, sessionId, buffer.array()));
		voidCheckSuccess();
	}

	public void addInputBoxContextExtensionChunk(int sessionId, byte[] chunk) throws ErgoException {
		if (chunk.length > 255) throw new IllegalArgumentException("max length 255");
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x13, sessionId, chunk));
		voidCheckSuccess();
	}

	public void addDataInputs(int sessionId, byte[][] boxIds) throws ErgoException {
		if (boxIds.length > 7) throw new IllegalArgumentException("max length 7");
		ByteBuffer buffer = ByteBuffer.allocate(boxIds.length * 32);
		for (byte[] boxId : boxIds) {
			requireLength(boxId, 32);
			buffer.put(boxId);
		}
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x14, sessionId, buffer.array()));
		voidCheckSuccess();
	}

	/**
	 * @param tokensCount unsigned byte
	 */
	public void addOutputBoxStart(int sessionId, long boxValue, int ergoTreeSizeBytes, int creationHeight, int tokensCount, int additionalRegistersSizeBytes) throws ErgoException {
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x15, sessionId, ByteBuffer.allocate(21)
				.putLong(boxValue)
				.putInt(ergoTreeSizeBytes)
				.putInt(creationHeight)
				.put((byte) tokensCount)
				.putInt(additionalRegistersSizeBytes).array()));
		voidCheckSuccess();
	}

	public void addOutputBoxErgoTreeChunk(int sessionId, byte[] bytes) throws ErgoException {
		if (bytes.length > 255) throw new IllegalArgumentException("max length 255");
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x16, sessionId, bytes));
		voidCheckSuccess();
	}

	public void addOutputBoxMinersFeeTree(int sessionId, ErgoNetworkType networkType) throws ErgoException {
		Objects.requireNonNull(networkType);
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x17, sessionId, new byte[] { (byte) (networkType == ErgoNetworkType.MAINNET ? 0x01 : 0x02) }));
		voidCheckSuccess();
	}

	/**
	 * @param bip32Path unsigned integers, 2-10 inclusive
	 */
	public void addOutputBoxChangeTree(int sessionId, long... bip32Path) throws ErgoException {
		if (bip32Path.length < 2 || bip32Path.length > 10) throw new IllegalArgumentException("2-10 inc.");
		ByteBuffer buffer = ByteBuffer.allocate(bip32Path.length * 4);
		putBip32Path(buffer, bip32Path);
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x18, sessionId, buffer.array()));
		voidCheckSuccess();
	}

	public void addOutputBoxTokens(int sessionId, LinkedHashMap<Integer, Long> tokens) throws ErgoException {
		// TODO size limit
		ByteBuffer buffer = ByteBuffer.allocate(tokens.size() * 12);
		tokens.forEach((index, value) -> {
			buffer.putInt(index);
			buffer.putLong(value);
		});
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x19, sessionId, buffer.array()));
		voidCheckSuccess();
	}

	public void addOutputBoxRegistersChunk(int sessionId, byte[] registersChunk) throws ErgoException {
		if (registersChunk.length > 255) throw new IllegalArgumentException("max length 255");
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x1A, sessionId, registersChunk));
		voidCheckSuccess();
	}

	/**
	 * @return signature (56 bytes)
	 */
	public byte[] confirmAndSign(int sessionId) throws ErgoException {
		device.writeAPDU(new APDUCommand(CLA, 0x21, 0x20, sessionId, true));
		return checkError(device.readAPDU(56))
				.getData();
	}


	private void voidCheckSuccess() throws ErgoException {
		super.voidCheckError(RESULT_OK, ErgoException::new);
	}

	private APDUResponse checkError(APDUResponse response) throws ErgoException {
		return super.checkError(RESULT_OK, response, ErgoException::new);
	}

	private Optional<Integer> emptyOrOneUnsignedByte() throws ErgoException {
		APDUResponse response = device.readAPDU(1);
		if (response.getNr() == 0) {
			checkError(response);
			return Optional.empty();
		}
		return Optional.of(checkError(response)
				.getDataByte(0) & 0xFF);
	}
}
