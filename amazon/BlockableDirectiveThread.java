package com.goertek.smartear.amazon;

import android.util.Log;

import java.util.concurrent.BlockingQueue;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class BlockableDirectiveThread extends Thread {
    private static final boolean D  = false;
    private static final String TAG = BlockableDirectiveThread.class.getSimpleName();
    private final BlockingQueue<Directive> directiveQueue;
    private final DirectiveDispatcher directiveDispatcher;
    private volatile boolean block;
    private volatile boolean is_looping = true;

    public BlockableDirectiveThread(BlockingQueue<Directive> directiveQueue,
                                    DirectiveDispatcher directiveDispatcher) {
        this(directiveQueue, directiveDispatcher, BlockableDirectiveThread.class.getSimpleName());
    }

    public BlockableDirectiveThread(BlockingQueue<Directive> directiveQueue,
                                    DirectiveDispatcher directiveDispatcher, String name) {
        this.directiveQueue = directiveQueue;
        this.directiveDispatcher = directiveDispatcher;
        setName(name);
    }

    public synchronized void block() {
        block = true;
    }

    public synchronized void unblock() {
        block = false;
        notify();
    }

    public synchronized void clear() {
        directiveQueue.clear();
    }

    public void realease() {
        clear();
        is_looping = false;
    }

    @Override
    public void run() {
        while (is_looping) {
            try {
                if(D) Log.d(TAG,"BlockableDirectiveThread run...");

               /* synchronized (this) {
                    if (block) {
                        Thread.sleep(3000);
                    }
                }*/

                Directive directive = directiveQueue.take();
                if(D) Log.d(TAG,"take one directive...");
                directiveDispatcher.dispatch(directive);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
