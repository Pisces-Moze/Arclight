package io.izzel.arclight.common.mod.server.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.function.Consumer;

final class ArclightScheduledTask implements ScheduledTask, Runnable {

    private final Plugin plugin;
    private final Consumer<ScheduledTask> action;
    private final boolean repeating;
    private volatile BukkitTask task;
    private volatile ExecutionState state = ExecutionState.IDLE;
    private boolean cancelBeforeBind;

    ArclightScheduledTask(Plugin plugin, Consumer<ScheduledTask> action, boolean repeating) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.action = Objects.requireNonNull(action, "action");
        this.repeating = repeating;
    }

    synchronized void bind(BukkitTask task) {
        this.task = Objects.requireNonNull(task, "task");
        if (this.cancelBeforeBind) {
            task.cancel();
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            if (this.state == ExecutionState.CANCELLED
                || this.state == ExecutionState.CANCELLED_RUNNING
                || this.state == ExecutionState.FINISHED) {
                return;
            }
            this.state = ExecutionState.RUNNING;
        }
        try {
            this.action.accept(this);
        } finally {
            synchronized (this) {
                if (this.state == ExecutionState.RUNNING) {
                    this.state = this.repeating ? ExecutionState.IDLE : ExecutionState.FINISHED;
                }
            }
        }
    }

    @Override
    public Plugin getOwningPlugin() {
        return this.plugin;
    }

    @Override
    public boolean isRepeatingTask() {
        return this.repeating;
    }

    @Override
    public synchronized CancelledState cancel() {
        return switch (this.state) {
            case FINISHED -> CancelledState.ALREADY_EXECUTED;
            case CANCELLED -> CancelledState.CANCELLED_ALREADY;
            case CANCELLED_RUNNING -> this.repeating
                ? CancelledState.NEXT_RUNS_CANCELLED_ALREADY : CancelledState.RUNNING;
            case RUNNING -> {
                if (!this.repeating) {
                    yield CancelledState.RUNNING;
                }
                this.state = ExecutionState.CANCELLED_RUNNING;
                cancelDelegate();
                yield CancelledState.NEXT_RUNS_CANCELLED;
            }
            case IDLE -> {
                this.state = ExecutionState.CANCELLED;
                cancelDelegate();
                yield CancelledState.CANCELLED_BY_CALLER;
            }
        };
    }

    private void cancelDelegate() {
        if (this.task == null) {
            this.cancelBeforeBind = true;
        } else {
            this.task.cancel();
        }
    }

    @Override
    public ExecutionState getExecutionState() {
        return this.state;
    }
}
