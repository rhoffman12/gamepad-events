package dev.rhoffman.gamepadevents;

public interface GamepadEventListener {

    void handleButtonEvent(ButtonEvent event);

    void handleStickEvent(StickEvent event);

}
