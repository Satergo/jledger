# jledger

Ledger hardware cryptocurrency wallet key library for Java.

## Usage
First you need to obtain and `open()` an instance of LedgerDevice using one of the transport libraries.

Then you can instantiate a protocol class like for example `new ErgoProtocol(ledgerDevice)` and use the methods available.

## Artifacts

Note: No artifacts are published on Maven Central yet. You need to build them yourself.

The core artifact is `com.satergo.jledger:core:VERSION`

### App Protocols
An app protocol is the lowest-level access to an app on the Ledger device. Everything in the protocol is implemented 1:1.
- ergo (`com.satergo.jledger:protocol-ergo:VERSION`)

### Transports
- HID with [hid4java][https://github.com/gary-rowe/hid4java]: `com.satergo.jledger:transport-hid4java:VERSION` (class Hid4javaLedgerDevice) (Linux/Windows/Mac)
- [Speculos emulator](https://speculos.ledger.com/): `com.satergo.jledger.transport-speculos:VERSION` (class SpeculosLedgerDevice)

### Implementing a custom transport or protocol
Use the core library if you are implementing an app protocol or a transport library.

### Library Integrations
Integrations with various Java cryptocurrency libraries. These would be artifacts that use the jledger app protocols and work with classes provided by the crypto library. They are not meant to be integrated into this repository.