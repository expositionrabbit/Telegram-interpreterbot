package interpreterbot;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** A class that runs {@link Task}s and provides timeout utility.*/
public class Tasks {
	
	private final static ExecutorService service = Executors.newCachedThreadPool();
	private final static ScheduledExecutorService stoppers = Executors.newScheduledThreadPool(1);
	
	private Tasks() { throw new AssertionError(); }
	
	/** Runs a {@code Task}, never terminating.
	 * <p>Actually terminates in 9223372036854775807 years, or about a
	 * million times the age of the universe, but this situation should seldom
	 * occur.*/
	public static <T> SafeFuture<T> run(Task<T> task) {
		return timeout(task, Long.MAX_VALUE, TimeUnit.DAYS);
	}
	
	/** Runs a {@code Task}, terminating after a specific amount of time.
	 * <p>The task handles providing the return value in case of premature cancellation.
	 * @param task the {@code Task} to run
	 * @param timeout the amount of time before a timeout
	 * @param unit the used unit of time*/
	public static <T> SafeFuture<T> timeout(Task<T> task, long timeout, TimeUnit unit) {
		Future<T> taskFuture = service.submit(task);
		
		Future<T> wrapperFuture = service.submit(() -> {
			stoppers.schedule(() -> {
				taskFuture.cancel(true);
			}, timeout, unit);
			
			try {
				return taskFuture.get();
			} catch(CancellationException e) {
				return task.onCancel().get();
			} catch(ExecutionException e) {
				return task.onError().apply(e.getCause());
			}
		});
		
		return SafeFuture.of(wrapperFuture);
	}
	
	/** Quits all tasks initiated.
	 * @throws InterruptedException When the thread is interrupted */
	public static void quitAll() throws InterruptedException {
		service.shutdown();
		service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		stoppers.shutdown();
		stoppers.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}
}