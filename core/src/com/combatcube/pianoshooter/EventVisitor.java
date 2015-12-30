package com.combatcube.pianoshooter;

import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.KeySignature;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.event.meta.TimeSignature;

/**
 * Interface for visiting specific MidiEvents.
 * Created by andrew on 12/29/2015.
 */
public interface EventVisitor {
    void visit(NoteOn noteOn);

    void visit(NoteOff noteOff);

    void visit(KeySignature keySig);

    void visit(TimeSignature timeSig);

    void visit(Tempo tempo);
}