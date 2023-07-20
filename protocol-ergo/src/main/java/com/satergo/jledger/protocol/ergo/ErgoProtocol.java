package com.satergo.jledger.protocol.ergo;

import com.satergo.jledger.*;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.satergo.jledger.protocol.ergo.Utils.*;

/**
 * Lowest-level protocol access, mostly intended for implementing library integration.
 * Many numeric parameters are actually cast to other types, check their javadoc.
 *
 * @see <a href="https://github.com/LedgerHQ/app-ergo/tree/develop/doc">The documentation of the Ergo Ledger app</a>
 */
public final class ErgoProtocol extends AppProtocol {

	private static final int CLA = 0xE0;
	static final int RESULT_OK = 0x9000;

	public ErgoProtocol(LedgerDevice device) {
		super(device);
	}

	public record TokenId(byte[] bytes) {
		public TokenId(byte[] bytes) {
			Expect.length(bytes, 32);
			this.bytes = bytes.clone();
		}
		@Override public boolean equals(Object o) { return o instanceof TokenId t && Arrays.equals(bytes, t.bytes); }
		@Override public int hashCode() { return Arrays.hashCode(bytes); }
	}
	public record TokenValue(TokenId id, long value) {
		public TokenValue(byte[] id, long value) {
			this(new TokenId(id), value);
		}
	}
	public record TokenIndexValue(int tokenIndex, long value) {}

	public ErgoResponse.Version getAppVersion() {
		APDUResponse apdu = checkError(device.exchange(new APDUCommand(CLA, 0x01, 0x00, 0x00, true)));
		return new ErgoResponse.Version(
				apdu.getDataByte(0) & 0xFF,
				apdu.getDataByte(1) & 0xFF,
				apdu.getDataByte(2) & 0xFF,
				apdu.getDataByte(3) == 0x01);
	}

	/** Must return "Ergo" */
	public String getAppName() {
		APDUResponse apdu = checkError(device.exchange(new APDUCommand(CLA, 0x02, 0x00, 0x00, true)));
		return new String(apdu.getData(), StandardCharsets.US_ASCII);
	}

	/**
	 * @param bip44Path unsigned integers, 2-10 inclusive
	 */
	public ErgoResponse.ExtendedPublicKey getExtendedPublicKey(int[] bip44Path, @Nullable Integer optionalAuthToken) throws ErgoLedgerException {
		if (bip44Path.length < 2 || bip44Path.length > 10) throw new IllegalArgumentException("2-10 inc.");
		ByteBuffer buffer = ByteBuffer.allocate(bip44PathLength(bip44Path) + optTokenLength(optionalAuthToken));
		putBip44Path(buffer, bip44Path);
		putOptToken(buffer, optionalAuthToken);
		APDUCommand apduCommand = new APDUCommand(CLA, 0x10, optionalAuthToken != null ? 0x02 : 0x01, 0x00, buffer.array());
		APDUResponse apduResponse = checkError(device.exchange(apduCommand));
		return new ErgoResponse.ExtendedPublicKey(apduResponse.getDataRange(0, 33), apduResponse.getDataRange(33, 65));
	}

	public enum DerivationAction {
		RETURN(0x01),
		DISPLAY(0x02);

		private final int code; DerivationAction(int code) { this.code = code; }
	}

	/**
	 * @param bip44Path unsigned integers, 5-10 inclusive
	 * @return When {@code action} is {@link DerivationAction#RETURN RETURN}, it returns the address data (38 bytes).
	 * 	For {@link DerivationAction#DISPLAY DISPLAY}, it returns an empty array.
	 */
	public byte[] deriveAddress(
			DerivationAction action,
			ErgoNetworkType networkType, int[] bip44Path, @Nullable Integer optionalAuthToken) throws ErgoLedgerException {
		if (bip44Path.length < 5 || bip44Path.length > 10) throw new IllegalArgumentException("5-10 inc.");
		ByteBuffer buffer = ByteBuffer.allocate(1 + bip44PathLength(bip44Path) + optTokenLength(optionalAuthToken));
		buffer.put(networkType.id);
		putBip44Path(buffer, bip44Path);
		putOptToken(buffer, optionalAuthToken);
		APDUCommand command = new APDUCommand(CLA, 0x11, action.code, optionalAuthToken != null ? 0x02 : 0x01, buffer.array());
		return checkError(device.exchange(command)).getData();
	}

	// ATTEST BOX (0x20)

