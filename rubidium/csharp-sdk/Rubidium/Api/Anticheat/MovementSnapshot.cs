using System;

namespace Rubidium.Api.Anticheat
{
    public class MovementSnapshot
    {
        public double X { get; }
        public double Y { get; }
        public double Z { get; }
        public float Yaw { get; }
        public float Pitch { get; }
        public bool OnGround { get; }
        public bool InWater { get; }
        public bool IsGliding { get; }
        public bool IsTeleporting { get; }
        public bool TookFallDamage { get; }
        public long Timestamp { get; }
        public string World { get; }
        
        private MovementSnapshot(Builder builder)
        {
            X = builder._x;
            Y = builder._y;
            Z = builder._z;
            Yaw = builder._yaw;
            Pitch = builder._pitch;
            OnGround = builder._onGround;
            InWater = builder._inWater;
            IsGliding = builder._isGliding;
            IsTeleporting = builder._isTeleporting;
            TookFallDamage = builder._tookFallDamage;
            Timestamp = builder._timestamp;
            World = builder._world;
        }
        
        public double DistanceTo(MovementSnapshot other)
        {
            var dx = X - other.X;
            var dy = Y - other.Y;
            var dz = Z - other.Z;
            return Math.Sqrt(dx * dx + dy * dy + dz * dz);
        }
        
        public static Builder Create(double x, double y, double z) => new(x, y, z);
        
        public class Builder
        {
            internal double _x, _y, _z;
            internal float _yaw, _pitch;
            internal bool _onGround = true;
            internal bool _inWater = false;
            internal bool _isGliding = false;
            internal bool _isTeleporting = false;
            internal bool _tookFallDamage = false;
            internal long _timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            internal string _world = "world";
            
            public Builder(double x, double y, double z)
            {
                _x = x;
                _y = y;
                _z = z;
            }
            
            public Builder WithRotation(float yaw, float pitch)
            {
                _yaw = yaw;
                _pitch = pitch;
                return this;
            }
            
            public Builder WithOnGround(bool onGround)
            {
                _onGround = onGround;
                return this;
            }
            
            public Builder WithInWater(bool inWater)
            {
                _inWater = inWater;
                return this;
            }
            
            public Builder WithGliding(bool isGliding)
            {
                _isGliding = isGliding;
                return this;
            }
            
            public Builder WithTeleporting(bool isTeleporting)
            {
                _isTeleporting = isTeleporting;
                return this;
            }
            
            public Builder WithFallDamage(bool tookFallDamage)
            {
                _tookFallDamage = tookFallDamage;
                return this;
            }
            
            public Builder InWorld(string world)
            {
                _world = world;
                return this;
            }
            
            public MovementSnapshot Build() => new(this);
        }
    }
}
