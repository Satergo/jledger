package com.satergo.jledger.protocol.ergo;

import java.util.Map;

public class ErgoResponse {
	private ErgoResponse() {}

	public record Version(int major, int minor, int patch, boolean debug) {}

	public record AttestedBoxFrame(byte[] boxId, int framesCount, int frameIndex, long amount, Map<ErgoProtocol.TokenId, Long> tokens, byte[] attestation) {
		public AttestedBoxFrame {
			if (boxId.length != 32) throw new IllegalArgumentException("boxId must be length 32");
			if (attestation.length != 16) throw new IllegalArgumentException("attestation must be length 16");
		}

		@Override public byte[] boxId() {
			return boxId.clone();
		}

		@Override public byte[] attestation() {
			return attestation.clone();
		}
	}

	public record ExtendedPublicKey(byte[] compressedPublicKey, byte[] chainCode) {
		public ExtendedPublicKey {
			if (compressedPublicKey.length != 33) throw new IllegalArgumentException("compressedPublicKey must be length 33");
			if (chainCode.length != 32) throw new IllegalArgumentException("chainCode must be length 32");
		}

		@Override public byte[] compressedPublicKey() {
			return compressedPublicKey.clone();
		}

		@Override public byte[] chainCode() {
			return chainCode.clone();
		}
	}
}
