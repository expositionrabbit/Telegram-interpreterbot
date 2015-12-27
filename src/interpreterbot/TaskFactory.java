package interpreterbot;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/** A {@code TaskFactory} is an utility class for creating {@link Task}
 * instances with similar configuration.*/
public class TaskFactory<T> {
	
	private final Function<Throwable, T> onError;
	private final Supplier<T> onCancel;
	
	/** Creates an instance.
	 * @param onError a function that provides a return value when an exception is encountered
	 * @param onCancel a supplier that provides a return value when cancelled*/
	public TaskFactory(Function<Throwable, T> onError, Supplier<T> onCancel) {
		this.onError = onError;
		this.onCancel = onCancel;
	}
	
	/** Creates a new task based on the given configuration.
	 * @param callable the {@code Callable} to run*/
	public Task<T> newTask(Callable<T> callable) {
		return new Task<>(callable, onError, onCancel);
	}
}