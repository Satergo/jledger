# jledger

Ledger hardware cryptocurrency wallet key library for Java.

## Usage
Use hid4java to find your device, then `LedgerDevice ledger = new HidLedgerDevice(hidDevice);`.

Then you can do `ledger.open();`, and define your protocol using for example (Ergo) `ErgoProtocol protocol = new ErgoProtocol(ledger);`.

Finally, you can call any method you want for example `protocol.getVersion()`.

## Artifacts

Note: No artifacts are published yet. You need to build them yourself.

### For desktop programs
Use `com.satergo.jledger:core:VERSION` and `com.satergo.jledger:transport-hid4java:VERSION` (class HidLedgerDevice). [Hid4java](https://github.com/gary-rowe/hid4java) is used.

For testing with the [Speculos](https://speculos.ledger.com/) emulator, use `com.satergo.jledger:transport-speculos:VERSION` (class SpeculosLedgerDevice).

### Implementing a custom transport or protocol
If you only want the core library, use `com.satergo.jledger:core:VERSION`. Use this if your project is an app protocol or if you are developing for another platform.

### App Protocols
An app protocol is the lowest-level access to an app on the Ledger device. Everything in the protocol is implemented 1:1.
- ergo (`com.satergo.jledger:protocol-ergo:VERSION`)

### Library Integrations
Integrations with various cryptocurrency Java libraries. These would be artifacts that use the jledger app protocols and work with classes provided by the crypto library. They are not meant to be integrated into this repository.

## Example
```java
public class Example {
	public static void main(String[] args) {
		HidServicesSpecification hidSpec = new HidServicesSpecification();
		hidSpec.setAutoStart(false);

		// Scan for devices
		HidServices hidServices = HidManager.getHidServices(hidSpec);
		hidServices.start();

		for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
			if (LedgerDevice.isLedgerDevice(hidDevice.getVendorId(), hidDevice.getProductId())) {
				handleLedgerDevice(hidDevice);
			}
			System.out.println(hidDevice);
		}

		hidServices.stop();
	}
	
	private static void handleLedgerDevice(HidDevice hidDevice) {
		HidLedgerDevice device = new HidLedgerDevice(hidDevice);
		device.open();
		ErgoProtocol protocol = new ErgoProtocol(device);
		
		System.out.println("Ergo app version: " + protocol.getVersion());
		device.close();
	}
}
```