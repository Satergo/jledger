# jledger

Ledger hardware cryptocurrency wallet key library for Java.

## Usage
Use hid4java to find your device, then `LedgerDevice ledger = new HidLedgerDevice(hidDevice);`.

Then you can do `ledger.open();`, and define your protocol using for example (Ergo) `ErgoProtocol protocol = new ErgoProtocol(ledger);`.

Finally, you can call any method you want for example `protocol.getVersion()`.

## Artifacts

### For desktop programs
Use `com.satergo.jledger:core:VERSION` and `com.satergo.jledger:transport-hid:VERSION` (class HidLedgerDevice). [Hid4java](https://github.com/gary-rowe/hid4java) is used.

For testing with the [Speculos](https://speculos.ledger.com/) emulator, use `com.satergo.jledger:transport-speculos:VERSION` (class EmulatorLedgerDevice).

### Implementing custom transport or protocol
If you only want the core library, use `com.satergo.jledger:core:VERSION`. Use this if your project is an app protocol or if you are developing for another platform.

### App Protocols
These are the lowest-level access to the devices. Everything in the protocol is implemented 1:1.
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