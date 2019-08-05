package dev.rhoffman.gamepadevents;

import com.studiohartman.jamepad.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControllerMonitor {

    private ControllerManager controllers;
    private ControllerIndex activeController;
    private Boolean[] buttonState;
    private float[] axesState;
    private final float axisDeadzone;
    private final float axisUpdateThreshold;

    private List<GamepadEventListener> listeners = new ArrayList<>();

    private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private final int INTERVAL; // ms

    private final Logger log = Logger.getLogger(ControllerMonitor.class.getName());

    public Boolean isConnected() {
        return (activeController != null) && (activeController.isConnected());
    }

    public ControllerMonitor(ControllerManager manager, int i, int ms, float deazone, float threshold) {
        controllers = manager;
        INTERVAL = ms;
        axisDeadzone = deazone;
        axisUpdateThreshold = threshold;
        setActiveController(i);
    }

    public ControllerMonitor() {
        controllers = new ControllerManager();
        controllers.initSDLGamepad();
        INTERVAL = 50;
        axisDeadzone = 0.05f;
        axisUpdateThreshold = 0.05f;
        setActiveController(0);
    }

    private void setActiveController(int i) {
        // initialize most things here
        stop();
        activeController = controllers.getControllerIndex(i);
        buttonState = new Boolean[ControllerButton.values().length];
        Arrays.fill(buttonState, false);
        axesState = new float[ControllerAxis.values().length];
        Arrays.fill(axesState, 0f);
        timer.scheduleAtFixedRate(this::update, 100, INTERVAL, TimeUnit.MILLISECONDS);
    }

    // update all
    private void update() {
        try {
            updateAxes();
            updateButtons();
        } catch (ControllerUnpluggedException e) {
            log.log(Level.SEVERE, "Lost connection to controller!", e);
            stop();
        }
    }

    private void stop() {
        timer.shutdown();
    }

    // loop through buttons and update
    private void updateButtons() throws ControllerUnpluggedException {
        Boolean temp;
        int i;
        for (ControllerButton button : ControllerButton.values()) {
            i = button.ordinal();
            temp = activeController.isButtonPressed(button);
            if (temp && !buttonState[i]) dispatchButtonEvent(button, ButtonEvent.Action.PRESSED);
            if (buttonState[i] && !temp) dispatchButtonEvent(button, ButtonEvent.Action.RELEASED);
            buttonState[i] = temp;
        }
    }

    // loop through stick axes and update
    private void updateAxes() throws ControllerUnpluggedException {
        float temp;
        int i;
        for (ControllerAxis axis : ControllerAxis.values()) {
            i = axis.ordinal();
            temp = activeController.getAxisState(axis);
            if (Math.abs(temp) < axisDeadzone) temp = 0f;
            if (Math.abs(temp - axesState[i]) > axisUpdateThreshold || (temp == 0f && axesState[i] != 0f)) {
                dispatchStickEvent(axis, temp);
            }
        }

    }

    synchronized public void subscribe(GamepadEventListener target) {
        listeners.add(target);
    }

    private void dispatchButtonEvent(ControllerButton button, ButtonEvent.Action action) {
        ButtonEvent event = new ButtonEvent(button, action);
        for (GamepadEventListener target : listeners) target.handleButtonEvent(event);
    }

    private void dispatchStickEvent(ControllerAxis axis, float value) {
        StickEvent event = new StickEvent(axis, value);
        for (GamepadEventListener target : listeners) target.handleStickEvent(event);
    }


}
