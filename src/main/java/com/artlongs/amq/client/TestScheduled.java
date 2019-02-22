package com.artlongs.amq.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Func :
 *
 * @author: leeton on 2019/2/20.
 */
public class TestScheduled {

    public static void main(String[] args) {
        final ScheduledExecutorService scheduler = Executors
                .newScheduledThreadPool(2);
        final Runnable beeper = new Runnable() {
            public void run() {
                System.out.println("The scheduler task is running!");
            }
        };

        //1秒后执行，每1秒执行一次
        final ScheduledFuture<?> beeperHandler = scheduler.scheduleAtFixedRate(
                beeper, 1, 1, SECONDS);
        //2秒后执行，执行完成后间隔5秒在执行下一次
        final ScheduledFuture<?> beeperHandler2 = scheduler.scheduleWithFixedDelay(
                beeper, 2, 5, SECONDS);

    }

}
