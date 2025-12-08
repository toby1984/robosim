package de.codesourcery.robosim.motor;

import de.codesourcery.robosim.Utils;

public class Motor
{
    private final Pid pid = new Pid();

    public enum MotorState {
        BROKEN_MAX_TORQUE_EXCEEDED,
        BROKEN_MAX_TEMPERATURE_EXCEEDED,
        OPERATIONAL;

        public boolean isBroken() {
            return this == BROKEN_MAX_TEMPERATURE_EXCEEDED || this ==  BROKEN_MAX_TORQUE_EXCEEDED;
        }
    }

    /* -----------------------------------------
     * Motor physical properties (from datasheet etc.)
     * -----------------------------------------
     */
    /** motor stall torque in kg*cm */
    public double stallTorque=30;

    /** motor rated torque in kg*cm */
    public double ratedTorque=10;

    /** motor weight in kg */
    public double weight;

    /** motor gear ratio denominator */
    public int gearRatioDenominator = 345; // for example "345" if gear ratio is 1:345

    public double maxRPM = 15540;

    /** Maximum motor temperature above which the motor will be considered to be
     * terminally broken */
    public double maxTemperature = 80;

    /** How many seconds the motor may be above the maxTemperature before its considered to be broken */
    public double maxOvertemperatureTime = 10;

    /* ------------------------------
     * Constant simulation properties
     * ------------------------------
     */

    public boolean breakOnOverTemperature = true;

    public boolean breakOnOverload = true;

    public String name;

    /** Used to simulate friction of gearbox etc*/
    public double frictionFactor = 0.98;

    /** How quickly the system will respond to a change in motor torque */
    public double systemInertia = 1.1;

    /** Thermal mass of system in Joule per degrees per Celsius */
    public double thermalMass = 500000;

    /** ambient temperature in degrees */
    public double ambientTemperature = 20;

    /* ------------------------------
     * Transient simulation properties
     * ------------------------------
     */
    /** Current angle of gearbox input shaft in radians */
    public double currentAngle;

    /** Desired angle of gearbox input shaft in radians */
    private double desiredAngle;

    /** the motor's current angular velocity in rad/s */
    public double currentAngularVelocity;

    public double elapsedSeconds;

    /** External torque (if any) in kg*cm ,
     * currently applied to the output shaft of the motor's gearbox, relative
     * to the output shaft's current turning direction.
     * If the output shaft is turning in a positive direction (=rotation angle increasing each time step)
     * the torque value will have a negative sign if it is opposing the turning direction (=counter force)
     */
    public double externalTorque;

    /** Current motor state. */
    public MotorState motorState = MotorState.OPERATIONAL;

    /** How long this motor has been stalling (in seconds) already*/
    public double stallTime;

    /** How many seconds total this motor has been stalling since reset() */
    public double totalStallTime;

    /** Current motor temperature in degrees celsius */
    public double currentTemperature;

    /** Current time in seconds since the motor has been above maxTemperature */
    public double overtemperatureTime;

    /** Total over-temperature time in seconds since reset() */
    public double totalOvertemperatureTime;

    public Motor(String name)
    {
        this.name = name;
        reset();
    }

    public boolean isBroken() {
        return motorState.isBroken();
    }

    private void markBroken(MotorState state) {
        if ( ! isBroken() ) {
            System.out.println( "*** Broken: " + state );
            this.motorState = state;
        }
    }

    @SuppressWarnings("unused")
    public boolean isCurrentlyStalled() {
        return stallTime > 0;
    }

    @SuppressWarnings("unused")
    public boolean isTooHot() {
        return currentTemperature > maxTemperature;
    }

    public boolean isMoving() {
        return Math.abs( this.currentAngularVelocity ) > 0.0001;
    }

    public void tick(double elapsedSeconds) {
        tick( elapsedSeconds, 0, 0, false );
    }

