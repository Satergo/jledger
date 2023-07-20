package com.satergo.jledger.protocol.ergo;

import com.satergo.jledger.Expect;

import java.util.HexFormat;
import java.util.List;

public class ErgoResponse {
	private static final HexFormat HEX = HexFormat.of();
	private ErgoResponse() {}

	public record Version(int major, int minor, int patch, boolean debug) {}

	public record AttestedBoxFrame(byte[] boxId, int frameCount, int frameIndex, long value, List<ErgoProtocol.TokenValue> tokens, byte[] attestation, byte[] rawData) {
		public AttestedBoxFrame {
			Expect.length(boxId, 32);
			Expect.ubyte(frameCount);
			Expect.ubyte(frameIndex);
			if (tokens.size() > 4) throw new IllegalArgumentException("max 4 tokens");
			Expect.length(attestation, 16);
		}

		@Override
		public String toString() {
			return String.format(
					"AttestedBoxFrame[boxId=%s, frameCount=%d, frameIndex=%d, value=%d, tokens=%s, attestation=%s]",
					HEX.formatHex(boxId), frameCount, frameIndex, value, tokens, HEX.formatHex(attestation));
		}
	}

	public record ExtendedPublicKey(byte[] compressedPublicKey, byte[] chainCode) {
		public ExtendedPublicKey {
			Expect.length(compressedPublicKey, 33);
			Expect.length(chainCode, 32);
		}
	}
}
