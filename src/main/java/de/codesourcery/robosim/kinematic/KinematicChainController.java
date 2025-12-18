package de.codesourcery.robosim.kinematic;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import de.codesourcery.robosim.ITickListener;

public class KinematicChainController implements ITickListener
{
    private final KinematicChain chain;

    private final Object motorSimulationFinishedLock = new Object();

    // @GuardedBy( motorSimulationFinishedLock )
    public boolean motorSimulationFinished = false;

    private volatile boolean hasArrivedAtDestinationAngles;

    private boolean shutdownStarted;
    private final CountDownLatch stopped = new CountDownLatch(1);

    private final Object threadLock = new Object();
    // @GuardedBy( threadLock )
    private MyThread worker;

    private final LinkedBlockingQueue<Consumer<KinematicChain>> workQueue = new LinkedBlockingQueue<>(10);

    public KinematicChainController(KinematicChain chain)
    {
        Validate.notNull( chain, "chain must not be null" );
        this.chain = chain;
    }

    public void stop() throws InterruptedException
    {
        boolean waitForShutdown;
        synchronized(  threadLock ) {
            if ( shutdownStarted ) {
                waitForShutdown = false;
            }
            else
            {
                waitForShutdown = worker != null && worker.isAlive();
                shutdownStarted = true;
            }
        }
        if ( waitForShutdown ) {
            while (!stopped.await( 1, TimeUnit.SECONDS ))
            {
                System.out.println( "Waiting for " + worker.getName() + " to stop..." );
            }
        }
    }

    public void start() {
        synchronized(  threadLock ) {
            if ( shutdownStarted ) {
                throw new IllegalStateException( "Shutting down" );
            }
            if ( worker == null || ! worker.isAlive() )
            {
                worker = new MyThread();
                worker.start();
            }
        }
    }

    private class MyThread extends Thread
    {

        private MyThread()
        {
            setName("kinematic-chain-controller");
            setDaemon( true );
        }

        private boolean doOneTick(double elapsedSeconds) {

            boolean arrivedAtDestinationAngles = true;
            Part part = chain.firstJoint;
            while ( part != null )
            {
                if ( part instanceof Joint j) {
                    j.motor.tick( elapsedSeconds );
                    arrivedAtDestinationAngles &= j.motor.hasArrivedAtDestinationAngle();
                }
                part = part.next();
            }
            return arrivedAtDestinationAngles;
        }

        @Override
        public void run()
        {
            try
            {
                while ( ! shutdownStarted )
                {
                    // update motor angles
                    synchronized( motorSimulationFinishedLock )
                    {
                        while (!shutdownStarted && motorSimulationFinished)
                        {
                            try
                            {
                                motorSimulationFinishedLock.wait( 1000 );
                            }
                            catch( Exception e )
                            {
                                // can't help it
                            }
                        }
                    }
                    if ( ! shutdownStarted )
                    {
                        Consumer<KinematicChain> worker;
                        while( (worker = workQueue.poll()) != null )
                        {
                            worker.accept( chain );
                        }

                        boolean arrivedAtDestination = true;
                        for ( int i = 0; i < 1; i++ )
                        {
                            arrivedAtDestination &= doOneTick( 0.3 );
                        }
                        synchronized( motorSimulationFinishedLock )
                        {
                            hasArrivedAtDestinationAngles = arrivedAtDestination;
                            motorSimulationFinished = true;
                        }
                    }
                }
            } finally {
                if ( shutdownStarted ) {
                    stopped.countDown();
                }
            }
        }
    }

    public void enqueue(Consumer<KinematicChain> work) {
        if ( workQueue.size() >= 10 ) {
            throw new IllegalStateException( "Worker queue full" );
        }
        workQueue.add(work);
    }

    public void applyMotorRotationsToBodies() {
        // apply motor rotation angle to body
        Part part = chain.firstJoint;
        while ( part != null )
        {
            if ( part instanceof Joint j) {
                j.updateRotationFromMotor();
            }
            part = part.next();
        }
    }

    /**
     * Called by render loop before a frame gets rendered.
     * @param elapsedSeconds
     */
    @Override
    public void tick(double elapsedSeconds)
    {
        final boolean updateBodies;
        synchronized( motorSimulationFinishedLock )
        {
            updateBodies = motorSimulationFinished;
        }
        if ( updateBodies )
        {
            applyMotorRotationsToBodies();
            synchronized( motorSimulationFinishedLock )
            {
                motorSimulationFinished = false;
                motorSimulationFinishedLock.notifyAll();
            }
        }
    }

    public boolean hasArrivedAtDestinationAngles() {
        return hasArrivedAtDestinationAngles;
    }
}