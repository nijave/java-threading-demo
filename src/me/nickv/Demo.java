/**
 * Java threading example/demo
 * 
 * Uses the ExecutorService and sends in a basic
 * Callable that sleeps a random amount of ms
 * then adds its ID to a queue to be printed out
 * 
 * Callable and Echoer are lambda functions
 * 
 * Echoer could be replaced by any thread/code
 * that handles results differently
 * 
 * By Nick Venenga
 * Date 17 May 2017
 */
package me.nickv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

public class Demo {
    private static final int taskCount = 500;
    private static final int threadPoolSize = 16;
    
    public static void main(String args[]) throws InterruptedException {
        // Get the starting time
        long start = System.currentTimeMillis();
        
        // Create a queue for communication
        final BlockingQueue<Integer> finishedTasks = new LinkedBlockingQueue<Integer>();
        
        // Create a mutable int object to keep track of echoes
        final MutableInt counter = new MutableInt(0);
        // Create the echoer thread
        Thread echoerThread = new Thread(() -> {
            Integer queueData = new Integer(0);
            try {
                // Try to remove items from the queue (blocking)
                while((queueData = finishedTasks.take()) != null) {
                    // Create a left padded string
                    String paddedTask;
                    paddedTask = StringUtils.leftPad("" + queueData, 3);
                    
                    // Print it out
                    System.out.print(" " + paddedTask + " ");
                    
                    // Increment print counter
                    counter.increment();
                    
                    // Add a new line every 10th item
                    if(counter.getValue() % 10 == 0) {
                        System.out.print("\n");
                    }
                }
            }
            // Echoer has been interrupted (stopped)
            catch (InterruptedException e) {
                 System.out.println("Echoer interrupted!");
                 System.out.println("Echoer counted " + counter.getValue() + " items");
                 return;
            }
        });
        
        // Start the echoer thread
        echoerThread.start();        
        
        // Create a pool of workers
        // Creates up to n threads (startup and shutdown of threads happen dynamically)
        //ExecutorService executor = Executors.newWorkStealingPool(16);
        // Creates exactly n threads and queues other work
        ExecutorService executor = Executors.newFixedThreadPool(Demo.threadPoolSize);

        List<Callable<Void>> callables = new ArrayList<Callable<Void>>();
        
        // Create taskCount number of tasks
        for(int i = 0; i < Demo.taskCount; i++) {
            // taskId needs to be a final
            final int taskId = i;
            
            // Add a lambda function that randomly sleeps than adds its id to the completed queue
            callables.add(() -> {
                // Sleep for a random amount of time
                long millis = (long) (Math.random()*1000);
                Thread.sleep(millis);
                
                // Add id to the complete queue
                finishedTasks.add(taskId);
                return null;
            });
        }

        // Sends the callable tasks to the executor for execution
        executor.invokeAll(callables);
        /*    .stream()
            .map(future -> {
                try {
                    return future.get();
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            })
            .forEach(System.out::println);
        */
        
        System.out.println();
        
        System.out.println("Shutting down executor");
        executor.shutdown();
        System.out.println("Awaiting executor termination");
        // Wait up to taskCount seconds for the tasks to complete
        executor.awaitTermination(Demo.taskCount, TimeUnit.SECONDS);
        
        // Interrupt the echoer (to stop it) -- try/catch triggered to break out of while loop
        System.out.println("Stopping echoer");
        echoerThread.interrupt();
        
        System.out.println("Waiting for echoer thread");
        echoerThread.join();
        
        // Calculate the total runtime
        System.out.println("\nDone in " + (1.0*System.currentTimeMillis() - start)/1000);
        
        while(!finishedTasks.isEmpty()) {
            System.out.println("Found " + finishedTasks.take() + " still in queue");
        }
    }
}
