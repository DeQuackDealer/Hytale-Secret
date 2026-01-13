package com.yellowtale.jurassictale.ai.steering;

public record SteeringOutput(Vec2 linear, double angular) {
    
    public static final SteeringOutput ZERO = new SteeringOutput(Vec2.ZERO, 0);
    
    public static SteeringOutput linear(Vec2 force) {
        return new SteeringOutput(force, 0);
    }
    
    public static SteeringOutput angular(double rotation) {
        return new SteeringOutput(Vec2.ZERO, rotation);
    }
    
    public SteeringOutput add(SteeringOutput other) {
        return new SteeringOutput(
            linear.add(other.linear),
            angular + other.angular
        );
    }
    
    public SteeringOutput multiply(double scalar) {
        return new SteeringOutput(
            linear.multiply(scalar),
            angular * scalar
        );
    }
    
    public boolean isZero() {
        return linear.isZero() && angular == 0;
    }
}
