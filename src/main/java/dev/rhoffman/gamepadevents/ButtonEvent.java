package dev.rhoffman.gamepadevents;

import com.studiohartman.jamepad.ControllerButton;

import java.util.EventObject;

public class ButtonEvent extends EventObject {

    public static enum Action {PRESSED, RELEASED}

    public final Action action;

    ButtonEvent(ControllerButton source, Action action) {
        super(source);
        this.action = action;
    }

}
