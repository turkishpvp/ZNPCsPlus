package lol.pyr.znpcsplus.util;

import org.bukkit.entity.Player;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public abstract class Viewable {
    private final static List<WeakReference<Viewable>> all = Collections.synchronizedList(new ArrayList<>());
    private static ExecutorService visibilityExecutor = createExecutor();

    public static List<Viewable> all() {
        synchronized (all) {
            all.removeIf(reference -> reference.get() == null);
            return all.stream()
                    .map(Reference::get)
                    .collect(Collectors.toList());
        }
    }

    public static void shutdown() {
        synchronized (all) {
            for (WeakReference<Viewable> reference : all) {
                Viewable viewable = reference.get();
                if (viewable == null) continue;
                viewable.UNSAFE_hideAll();
                viewable.viewers.clear();
            }
            all.clear();
        }
        visibilityExecutor.shutdown();
        try {
            if (!visibilityExecutor.awaitTermination(5, TimeUnit.SECONDS)) visibilityExecutor.shutdownNow();
        } catch (InterruptedException e) {
            visibilityExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private final Set<Player> viewers = ConcurrentHashMap.newKeySet();

    public Viewable() {
        all.add(new WeakReference<>(this));
    }

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ZNPCsPlus Viewable Visibility");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static synchronized ExecutorService getVisibilityExecutor() {
        if (visibilityExecutor.isShutdown() || visibilityExecutor.isTerminated()) visibilityExecutor = createExecutor();
        return visibilityExecutor;
    }

    public void delete() {
        getVisibilityExecutor().submit(() -> {
            UNSAFE_hideAll();
            viewers.clear();
            synchronized (all) {
                all.removeIf(reference -> reference.get() == null || reference.get() == this);
            }
        });
    }

    public CompletableFuture<Void> respawn() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        getVisibilityExecutor().submit(() -> {
            UNSAFE_hideAll();
            UNSAFE_showAll().thenRun(() -> future.complete(null));
        });
        return future;
    }

    public CompletableFuture<Void> respawn(Player player) {
        hide(player);
        return show(player);
    }

    public CompletableFuture<Void> show(Player player) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        getVisibilityExecutor().submit(() -> {
            if (viewers.contains(player)) {
                future.complete(null);
                return;
            }
            viewers.add(player);
            UNSAFE_show(player).thenRun(() -> future.complete(null));
        });
        return future;
    }

    public void hide(Player player) {
        getVisibilityExecutor().submit(() -> {
            if (!viewers.contains(player)) return;
            viewers.remove(player);
            UNSAFE_hide(player);
        });
    }

    public void UNSAFE_removeViewer(Player player) {
        viewers.remove(player);
    }

    protected void UNSAFE_hideAll() {
        for (Player viewer : viewers) UNSAFE_hide(viewer);
    }

    protected CompletableFuture<Void> UNSAFE_showAll() {
        return FutureUtil.allOf(viewers.stream()
                .map(this::UNSAFE_show)
                .collect(Collectors.toList()));
    }

    public Set<Player> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    public boolean isVisibleTo(Player player) {
        return viewers.contains(player);
    }

    protected abstract CompletableFuture<Void> UNSAFE_show(Player player);

    protected abstract void UNSAFE_hide(Player player);
}
