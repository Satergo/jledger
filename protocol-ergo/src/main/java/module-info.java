import org.jspecify.annotations.NullMarked;

@NullMarked
module com.satergo.jledger.protocol.ergo {

	requires org.jspecify;
	requires com.satergo.jledger.core;
	exports com.satergo.jledger.protocol.ergo;
}