package dev.rhoffman.gamepadevents;

import com.studiohartman.jamepad.ControllerButton;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LiveTest implements GamepadEventListener {

    // mvn exec:java -Dexec.mainClass="dev.rhoffman.gamepadevents.LiveTest" -Dexec.classpathScope=test
    public static void main(String[] args) {
        new LiveTest();
    }

    private GamepadMonitor m;

    LiveTest() {
        System.out.println("\nCONTROLLER EVENTS TEST");
        System.out.println("----------------------");
        System.out.println("\nPress B to exit any time. It will automatically exit in 60sec.\n");
        m = new GamepadMonitor();
        m.subscribe(this);
        Executors.newScheduledThreadPool(1).schedule(this::shutdown, 60, TimeUnit.SECONDS);
    }

    private void shutdown() {
        System.out.println("Shutting down...");
        m.unsubscribe(this);
        m.shutdown();
        System.exit(0);
    }

    @Override
    public void handleButtonEvent(ButtonEvent event) {
        System.out.println(String.format("Button Event: %12s %s", event.button, event.action));
        if (event.button==ControllerButton.B && event.action==ButtonEvent.Action.PRESSED) shutdown();
    }

    @Override
    public void handleStickEvent(StickEvent event) {
        System.out.println(String.format("Stick Event:  %12s %06.6f", event.axis, event.value));
    }
}
