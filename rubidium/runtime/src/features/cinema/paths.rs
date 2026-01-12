use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum InterpolationType {
    Linear,
    Smooth,
    Bezier,
    Catmull,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PathKeyframe {
    pub time_ms: u64,
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub yaw: f32,
    pub pitch: f32,
    pub roll: f32,
    pub fov: f32,
    pub focus_entity: Option<Uuid>,
    pub interpolation: InterpolationType,
}

impl PathKeyframe {
    pub fn new(time_ms: u64, x: f64, y: f64, z: f64, yaw: f32, pitch: f32) -> Self {
        Self {
            time_ms,
            x, y, z,
            yaw, pitch,
            roll: 0.0,
            fov: 70.0,
            focus_entity: None,
            interpolation: InterpolationType::Smooth,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CameraPath {
    pub id: Uuid,
    pub name: String,
    pub owner_id: Uuid,
    pub keyframes: Vec<PathKeyframe>,
    pub duration_ms: u64,
    pub loop_enabled: bool,
    pub time_scale: f32,
}

impl CameraPath {
    pub fn new(owner_id: Uuid, name: String) -> Self {
        Self {
            id: Uuid::new_v4(),
            name,
            owner_id,
            keyframes: Vec::new(),
            duration_ms: 0,
            loop_enabled: false,
            time_scale: 1.0,
        }
    }

    pub fn add_keyframe(&mut self, keyframe: PathKeyframe) {
        self.keyframes.push(keyframe);
        self.keyframes.sort_by_key(|k| k.time_ms);
        self.recalculate_duration();
    }

    pub fn remove_keyframe(&mut self, index: usize) -> Option<PathKeyframe> {
        if index < self.keyframes.len() {
            let kf = self.keyframes.remove(index);
            self.recalculate_duration();
            Some(kf)
        } else {
            None
        }
    }

    fn recalculate_duration(&mut self) {
        self.duration_ms = self.keyframes.last()
            .map(|k| k.time_ms)
            .unwrap_or(0);
    }

    pub fn get_position_at(&self, time_ms: u64) -> Option<PathKeyframe> {
        if self.keyframes.is_empty() {
            return None;
        }

        if self.keyframes.len() == 1 {
            return Some(self.keyframes[0].clone());
        }

        let time = if self.loop_enabled && self.duration_ms > 0 {
            time_ms % self.duration_ms
        } else {
            time_ms.min(self.duration_ms)
        };

        let before = self.keyframes.iter()
            .rev()
            .find(|k| k.time_ms <= time);
        let after = self.keyframes.iter()
            .find(|k| k.time_ms > time);

        match (before, after) {
            (Some(b), Some(a)) => {
                let t = (time - b.time_ms) as f64 / (a.time_ms - b.time_ms) as f64;
                Some(Self::interpolate(b, a, t))
            }
            (Some(b), None) => Some(b.clone()),
            (None, Some(a)) => Some(a.clone()),
            (None, None) => None,
        }
    }

    fn interpolate(from: &PathKeyframe, to: &PathKeyframe, t: f64) -> PathKeyframe {
        let t = match from.interpolation {
            InterpolationType::Linear => t,
            InterpolationType::Smooth => t * t * (3.0 - 2.0 * t),
            InterpolationType::Bezier => t * t * t * (t * (t * 6.0 - 15.0) + 10.0),
            InterpolationType::Catmull => t * t * (3.0 - 2.0 * t),
        };

        PathKeyframe {
            time_ms: from.time_ms + ((to.time_ms - from.time_ms) as f64 * t) as u64,
            x: from.x + (to.x - from.x) * t,
            y: from.y + (to.y - from.y) * t,
            z: from.z + (to.z - from.z) * t,
            yaw: from.yaw + (to.yaw - from.yaw) * t as f32,
            pitch: from.pitch + (to.pitch - from.pitch) * t as f32,
            roll: from.roll + (to.roll - from.roll) * t as f32,
            fov: from.fov + (to.fov - from.fov) * t as f32,
            focus_entity: from.focus_entity,
            interpolation: from.interpolation,
        }
    }
}
