using System;

namespace Rubidium.Api.Anticheat
{
    public class CombatSnapshot
    {
        public bool IsAttack { get; }
        public Guid? TargetId { get; }
        public double? DistanceToTarget { get; }
        public double? AngleToTarget { get; }
        public double DamageDealt { get; }
        public string? WeaponType { get; }
        public bool WasCritical { get; }
        public long Timestamp { get; }
        
        private CombatSnapshot(Builder builder)
        {
            IsAttack = builder._isAttack;
            TargetId = builder._targetId;
            DistanceToTarget = builder._distanceToTarget;
            AngleToTarget = builder._angleToTarget;
            DamageDealt = builder._damageDealt;
            WeaponType = builder._weaponType;
            WasCritical = builder._wasCritical;
            Timestamp = builder._timestamp;
        }
        
        public static Builder Attack(Guid targetId, double distance, double angle) =>
            new Builder(true).WithTarget(targetId, distance, angle);
        
        public static Builder Miss() => new(true);
        
        public class Builder
        {
            internal bool _isAttack;
            internal Guid? _targetId;
            internal double? _distanceToTarget;
            internal double? _angleToTarget;
            internal double _damageDealt = 0.0;
            internal string? _weaponType;
            internal bool _wasCritical = false;
            internal long _timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            
            public Builder(bool isAttack)
            {
                _isAttack = isAttack;
            }
            
            public Builder WithTarget(Guid targetId, double distance, double angle)
            {
                _targetId = targetId;
                _distanceToTarget = distance;
                _angleToTarget = angle;
                return this;
            }
            
            public Builder WithDamage(double damage)
            {
                _damageDealt = damage;
                return this;
            }
            
            public Builder WithWeapon(string weaponType)
            {
                _weaponType = weaponType;
                return this;
            }
            
            public Builder WithCritical(bool wasCritical)
            {
                _wasCritical = wasCritical;
                return this;
            }
            
            public CombatSnapshot Build() => new(this);
        }
    }
}
