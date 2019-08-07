package dev.rhoffman.gamepadevents;

import com.studiohartman.jamepad.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GamepadMonitor {

    private ControllerManager controllers;
    private ControllerIndex activeController;
    private Boolean[] buttonState;
    private float[] axesState, axesLastUpdate;
    private final float axisDeadzone;
    private final float axisUpdateThreshold;

    private List<GamepadEventListener> listeners = new ArrayList<>();

    private ScheduledExecutorService thread = Executors.newScheduledThreadPool(1);
    private ScheduledFuture timer;
    private final int INTERVAL; // ms

    private final Logger log = Logger.getLogger(GamepadMonitor.class.getName());

    public Boolean isConnected() {
        return (activeController != null) && (activeController.isConnected());
    }

    public GamepadMonitor(ControllerManager manager, int i, int ms, float deazone, float threshold) {
        controllers = manager;
        INTERVAL = ms;
        axisDeadzone = deazone;
        axisUpdateThreshold = threshold;
        assert(axisUpdateThreshold <= axisDeadzone); // otherwise stick release may never register
        setActiveController(i);
    }

    public GamepadMonitor() {
        controllers = new ControllerManager();
        controllers.initSDLGamepad();
        INTERVAL = 50;
        axisDeadzone = 0.1f;
        axisUpdateThreshold = 0.01f;
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
        axesLastUpdate = new float[ControllerAxis.values().length];
        Arrays.fill(axesLastUpdate, Float.NEGATIVE_INFINITY);
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

    private void start() {
        timer = thread.scheduleAtFixedRate(this::update, 0, INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (timer!=null && !timer.isDone()) timer.cancel(true);
    }

    public void shutdown() {
        if (timer != null) timer.cancel(true);
        thread.shutdown();
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
            if (Math.abs(temp - axesLastUpdate[i]) > axisUpdateThreshold) {
                dispatchStickEvent(axis, temp);
                axesLastUpdate[i] = temp;
            }
            axesState[i] = temp;
        }

    }

    synchronized public void subscribe(GamepadEventListener target) {
        listeners.add(target);
        if (timer==null || timer.isDone()) start();
    }

    synchronized public void unsubscribe(GamepadEventListener target) {
        listeners.remove(target);
        if (listeners.isEmpty()) stop();
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