    /**
     *
     * @param elapsedSeconds
     * @param minAngleInclusive min. angle in rad
     * @param maxAngleInclusive max. angle in rad
     * @param clampToMinMaxAngles
     */
    public void tick(double elapsedSeconds, double minAngleInclusive, double maxAngleInclusive, boolean clampToMinMaxAngles)
    {
        this.elapsedSeconds += elapsedSeconds;

        if ( breakOnOverload && Math.abs( externalTorque ) > stallTorque ) {
            markBroken(MotorState.BROKEN_MAX_TORQUE_EXCEEDED);
        }

        final double torqueFactor;
        if ( isBroken() ) {
            torqueFactor = 0;
        } else {
            torqueFactor = pid.step( currentAngle, desiredAngle, elapsedSeconds );
        }

        final double maxTorque = Math.max( ratedTorque, Math.min( Math.abs( externalTorque ) , stallTorque ) );
        // final double maxTorque = ratedTorque;
        // speedSetting will be in range [-1,1]
        final double pidTorque = torqueFactor * maxTorque;

        final double generatedHeat = (pidTorque * pidTorque) / Math.max(0.000000001, elapsedSeconds);
        final double dissipatedHeat = (currentTemperature - ambientTemperature) * 500 * Math.max(0.000000001, elapsedSeconds);

        final double deltaHeat = generatedHeat - dissipatedHeat;
        final double deltaTemperature = deltaHeat / thermalMass;
        this.currentTemperature += deltaTemperature;
        if ( currentTemperature > maxTemperature) {
            this.overtemperatureTime += elapsedSeconds;
            this.totalOvertemperatureTime += elapsedSeconds;
            if ( breakOnOverTemperature && overtemperatureTime > maxOvertemperatureTime )
            {
                markBroken( MotorState.BROKEN_MAX_TEMPERATURE_EXCEEDED );
            }
        } else {
            this.overtemperatureTime = 0;
        }

        if ( isBroken() ) {
            return;
        }

        final double motorTorque = pidTorque * this.frictionFactor;
        final double netTorque = motorTorque - this.externalTorque;

        if ( Math.abs( netTorque ) > stallTorque ) {
            markBroken(MotorState.BROKEN_MAX_TORQUE_EXCEEDED);
        }

        final double angularAcceleration = netTorque / this.systemInertia;
        final double speedDelta = angularAcceleration*elapsedSeconds;
        double newAngularVelocity = this.currentAngularVelocity + speedDelta;
        double rpm = Math.min( maxRPM, (newAngularVelocity/(2*Math.PI*60)));
        newAngularVelocity = rpm*2*Math.PI*60;
        double newAngle = currentAngle + (newAngularVelocity * elapsedSeconds) / gearRatioDenominator;

        final boolean stalled =
            clampToMinMaxAngles && (newAngle < minAngleInclusive || newAngle > maxAngleInclusive);

        if ( stalled  ) {
            this.stallTime += elapsedSeconds;
            this.totalStallTime += elapsedSeconds;
            this.currentAngularVelocity = 0;
            this.currentAngle = Utils.clamp(newAngle, minAngleInclusive, maxAngleInclusive);
        }
        else
        {
            this.stallTime = 0;
            this.currentAngularVelocity = newAngularVelocity;
            this.currentAngle = newAngle;
        }

//        if ( isMoving() ) {
//            System.out.println("Current angle: "+currentAngle+" rad");
//            System.out.println( "Current speed: " + currentAngularVelocity + " rad/s" );
//        }
    }

    public void reset()
    {
        this.pid.reset();

        this.externalTorque = 0;
        this.motorState = MotorState.OPERATIONAL;
        this.currentAngle = 0;
        this.currentAngularVelocity = 0;
        this.currentTemperature = ambientTemperature;
        this.elapsedSeconds = 0;

        this.stallTime = 0;
        this.totalStallTime = 0;

        this.overtemperatureTime = 0;
        this.totalOvertemperatureTime = 0;
    }

    public double getDesiredAngle()
    {
        return desiredAngle;
    }

    public void setDesiredAngle(double desiredAngle)
    {
        while( desiredAngle < 0 ) {
            desiredAngle += 2*Math.PI;
        }
        while( desiredAngle > 2*Math.PI ) {
            desiredAngle -= 2*Math.PI;
        }
        this.desiredAngle = desiredAngle;
    }
}