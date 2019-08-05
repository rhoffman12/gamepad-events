package dev.rhoffman.gamepadevents;

import com.studiohartman.jamepad.ControllerAxis;

import java.util.EventObject;

public class StickEvent extends EventObject {

    public final float value;

    StickEvent(ControllerAxis source, float value) {
        super(source);
        this.value = value;
    }

}