	/**
	 * @param boxIndex unsigned short
	 * @param tokenCount unsigned byte
	 * @return Random session ID
	 */
	public int attestBoxStart(byte[] transactionId, int boxIndex, long boxValue, int ergoTreeSize, int creationHeight, int tokenCount, int additionalRegistersSizeBytes, @Nullable Integer optionalAuthToken) throws ErgoLedgerException {
		Expect.length(transactionId, 32);
		Expect.ushort(boxIndex);
		Expect.ubyte(tokenCount);
		ByteBuffer buffer = ByteBuffer.allocate(optionalAuthToken == null ? 55 : 59);
		buffer.put(transactionId)
				.putShort((short) boxIndex)
				.putLong(boxValue)
				.putInt(ergoTreeSize)
				.putInt(creationHeight)
				.put((byte) tokenCount)
				.putInt(additionalRegistersSizeBytes);
		putOptToken(buffer, optionalAuthToken);
		APDUCommand command = new APDUCommand(CLA, 0x20, 0x01, optionalAuthToken != null ? 0x02 : 0x01, buffer.array());
		byte dataByte = checkError(device.exchange(command)).getDataByte(0);
		return dataByte & 0xFF;
	}

	/**
	 * @return empty if the box is unfinished, the amount of frames if the box is finished
	 */
	public Optional<Integer> attestAddErgoTreeChunk(int sessionId, byte[] ergoTreeChunk) throws ErgoLedgerException {
		if (ergoTreeChunk.length > 255) throw new IllegalArgumentException("max length 255");
		APDUCommand command = new APDUCommand(CLA, 0x20, 0x02, sessionId, ergoTreeChunk);
		return emptyOrOneUnsignedByte(device.exchange(command));
	}

	public Optional<Integer> attestAddTokens(int sessionId, List<TokenValue> tokens) throws ErgoLedgerException {
		if (tokens.size() > 6) throw new IllegalArgumentException("max 6 tokens");
		ByteBuffer buffer = ByteBuffer.allocate(tokens.size() * 40);
		for (TokenValue token : tokens) {
			buffer.put(token.id.bytes);
			buffer.putLong(token.value);
		}
		APDUCommand command = new APDUCommand(CLA, 0x20, 0x03, sessionId, buffer.array());
		return emptyOrOneUnsignedByte(device.exchange(command));
	}

	public Optional<Integer> attestAddRegistersChunk(int sessionId, byte[] registersChunk) throws ErgoLedgerException {
		if (registersChunk.length > 255) throw new IllegalArgumentException("max length 255");
		APDUCommand command = new APDUCommand(CLA, 0x20, 0x04, sessionId, registersChunk);
		return emptyOrOneUnsignedByte(device.exchange(command));
	}

	/**
	 * @param frameId unsigned byte
	 */
	public ErgoResponse.AttestedBoxFrame getAttestedBoxFrame(int sessionId, int frameId) throws ErgoLedgerException {
		Expect.ubyte(frameId);
		APDUResponse response = checkError(device.exchange(new APDUCommand(CLA, 0x20, 0x05, sessionId, new byte[] { (byte) frameId })));
		ByteBuffer buffer = ByteBuffer.wrap(response.getData());
		byte[] boxId = new byte[32];
		buffer.get(boxId);
		int frameCount = buffer.get() & 0xFF;
		int frameIndex = buffer.get() & 0xFF;
		long amount = buffer.getLong();
		int tokenCount = buffer.get() & 0xFF;
		ArrayList<TokenValue> tokens = new ArrayList<>();
		for (int i = 0; i < tokenCount; i++) {
			byte[] tokenId = new byte[32];
			buffer.get(tokenId);
			tokens.add(new TokenValue(new TokenId(tokenId), buffer.getLong()));
		}
		byte[] attestation = new byte[16];
		buffer.get(attestation);
		if (buffer.position() != buffer.capacity())
			throw new IllegalStateException("Unread data");
		return new ErgoResponse.AttestedBoxFrame(boxId, frameCount, frameIndex, amount, Collections.unmodifiableList(tokens), attestation, response.getData());
	}

	// SIGN TRANSACTION (0x21)

	/**
	 * @param bip44Path unsigned integers, 5-10 inclusive
	 * @return signature (56 bytes)
	 */
	public int startP2PKSigning(ErgoNetworkType networkType, int[] bip44Path, @Nullable Integer optionalAuthToken) throws ErgoLedgerException {
		Objects.requireNonNull(networkType, "networkType");
		if (bip44Path.length < 5 || bip44Path.length > 10) throw new IllegalArgumentException("5-10 inc.");
		ByteBuffer buffer = ByteBuffer.allocate(1 + bip44PathLength(bip44Path) + optTokenLength(optionalAuthToken));
		buffer.put(networkType.id);
		putBip44Path(buffer, bip44Path);
		putOptToken(buffer, optionalAuthToken);
		APDUCommand command = new APDUCommand(CLA, 0x21, 0x01, optionalAuthToken != null ? 0x02 : 0x01, buffer.array());
		return checkError(device.exchange(command)).getDataByte(0);
	}


