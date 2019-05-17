package cmov1819.p2photo.helpers.callables;

import org.json.JSONObject;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;

public class CallableManager implements Callable<JSONObject> {
    private static final String CALLABLE_MGR_TAG = "CALLABLE MANAGER";
    protected Callable<JSONObject> callable;
    protected long timeout;
    protected TimeUnit timeUnit;

    public CallableManager(Callable<JSONObject> callable, long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.callable = callable;
    }

    @Override
    public JSONObject call() {
        JSONObject result;
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            result = exec.submit(callable).get(timeout, timeUnit);
            exec.shutdown();
            return result;
        } catch (InterruptedException | ExecutionException | TimeoutException exc) {
            if (exc instanceof TimeoutException) {
                logWarning(CALLABLE_MGR_TAG, "Peer took too long to reply...");
            } else {
                logWarning(CALLABLE_MGR_TAG, exc.getMessage());
            }
            exec.shutdown();
            return null;
        }
    }
}
