package interpreterbot;

import java.util.concurrent.Future;

/** A {@code SafeFuture} is a {@code Future} that will not throw
 * exceptions.
 * <p>An {@code AssertionError} is thrown if an exception is thrown from
 * the inner {@code Future} instance. This will only happen due to
 * programmer error, as any {@code Future} passed to this class should not
 * throw any exceptions whatsoever.
 * 
 * @param <T> The return type*/
@FunctionalInterface
public interface SafeFuture<T> {
	
	/** Waits for the computation to finish and returns the return value.*/
	public T get();
	
	/** Wraps a {@code SafeFuture} around an ordinary {@code Future}.
	 * @param future the future to wrap, should not throw any exceptions*/
	public static <T> SafeFuture<T> of(Future<T> future) {
		return () -> {
			try {
				return future.get();
			} catch(Exception e) {
				throw new AssertionError("Safe future threw exception");
			}
		};
	}
}