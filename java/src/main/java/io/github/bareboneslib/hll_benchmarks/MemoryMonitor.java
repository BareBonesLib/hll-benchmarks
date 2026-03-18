package io.github.bareboneslib.hll_benchmarks;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

class MemorySnapshot {
    public long timestamp;
    public long usedHeap;
    public long committedHeap;
    public long maxHeap;

    public MemorySnapshot(long timestamp, long used, long committed, long max) {
        this.timestamp = timestamp;
        this.usedHeap = used;
        this.committedHeap = committed;
        this.maxHeap = max;
    }
}

public class MemoryMonitor extends Thread {
    private volatile boolean running = true;
    private final List<MemorySnapshot> snapshots = new ArrayList<>();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();


    @Override
    public void run() {
        while (running) {
            try {
                collectMemoryUsage();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void collectMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long currentTime = System.nanoTime();

        synchronized (snapshots) {
            snapshots.add(new MemorySnapshot(
                    currentTime,
                    heapUsage.getUsed(),
                    heapUsage.getCommitted(),
                    heapUsage.getMax()
            ));
        }
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }

    public List<MemorySnapshot> getSnapshots() {
        return snapshots;
    }

    // Example usage
    public static void main(String[] args) {
        MemoryMonitor monitor = new MemoryMonitor();
        monitor.start();

        // Simulate application work
        try {
            List<byte[]> data = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                data.add(new byte[1024 * 1024]); // Allocate 1MB
                Thread.sleep(500);
            }
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Shutdown and print report
        monitor.shutdown();
        try {
            monitor.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(monitor.getSnapshots().toString());
    }
}