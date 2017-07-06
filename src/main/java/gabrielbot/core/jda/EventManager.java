package gabrielbot.core.jda;

import br.com.brjdevs.highhacks.eventbus.ASMEventBus;
import com.google.common.base.Preconditions;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.IEventManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class EventManager implements IEventManager, Thread.UncaughtExceptionHandler {
    private final List<Object> listeners = new CopyOnWriteArrayList<>();
    private final ASMEventBus eventBus = new ASMEventBus(EventManager.class.getClassLoader(), true);
    private final ExecutorService executor;

    public EventManager(ExecutorService executor) {
        this.executor = Preconditions.checkNotNull(executor);
    }

    public EventManager(int shardId) {
        AtomicInteger number = new AtomicInteger();
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new EventManagerThread(r, shardId, number.incrementAndGet());
            t.setUncaughtExceptionHandler(EventManager.this);
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    @Override
    public void register(Object listener) {
        eventBus.register(listener);
        listeners.add(listener);
    }

    @Override
    public void unregister(Object listener) {
        eventBus.unregister(listener);
        listeners.remove(listener);
    }

    @Override
    public void handle(Event event) {
        executor.submit(()->handleSync(event));
    }

    @Override
    public List<Object> getRegisteredListeners() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Thread.currentThread().setName("Exception Handler");
        eventBus.post(new HandlerExceptionEvent(t, e));
    }

    public void handleSync(Object event) {
        Thread.currentThread().setName(event.getClass().getSimpleName() + " Handler");
        eventBus.post(event);
    }
}
