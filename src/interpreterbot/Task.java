package interpreterbot;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/** A {@code Task} is a {@code Callable} prepared for situations
 * where it is interrupted by an exception or cancelled due to some
 * reason.
 * @param <T> The type of the return value*/
public class Task<T> implements Callable<T> {
	
	private final Callable<T> callable;
	private final Function<Throwable, T> onError;
	private final Supplier<T> onCancel;
	
	/** Creates an instance.
	 * @param callable the {@code Callable} to run
	 * @param onError a function that provides a return value upon an exceptional situation
	 * @param onCancel a supplier that provides a return value upon cancellation*/
	public Task(Callable<T> callable, Function<Throwable, T> onError, Supplier<T> onCancel) {
		this.callable = callable;
		this.onError = onError;
		this.onCancel = onCancel;
	}
	
	@Override
	public T call() throws Exception {
		return callable.call();
	}
	
	/** Returns the function that provides a return value upon exceptional situations.*/
	public Function<Throwable, T> onError() { return onError; }
	
	/** Returns the supplier that provides a return value upon cancellation.*/
	public Supplier<T> onCancel() { return onCancel; }
}