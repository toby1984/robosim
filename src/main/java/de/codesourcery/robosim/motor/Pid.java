package de.codesourcery.robosim.motor;

public class Pid {
    // --- PID Constants ---
    private final double Kp; // Proportional gain
    private final double Ki; // Integral gain
    private final double Kd; // Derivative gain

    // --- State Variables ---
    private double integral = 0.0;
    private double lastError = 0.0;

    /**
     * Initializes the PID controller with the specified gains.
     * @param Kp The proportional gain (P).
     * @param Ki The integral gain (I).
     * @param Kd The derivative gain (D).
     */
    public Pid(double Kp, double Ki, double Kd) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
    }

    public Pid() {
        // 4.106 => this(0.708, 0.00, 9.5);
        // 4.094 => this(0.708, 0.00, 9.498);
        // 4.089 => this(0.708, 0.00, 9.496);
        this(0.708, 0.00, 9.496);
    }

    /**
     * Calculates the control output based on the current value, desired value, and time elapsed.
     *
     * The method computes the error, applies the P, I, and D terms using the
     * elapsed time (dt) for proper scaling, and clamps the final output to [-1.0, 1.0].
     *
     * @param currentValue The measured value from the system (e.g., current position).
     * @param desiredValue The target value (setpoint) (e.g., target position).
     * @param dt The time elapsed in seconds since the last invocation of this method.
     * @return The control signal, clamped between -1.0 and 1.0.
     */
    public double step(double currentValue, double desiredValue, double dt) {
        // A small or zero dt can cause division by zero or large spikes in the derivative term.
        if (dt <= 0.0) {
            return 0.0;
        }

        // 1. Calculate the Error
        double error = desiredValue - currentValue;

        // 2. Proportional Term (P)
        double pTerm = Kp * error;

        // 3. Derivative Term (D)
        double derivative = (error - lastError) / dt;
        double dTerm = Kd * derivative;

        // --- Integral Anti-Windup Logic ---

        // Calculate the potential new integral value
        double potentialIntegral = integral + error * dt;
        double potentialITerm = Ki * potentialIntegral;

        // Calculate the total output if we used the potential new integral value (unclamped)
        double potentialOutput = pTerm + potentialITerm + dTerm;

        // Check for Integral Windup:
        // We only allow the integral to update if:
        // 1. The potential output is within the bounds [-1.0, 1.0] (no saturation).
        // 2. OR, if the error is leading the integral *away* from saturation.

        boolean isWindupPrevented = (potentialOutput > 1.0 && error > 0) || // Saturated high and error is positive (pushing further)
                                    (potentialOutput < -1.0 && error < 0);  // Saturated low and error is negative (pushing further)

        if (!isWindupPrevented) {
            // Update the integral only if windup is not occurring
            integral = potentialIntegral;
        }

        // 4. Final Total Output Calculation
        double iTerm = Ki * integral;
        double output = pTerm + iTerm + dTerm;

        // 5. Update State
        lastError = error;

        // 6. Clamp the Final Output to [-1.0, 1.0]
        if (output > 1.0) {
            output = 1.0;
        } else if (output < -1.0) {
            output = -1.0;
        }

        return output;
    }

    // Optional: Reset integral term if the system changes state dramatically
    public void reset() {
        this.integral = 0.0;
        this.lastError = 0.0;
    }

    // Optional: Simple main method for demonstration
    static void main(String[] args) throws InterruptedException
    {
        // Example Usage: Simple PID tuned to move an object's position (currentValue)
        Pid controller = new Pid(0.8, 0.01, 1.5);

        double currentValue = 0.0;
        double desiredValue = .5;
        double velocity = 0.0;

        // Define a fixed time step (dt) for the simulation loop
        final double deltaT = 0.02; // e.g., 50 Hz loop frequency
        double time = 0.0;

        System.out.println("Starting PID Simulation (Target: " + desiredValue + ", DT=" + deltaT + "s)");

        int cnt = 0;
        boolean reachedTarget = false;
        int extraTicks = 1000;
        while ( true ) {
            // 1. Calculate the required acceleration/force (control signal)
            double controlSignal = controller.step(currentValue, desiredValue, deltaT);

            // 2. Apply control signal to the system (simplified physics: F=ma)
            // Control signal acts as acceleration/force
            velocity += controlSignal * deltaT;
            currentValue += velocity * deltaT;
            time += deltaT;

            if ( (cnt++ % 100) == 0 )
            {
                System.out.printf( "Time=%.2fs | Pos=%.4f | Vel=%.4f | Control Signal=%.4f\n", time, currentValue, velocity, controlSignal );
            }

            // Check for stability and target proximity
            if ( ! reachedTarget )
            {
                if ( Math.abs( currentValue - desiredValue ) < 0.01 && Math.abs( velocity ) < 0.01 )
                {
                    reachedTarget = true;
                    System.out.println( "*** Target reached and stable." );
                }
            }
            else if ( extraTicks-- == 0 )
            {
                break;
            }
        }
    }
}