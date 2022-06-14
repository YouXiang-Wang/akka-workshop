package tech.parasol.thread;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

public class ForkJoinPoolTest {

    public static void main(String[] args) throws Exception{
        testHasResultTask();
    }

    public static void testHasResultTask() throws Exception {
        int result1 = 0;
        for (int i = 1; i <= 1000000; i++) {
            result1 += i;
        }
        System.out.println("Computing 1-1000000 accumulation value: " + result1);

        ForkJoinPool pool = new ForkJoinPool();
        ForkJoinTask<Integer> task = pool.submit(new CalculateTask(1, 1000000));
        int result2 = task.get();
        System.out.println("Parallel computing 1-1000000 accumulation value: " + result2);
        pool.awaitTermination(2, TimeUnit.SECONDS);
        pool.shutdown();
    }
}
