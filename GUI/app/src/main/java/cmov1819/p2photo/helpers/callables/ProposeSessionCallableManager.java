package cmov1819.p2photo.helpers.callables;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pt.inesc.termite.wifidirect.SimWifiP2pDevice;

import static cmov1819.p2photo.helpers.managers.LogManager.CALLABLE_MGR_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;

public class ProposeSessionCallableManager implements Callable<SimWifiP2pDevice> {
    protected Callable<SimWifiP2pDevice> callable;
    protected long timeout;
    protected TimeUnit timeUnit;

    public ProposeSessionCallableManager(Callable<SimWifiP2pDevice> callable, long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.callable = callable;
    }

    @Override
    public SimWifiP2pDevice call() {
        SimWifiP2pDevice result = ((ProposeSessionCallable) callable).getTargetDevice();
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            // Returns null when inner task finishes successfully else ends with device for reattempt
            result = exec.submit(callable).get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException exc) {
            if (exc instanceof TimeoutException) {
                logWarning(CALLABLE_MGR_TAG, "Peer took too long to reply...");
            } else {
                logError(CALLABLE_MGR_TAG, exc.getMessage());
            }
            return result;
        }
        exec.shutdown();
        return result;
    }
}
