package de.codesourcery.robosim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import de.codesourcery.robosim.motor.Motor;

public class Main extends JFrame
{
    private class MotorPanel extends JPanel
    {
        private Motion currentMotion;
        private int mouseX, mouseY;

        private final Chart angleChart = new Chart( Color.YELLOW, Color.WHITE,250)
            .yScaling( 15 ).sampleEvery( 50 );
        {
            setFocusable( true );
            requestFocus();
            final MouseAdapter mouseListener = new MouseAdapter()
            {
                @Override
                public void mouseMoved(MouseEvent e)
                {
                    mouseX = e.getX();
                    mouseY = e.getY();
                    repaint();
                }

                private double printAngle(MouseEvent e) {
                    final int cx = getWidth() / 2;
                    final int cy = getHeight() / 2;
                    final double y = e.getY() - cy;
                    final double x = e.getX() - cx;
                    double angleInRad = Math.atan2( y, x );
                    final double angleInDeg = radToDeg( angleInRad + Math.PI );
                    System.out.println( "angle: " + angleInDeg );
                    return angleInRad;
                }

                @Override
                public void mouseClicked(MouseEvent e)
                {
                    mouseX = e.getX();
                    mouseY = e.getY();

                    if ( e.getButton() == MouseEvent.BUTTON1 )
                    {
                        final double desiredAngle = printAngle( e );
                        startMotion( desiredAngle );
                        repaint();
                    }
                }
            };
            addMouseListener( mouseListener );
            addMouseMotionListener( mouseListener );
        }

        public void startMotion(double angleInRad) {
//            if ( ! motor.isMoving() )
//            {
                motor.setDesiredAngle( angleInRad );
                currentMotion = new Motion();
//            } else {
//                System.out.println("*** Motor is still moving... ***");
//            }
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            g.setColor( Color.BLACK );
            g.fillRect(  0, 0, getWidth(), getHeight() );

            final int cx = getWidth() / 2;
            final int cy = getHeight() / 2;

            int motorX = cx;
            int motorY = cy;

            // render circle around motor
            Color motorColor;
            if ( motor.isBroken() ) {
                motorColor = Color.RED;
            } else if ( motor.isTooHot() || motor.isCurrentlyStalled() ) {
                motorColor = Color.YELLOW;
            } else {
                motorColor = Color.GREEN;
            }
            renderCircle( motorColor, motorX, motorY, 15, g );

            // render circle for output shaft
            renderCircle( Color.BLACK, motorX, motorY, 3, g );

            // render line indicating the motor's desired angle
            final int rDesired = 150;
            int px = (int) Math.round( motorX + rDesired * Math.cos( motor.getDesiredAngle()) );
            int py = (int) Math.round( motorY + rDesired * Math.sin( motor.getDesiredAngle() ) );
            g.setColor( Color.GREEN );
            g.drawLine( motorX, motorY, px, py );

            // render line indicating the motor's current angle
            final int rCurrent = 75;
            px = (int) Math.round( motorX + rCurrent * Math.cos( motor.currentAngle ) );
            py = (int) Math.round( motorY + rCurrent * Math.sin( motor.currentAngle ) );
            g.setColor( Color.ORANGE);
            g.drawLine( motorX, motorY, px, py );

            g.setColor( Color.WHITE );
            g.drawLine( motorX, motorY, mouseX, mouseY );

            // render temperature
            final int dx = (int) Math.sqrt( 20*20 + 20*20 );
            g.setColor( Color.WHITE );
            g.drawString( "%2.2f °C".formatted(motor.currentTemperature), motorX+dx, motorY-dx);

            angleChart.render( 10,30,400,230 , (Graphics2D) g);

            Toolkit.getDefaultToolkit().sync();
        }


        private void renderCircle(Color color,int x, int y, int radius, Graphics g) {
            int cx = x - radius;
            int cy = y - radius;
            g.setColor(color);
            g.drawArc( cx,cy,2*radius, 2*radius, 0,360 );
        }
    }

    private final Motor motor = new Motor("base");

    private static final class Motion {
        public final long motionStart=System.nanoTime();
        public long motionStop;
        public boolean stopped;
        public void stop() {
            if ( ! stopped ) {
                stopped = true;
                motionStop = System.nanoTime();
            }
        }
        public double elapsedSeconds() {
            double elapsedMillis;
            if ( stopped ) {
                elapsedMillis= (motionStop - motionStart) / 1000000.0;
            } else {
                elapsedMillis = (System.nanoTime() - motionStart) / 1000000.0;
            }
            return elapsedMillis/1000.0;
        }
    }

    private final MotorPanel motorPanel = new MotorPanel();

    public Main() {
        super("RoboSim");

        getContentPane().add(motorPanel);
        setDefaultCloseOperation( EXIT_ON_CLOSE );
        setPreferredSize( new Dimension( 640, 480 ) );
        setLocationRelativeTo( null );
        pack();
        setVisible(true);
    }

    private void run()
    {
        motorPanel.startMotion( degToRad(90) );

        // FIXME: Remove debug code
        motor.breakOnOverTemperature = false;

        final Timer t = new Timer(16, _ -> {
            int ticks = 10;
            do
            {
                motor.tick( 0.3f );
                if ( motorPanel.currentMotion != null && !motorPanel.currentMotion.stopped && !motor.isMoving() )
                {
                    motorPanel.currentMotion.stop();
                    System.out.println( "Motor stopped after " + motorPanel.currentMotion.elapsedSeconds() + " " +
                                        "seconds" );
                }
                motorPanel.angleChart.update( motor.elapsedSeconds, motor.currentAngle );
            } while (ticks-- > 0);
            motorPanel.repaint();
        });
        t.start();
    }

    static void main() throws InterruptedException, InvocationTargetException
    {
        SwingUtilities.invokeAndWait( () -> new Main().run() );
    }

    private static double degToRad(double deg)
    {
        return (deg*Math.PI)/180;
    }

    private static double radToDeg(double rad)
    {
        return rad * 180.0 / Math.PI;
    }
}