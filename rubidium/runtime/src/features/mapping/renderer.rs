use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RenderSettings {
    pub biome_colors: BiomeColorScheme,
    pub height_gradient: HeightGradient,
    pub water_color: u32,
    pub shadow_enabled: bool,
    pub shadow_intensity: f32,
}

impl Default for RenderSettings {
    fn default() -> Self {
        Self {
            biome_colors: BiomeColorScheme::default(),
            height_gradient: HeightGradient::default(),
            water_color: 0x3F76E4,
            shadow_enabled: true,
            shadow_intensity: 0.3,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BiomeColorScheme {
    pub plains: u32,
    pub forest: u32,
    pub desert: u32,
    pub mountain: u32,
    pub ocean: u32,
    pub swamp: u32,
    pub taiga: u32,
    pub jungle: u32,
    pub savanna: u32,
    pub badlands: u32,
    pub snow: u32,
}

impl Default for BiomeColorScheme {
    fn default() -> Self {
        Self {
            plains: 0x8DB360,
            forest: 0x4C7F32,
            desert: 0xEDC9AF,
            mountain: 0x8B8B8B,
            ocean: 0x3F76E4,
            swamp: 0x4C6559,
            taiga: 0x2D5A47,
            jungle: 0x1B7A37,
            savanna: 0xB5A84F,
            badlands: 0xD94515,
            snow: 0xFFFFFF,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HeightGradient {
    pub low_color: u32,
    pub mid_color: u32,
    pub high_color: u32,
    pub low_height: i32,
    pub high_height: i32,
}

impl Default for HeightGradient {
    fn default() -> Self {
        Self {
            low_color: 0x1A1A1A,
            mid_color: 0x808080,
            high_color: 0xFFFFFF,
            low_height: 0,
            high_height: 256,
        }
    }
}

pub fn blend_colors(color1: u32, color2: u32, t: f32) -> u32 {
    let r1 = ((color1 >> 16) & 0xFF) as f32;
    let g1 = ((color1 >> 8) & 0xFF) as f32;
    let b1 = (color1 & 0xFF) as f32;
    
    let r2 = ((color2 >> 16) & 0xFF) as f32;
    let g2 = ((color2 >> 8) & 0xFF) as f32;
    let b2 = (color2 & 0xFF) as f32;
    
    let r = (r1 + (r2 - r1) * t) as u32;
    let g = (g1 + (g2 - g1) * t) as u32;
    let b = (b1 + (b2 - b1) * t) as u32;
    
    (r << 16) | (g << 8) | b
}

pub fn apply_shadow(color: u32, height_diff: i32, intensity: f32) -> u32 {
    if height_diff >= 0 {
        return color;
    }
    
    let factor = 1.0 - (intensity * (-height_diff as f32 / 4.0).min(1.0));
    
    let r = (((color >> 16) & 0xFF) as f32 * factor) as u32;
    let g = (((color >> 8) & 0xFF) as f32 * factor) as u32;
    let b = ((color & 0xFF) as f32 * factor) as u32;
    
    (r << 16) | (g << 8) | b
}

pub fn get_height_color(height: i32, gradient: &HeightGradient) -> u32 {
    let range = (gradient.high_height - gradient.low_height) as f32;
    let normalized = ((height - gradient.low_height) as f32 / range).clamp(0.0, 1.0);
    
    if normalized < 0.5 {
        blend_colors(gradient.low_color, gradient.mid_color, normalized * 2.0)
    } else {
        blend_colors(gradient.mid_color, gradient.high_color, (normalized - 0.5) * 2.0)
    }
}
