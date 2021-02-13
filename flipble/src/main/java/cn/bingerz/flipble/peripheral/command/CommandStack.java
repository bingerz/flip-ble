package cn.bingerz.flipble.peripheral.command;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import cn.bingerz.flipble.utils.EasyLog;

/**
 * @author hanson
 */
public class CommandStack {

    private PriorityBlockingQueue<Command> commandQueue = new PriorityBlockingQueue<>(10, new comparator());

    private class comparator implements Comparator<Command> {
        @Override
        public int compare(Command o1, Command o2) {
            if (o1.getPriority() != o2.getPriority()) {
                return o1.getPriority() - o2.getPriority();
            } else {
                if (o1.getMethod() != o2.getMethod()) {
                    return o1.getMethod() - o2.getMethod();
                } else {
                    if (!o1.getKey().equals(o2.getKey())) {
                        return o1.getKey().compareTo(o2.getKey());
                    } else {
                        return 0;
                    }
                }
            }
        }
    }

    private PriorityBlockingQueue<Command> getCommandQueue() {
        if (commandQueue == null) {
            commandQueue = new PriorityBlockingQueue<>();
        }
        return commandQueue;
    }

    public boolean isContains(Command command) {
        PriorityBlockingQueue<Command> queue = getCommandQueue();
        return queue.contains(command);
    }

    public int size() {
        PriorityBlockingQueue<Command> queue = getCommandQueue();
        return queue.size();
    }

    public void add(Command command) {
        if (command != null && command.isValid()) {
            PriorityBlockingQueue<Command> queue = getCommandQueue();
            queue.offer(command);
        } else {
            EasyLog.e("Add fail, command is invalid. cmd=%s", command);
        }
    }

    public Command poll() {
        PriorityBlockingQueue<Command> queue = getCommandQueue();
        return queue.poll();
    }

    public void remove(Command command) {
        PriorityBlockingQueue<Command> queue = getCommandQueue();
        queue.remove(command);
    }

    public void printQueue() {
        int size = getCommandQueue().size();
        EasyLog.i("PrintQueue size=%s", size);
        for (int i = 0; i < size; i++) {
            Command command = poll();
            EasyLog.i("PrintQueue index=%s cmd=%s", i, command);
        }
    }
}
