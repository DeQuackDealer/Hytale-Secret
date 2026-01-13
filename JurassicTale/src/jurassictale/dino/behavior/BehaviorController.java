package jurassictale.dino.behavior;

public interface BehaviorController {
    void update(BehaviorContext context);
    String getBehaviorName();
}
