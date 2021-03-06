package com.combatcube.pianopaddle;

import com.leff.midi.event.NoteOn;

/**
 * Class to hold a note event.
 * Created by andrew on 12/25/2015.
 */
public class Note extends NoteOn {
    public long duration;
    public int originalVelocity;
    public int scaleDegree;
    public boolean passed;
    public boolean touched;
    public boolean missed;
    public boolean late;
    public int interval;

    public Note(NoteOn noteOn, long duration) {
        super(noteOn.getTick(), noteOn.getChannel(), noteOn.getNoteValue(), noteOn.getVelocity());
        this.duration = duration;
        this.scaleDegree = -1;
        this.originalVelocity = noteOn.getVelocity();
        this.passed = false;
        this.touched = false;
        this.missed = false;
        this.late = false;
        this.interval = 0;
    }

    @Override
    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
