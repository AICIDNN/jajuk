package org.jajuk;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.jajuk.util.log.Log;

/**
 * Helper class to test with many threads.
 * 
 * Sample usage is as follows:

    public void testMultipleThreads() throws Exception
    {
        ThreadTestHelper helper = new ThreadTestHelper(NUMBER_OF_THREADS, NUMBER_OF_TESTS);

        helper.executeTest(new ThreadTestHelper.TestRunnable()
        {
            public void doEnd(int threadnum) throws Exception
            {
              // do stuff at the end
              ...
            }

            public void run(int threadnum, int iter)
            {
              // do the actual threaded work
              ...
            }
        });
    }
 */
public class ThreadTestHelper
{
    private final int threadCount;
    private final int testsPerThread;

    private boolean failed = false;
    private int executions[] = null;

    /**
     * Initialize the class with the number of tests that should be executed
     * 
     * @param threadCount The number of threads to start running in parallel.
     * @param testsPerThread The number of single test-executions that are done in each thread
     */
    public ThreadTestHelper(int threadCount, int testsPerThread)
    {
        this.threadCount = threadCount;
        this.testsPerThread = testsPerThread;

        // Initialize array to allow to summarize afterwards
        executions = new int[threadCount];
    }

    public void executeTest(TestRunnable run) throws Exception
    {
        Log.debug("Starting thread test");

        List<Thread> threads = new LinkedList<Thread>();

        // start all threads
        for (int i = 0; i < threadCount; i++)
        {
            Thread t = startThread(i, run);
            threads.add(t);
        }

        // wait for all threads
        for (int i = 0; i < threadCount; i++)
        {
            threads.get(i).join();
        }

        // make sure the resulting number of executions is correct 
        for (int i = 0; i < threadCount; i++)
        {
            // check if enough items were performed
            TestCase.assertEquals("Thread " + i + " did not execute all iterations", testsPerThread, executions[i]);
        }

        // check that we did not fail in any thread, i.e. no exception occurred...
        TestCase.assertFalse(failed);
    }

    /**
     * This method is executed to start one thread. The thread will execute the 
     * provided runnable a number of times.  
     * 
     * @param threadnum The number of this thread
     * @param run The Runnable object that is used to perform the actual test operation
     * 
     * @return The thread that was started.
     * 
     */
    private Thread startThread(final int threadnum, final TestRunnable run)
    {
        Log.debug("Starting thread number: " + threadnum);

        Thread t1 = null;
        t1 = new Thread(new Runnable()
        {

            public void run()
            {
                try
                {
                    for (int iter = 0; iter < testsPerThread; iter++)
                    {
                        //Log.debug("Executing iteration " + iter + " in thread" + Thread.currentThread().getName());

                        // call the actual testcode
                        run.run(threadnum, iter);

                        executions[threadnum]++;
                    }
                    
                    // do end-work here, we don't do this in a finally as we log Exception then anyway
                    run.doEnd(threadnum);
                }
                catch (Throwable e)
                {
                    Log.error(e);
                    failed = true;
                }
                
                

            }
        }, "Thread " + threadnum);

        t1.start();

        return t1;
    }
    
    public interface TestRunnable
    {
        /**
         * When an object implementing interface <code>Runnable</code> is used 
         * to create a thread, starting the thread causes the object's 
         * <code>run</code> method to be called in that separately executing 
         * thread. 
         * <p>
         * The general contract of the method <code>run</code> is that it may 
         * take any action whatsoever.
         * 
         * @param threadnum The number of the thread executing this run()
         * @param iter The count of how many times this thread executed the method
         *
         * @see     java.lang.Thread#run()
         */
        public abstract void run(int threadnum, int iter) throws Exception;

        /**
         * Perform any action that should be done at the end.
         * 
         *  This method should throw an Exception if any check fails at this point.
         */
        void doEnd(int threadnum) throws Exception;
    }

    public void testDummy() {
      // small empty test to not fail if this class is executed as test case by Hudson/Sonar
    }
}
