package rubidium.voice;

public class ProximityManager {
    
    private VoiceConfig config;
    
    public ProximityManager(VoiceConfig config) {
        this.config = config;
    }
    
    public void setConfig(VoiceConfig config) {
        this.config = config;
    }
    
    public double calculateVolume(VoiceChatManager.Vector3d speaker, VoiceChatManager.Vector3d listener) {
        double distance = speaker.distance(listener);
        
        if (distance <= config.falloffStart()) {
            return 1.0;
        } else if (distance >= config.falloffEnd()) {
            return 0.0;
        } else {
            double range = config.falloffEnd() - config.falloffStart();
            double normalized = (distance - config.falloffStart()) / range;
            return 1.0 - easeOutQuad(normalized);
        }
    }
    
    public SpatialAudio calculateSpatialAudio(
        VoiceChatManager.Vector3d speaker,
        VoiceChatManager.Vector3d listener,
        VoiceChatManager.Vector3d listenerForward
    ) {
        if (!config.enable3DAudio()) {
            return new SpatialAudio(0, 0, calculateVolume(speaker, listener));
        }
        
        double dx = speaker.x() - listener.x();
        double dy = speaker.y() - listener.y();
        double dz = speaker.z() - listener.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (distance < 0.001) {
            return new SpatialAudio(0, 0, 1.0);
        }
        
        double dirX = dx / distance;
        double dirY = dy / distance;
        double dirZ = dz / distance;
        
        double forwardX = listenerForward.x();
        double forwardZ = listenerForward.z();
        double forwardLength = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        if (forwardLength > 0) {
            forwardX /= forwardLength;
            forwardZ /= forwardLength;
        }
        
        double rightX = -forwardZ;
        double rightZ = forwardX;
        
        double pan = dirX * rightX + dirZ * rightZ;
        double front = dirX * forwardX + dirZ * forwardZ;
        
        double volume = calculateVolume(speaker, listener);
        
        return new SpatialAudio(pan, dirY, volume);
    }
    
    private double easeOutQuad(double t) {
        return t * (2 - t);
    }
    
    public record SpatialAudio(double pan, double elevation, double volume) {
        public float getLeftVolume() {
            return (float) (volume * (1.0 - Math.max(0, pan)));
        }
        
        public float getRightVolume() {
            return (float) (volume * (1.0 + Math.min(0, pan)));
        }
    }
}
