import org.jspecify.annotations.NullMarked;

@NullMarked
module com.satergo.jledger.transport.hid4java {

	requires org.jspecify;
	requires com.satergo.jledger.core;
	requires hid4java;
	exports com.satergo.jledger.transport.hid4java;
}