import org.jspecify.annotations.NullMarked;

@NullMarked
module com.satergo.jledger.core {

	requires static org.jspecify;
	exports com.satergo.jledger;
}