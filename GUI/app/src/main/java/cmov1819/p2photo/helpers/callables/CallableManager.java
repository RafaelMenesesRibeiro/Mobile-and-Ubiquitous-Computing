package cmov1819.p2photo.helpers.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static cmov1819.p2photo.helpers.managers.LogManager.CALLABLE_MGR_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;

public class CallableManager implements Callable<Object> {
    protected Callable<Object> callable;
    protected long timeout;
    protected TimeUnit timeUnit;

    public CallableManager(Callable<Object> callable, long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.callable = callable;
    }

    @Override
    public Object call() {
        Object result = null;
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            result = exec.submit(callable).get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException exc) {
            if (exc instanceof TimeoutException) {
                logWarning(CALLABLE_MGR_TAG, "Peer took too long to reply...");
            } else {
                logError(CALLABLE_MGR_TAG, exc.getMessage());
            }
        }
        exec.shutdown();
        return result;
    }
}