	/**
	 * @param txInputs unsigned short
	 * @param txDataInputs unsigned short
	 * @param txDistinctTokenIds unsigned byte
	 * @param txOutputs unsigned short
	 */
	public void startTransaction(int sessionId, int txInputs, int txDataInputs, int txDistinctTokenIds, int txOutputs) throws ErgoLedgerException {
		Expect.ushort(txInputs);
		Expect.ushort(txDataInputs);
		Expect.ushort(txDistinctTokenIds);
		Expect.ushort(txOutputs);
		ByteBuffer buffer = ByteBuffer.allocate(2 + 2 + 1 + 2)
				.putShort((short) txInputs)
				.putShort((short) txDataInputs)
				.put((byte) txDistinctTokenIds)
				.putShort((short) txOutputs);
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x10, sessionId, buffer.array())));
	}

	public void addTokenIds(int sessionId, List<byte[]> tokenIds) throws ErgoLedgerException {
		if (tokenIds.size() > 7) throw new IllegalArgumentException("max 7 token ids");
		ByteBuffer buffer = ByteBuffer.allocate(tokenIds.size() * 32);
		for (byte[] tokenId : tokenIds) {
			Expect.length(tokenId, 32);
			buffer.put(tokenId);
		}
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x11, sessionId, buffer.array())));
	}

	public void addInputBoxFrame(int sessionId, ErgoResponse.AttestedBoxFrame attestedBoxFrame, int contentExtensionLength) throws ErgoLedgerException {
		if (attestedBoxFrame.frameIndex() != 0) {
			checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x12, sessionId, attestedBoxFrame.rawData())));
		} else {
			byte[] frameBytes = attestedBoxFrame.rawData();
			ByteBuffer buffer = ByteBuffer.allocate(frameBytes.length + 4)
					.put(frameBytes)
					.putInt(contentExtensionLength);
			checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x12, sessionId, buffer.array())));
		}
	}

	public void addInputBoxContextExtensionChunk(int sessionId, byte[] chunk) throws ErgoLedgerException {
		if (chunk.length > 255) throw new IllegalArgumentException("max length 255");
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x13, sessionId, chunk)));
	}

	public void addDataInputs(int sessionId, List<byte[]> boxIds) throws ErgoLedgerException {
		if (boxIds.size() > 7) throw new IllegalArgumentException("max 7 boxIds");
		ByteBuffer buffer = ByteBuffer.allocate(boxIds.size() * 32);
		for (byte[] boxId : boxIds) {
			Expect.length(boxId, 32);
			buffer.put(boxId);
		}
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x14, sessionId, buffer.array())));
	}

	/**
	 * @param tokenCount unsigned byte
	 */
	public void addOutputBoxStart(int sessionId, long boxValue, int ergoTreeSizeBytes, int creationHeight, int tokenCount, int additionalRegistersSizeBytes) throws ErgoLedgerException {
		Expect.ubyte(tokenCount);
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x15, sessionId, ByteBuffer.allocate(21)
				.putLong(boxValue)
				.putInt(ergoTreeSizeBytes)
				.putInt(creationHeight)
				.put((byte) tokenCount)
				.putInt(additionalRegistersSizeBytes).array())));
	}

	public void addOutputBoxErgoTreeChunk(int sessionId, byte[] bytes) throws ErgoLedgerException {
		if (bytes.length > 255) throw new IllegalArgumentException("max length 255");
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x16, sessionId, bytes)));
	}

	public void addOutputBoxMinerFeeTree(int sessionId) throws ErgoLedgerException {
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x17, sessionId, true)));
	}

	/**
	 * @param bip44Path unsigned integers, 2-10 inclusive
	 */
	public void addOutputBoxChangeTree(int sessionId, int[] bip44Path) throws ErgoLedgerException {
		if (bip44Path.length < 2 || bip44Path.length > 10) throw new IllegalArgumentException("2-10 inc.");
		ByteBuffer buffer = ByteBuffer.allocate(bip44PathLength(bip44Path));
		putBip44Path(buffer, bip44Path);
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x18, sessionId, buffer.array())));
	}

	public void addOutputBoxTokens(int sessionId, List<TokenIndexValue> tokens) throws ErgoLedgerException {
		int bufferSize = tokens.size() * 12;
		if (bufferSize > 255) {
			throw new IllegalArgumentException("Token limit exceeded");
		}
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		for (TokenIndexValue token : tokens) {
			buffer.putInt(token.tokenIndex);
			buffer.putLong(token.value);
		}
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x19, sessionId, buffer.array())));
	}

	public void addOutputBoxRegistersChunk(int sessionId, byte[] registersChunk) throws ErgoLedgerException {
		if (registersChunk.length > 255) throw new IllegalArgumentException("max length 255");
		checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x1A, sessionId, registersChunk)));
	}

	/**
	 * @return signature (56 bytes)
	 */
	public byte[] confirmAndSign(int sessionId) throws ErgoLedgerException {
		return Utils.checkError(device.exchange(new APDUCommand(CLA, 0x21, 0x20, sessionId, true)))
				.getData();
	}
}
