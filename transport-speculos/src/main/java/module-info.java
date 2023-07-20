import org.jspecify.annotations.NullMarked;

@NullMarked
module com.satergo.jledger.transport.speculos {

	requires org.jspecify;
	requires com.satergo.jledger.core;
	exports com.satergo.jledger.transport.speculos;
}