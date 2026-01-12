use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum CameraMode {
    FirstPerson,
    ThirdPerson,
    ThirdPersonFront,
    FreeCam,
    Orbit,
    Path,
    Spectate,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CameraPosition {
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub yaw: f32,
    pub pitch: f32,
    pub roll: f32,
    pub fov: f32,
}

impl Default for CameraPosition {
    fn default() -> Self {
        Self {
            x: 0.0,
            y: 64.0,
            z: 0.0,
            yaw: 0.0,
            pitch: 0.0,
            roll: 0.0,
            fov: 70.0,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CameraKeyframe {
    pub tick: u64,
    pub position: CameraPosition,
    pub interpolation: Interpolation,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Interpolation {
    Linear,
    Smooth,
    Ease,
    Step,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CameraSpline {
    pub keyframes: Vec<CameraKeyframe>,
    pub loop_mode: bool,
}

impl CameraSpline {
    pub fn new() -> Self {
        Self {
            keyframes: Vec::new(),
            loop_mode: false,
        }
    }

    pub fn add_keyframe(&mut self, keyframe: CameraKeyframe) {
        let insert_pos = self.keyframes.iter()
            .position(|k| k.tick > keyframe.tick)
            .unwrap_or(self.keyframes.len());
        self.keyframes.insert(insert_pos, keyframe);
    }

    pub fn remove_keyframe(&mut self, index: usize) -> Option<CameraKeyframe> {
        if index < self.keyframes.len() {
            Some(self.keyframes.remove(index))
        } else {
            None
        }
    }

    pub fn get_position_at(&self, tick: u64) -> Option<CameraPosition> {
        if self.keyframes.is_empty() {
            return None;
        }

        if self.keyframes.len() == 1 {
            return Some(self.keyframes[0].position.clone());
        }

        let before = self.keyframes.iter().rev()
            .find(|k| k.tick <= tick);
        let after = self.keyframes.iter()
            .find(|k| k.tick > tick);

        match (before, after) {
            (Some(b), Some(a)) => {
                let t = (tick - b.tick) as f64 / (a.tick - b.tick) as f64;
                Some(Self::interpolate(&b.position, &a.position, t, b.interpolation))
            }
            (Some(b), None) => Some(b.position.clone()),
            (None, Some(a)) => Some(a.position.clone()),
            (None, None) => None,
        }
    }

    fn interpolate(from: &CameraPosition, to: &CameraPosition, t: f64, method: Interpolation) -> CameraPosition {
        let t = match method {
            Interpolation::Linear => t,
            Interpolation::Smooth => t * t * (3.0 - 2.0 * t),
            Interpolation::Ease => t * t,
            Interpolation::Step => if t < 0.5 { 0.0 } else { 1.0 },
        };

        CameraPosition {
            x: from.x + (to.x - from.x) * t,
            y: from.y + (to.y - from.y) * t,
            z: from.z + (to.z - from.z) * t,
            yaw: from.yaw + (to.yaw - from.yaw) * t as f32,
            pitch: from.pitch + (to.pitch - from.pitch) * t as f32,
            roll: from.roll + (to.roll - from.roll) * t as f32,
            fov: from.fov + (to.fov - from.fov) * t as f32,
        }
    }
}

impl Default for CameraSpline {
    fn default() -> Self {
        Self::new()
    }
}

pub struct ReplayCamera {
    mode: CameraMode,
    position: CameraPosition,
    target_entity: Option<Uuid>,
    orbit_distance: f64,
    orbit_yaw: f32,
    orbit_pitch: f32,
    spline: Option<CameraSpline>,
    smooth_factor: f64,
}

impl ReplayCamera {
    pub fn new() -> Self {
        Self {
            mode: CameraMode::ThirdPerson,
            position: CameraPosition::default(),
            target_entity: None,
            orbit_distance: 5.0,
            orbit_yaw: 0.0,
            orbit_pitch: 30.0,
            spline: None,
            smooth_factor: 0.1,
        }
    }

    pub fn mode(&self) -> CameraMode {
        self.mode
    }

    pub fn set_mode(&mut self, mode: CameraMode) {
        self.mode = mode;
    }

    pub fn position(&self) -> &CameraPosition {
        &self.position
    }

    pub fn follow_entity(&mut self, entity_id: Uuid) {
        self.target_entity = Some(entity_id);
    }

    pub fn stop_following(&mut self) {
        self.target_entity = None;
    }

    pub fn target_entity(&self) -> Option<Uuid> {
        self.target_entity
    }

    pub fn set_free_position(&mut self, x: f64, y: f64, z: f64, yaw: f32, pitch: f32) {
        self.mode = CameraMode::FreeCam;
        self.position.x = x;
        self.position.y = y;
        self.position.z = z;
        self.position.yaw = yaw;
        self.position.pitch = pitch;
    }

    pub fn set_orbit(&mut self, distance: f64, yaw: f32, pitch: f32) {
        self.mode = CameraMode::Orbit;
        self.orbit_distance = distance;
        self.orbit_yaw = yaw;
        self.orbit_pitch = pitch;
    }

    pub fn orbit_around(&self, target_x: f64, target_y: f64, target_z: f64) -> CameraPosition {
        let yaw_rad = (self.orbit_yaw as f64).to_radians();
        let pitch_rad = (self.orbit_pitch as f64).to_radians();
        
        let x = target_x - self.orbit_distance * yaw_rad.sin() * pitch_rad.cos();
        let y = target_y + self.orbit_distance * pitch_rad.sin();
        let z = target_z + self.orbit_distance * yaw_rad.cos() * pitch_rad.cos();

        CameraPosition {
            x, y, z,
            yaw: self.orbit_yaw + 180.0,
            pitch: -self.orbit_pitch,
            roll: 0.0,
            fov: self.position.fov,
        }
    }

    pub fn set_path(&mut self, spline: CameraSpline) {
        self.mode = CameraMode::Path;
        self.spline = Some(spline);
    }

    pub fn get_path_position(&self, tick: u64) -> Option<CameraPosition> {
        self.spline.as_ref()?.get_position_at(tick)
    }

    pub fn update(&mut self, target_x: f64, target_y: f64, target_z: f64, target_yaw: f32, target_pitch: f32) {
        match self.mode {
            CameraMode::FirstPerson => {
                self.position.x = target_x;
                self.position.y = target_y + 1.62;
                self.position.z = target_z;
                self.position.yaw = target_yaw;
                self.position.pitch = target_pitch;
            }
            CameraMode::ThirdPerson => {
                let distance = 4.0;
                let yaw_rad = (target_yaw as f64 + 180.0).to_radians();
                let pitch_rad = (target_pitch as f64).to_radians();
                
                self.position.x = target_x - distance * yaw_rad.sin() * pitch_rad.cos();
                self.position.y = target_y + 1.62 + distance * pitch_rad.sin();
                self.position.z = target_z + distance * yaw_rad.cos() * pitch_rad.cos();
                self.position.yaw = target_yaw;
                self.position.pitch = target_pitch;
            }
            CameraMode::ThirdPersonFront => {
                let distance = 4.0;
                let yaw_rad = (target_yaw as f64).to_radians();
                let pitch_rad = (-target_pitch as f64).to_radians();
                
                self.position.x = target_x - distance * yaw_rad.sin() * pitch_rad.cos();
                self.position.y = target_y + 1.62 + distance * pitch_rad.sin();
                self.position.z = target_z + distance * yaw_rad.cos() * pitch_rad.cos();
                self.position.yaw = target_yaw + 180.0;
                self.position.pitch = -target_pitch;
            }
            CameraMode::Orbit => {
                let pos = self.orbit_around(target_x, target_y + 1.0, target_z);
                self.position = pos;
            }
            CameraMode::FreeCam | CameraMode::Path | CameraMode::Spectate => {}
        }
    }

    pub fn set_fov(&mut self, fov: f32) {
        self.position.fov = fov.clamp(30.0, 120.0);
    }

    pub fn set_smooth_factor(&mut self, factor: f64) {
        self.smooth_factor = factor.clamp(0.0, 1.0);
    }
}

impl Default for ReplayCamera {
    fn default() -> Self {
        Self::new()
    }
}
