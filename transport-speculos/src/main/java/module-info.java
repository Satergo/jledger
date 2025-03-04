import org.jspecify.annotations.NullMarked;

@NullMarked
module com.satergo.jledger.transport.speculos {

	requires static org.jspecify;
	requires com.satergo.jledger.core;
	exports com.satergo.jledger.transport.speculos;
}