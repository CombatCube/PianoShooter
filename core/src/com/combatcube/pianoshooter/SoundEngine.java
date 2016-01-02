package com.combatcube.pianoshooter;

import com.badlogic.gdx.utils.TimeUtils;
import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.KeySignature;
import com.leff.midi.event.meta.Tempo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SoundEngine extends Thread {
    public static final long MILLIS_PER_S = 1000;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int COUNT_IN = 4;
    private CsoundAdapter csoundAdapter;

    private Key key = new Key(PitchClass.C);
    private float bpm = 120;
    private long ppq = 480;
    private double currentTick = -COUNT_IN * ppq;

    private MidiFile midiFile;
    private EventMap eventMap;
    private boolean isPlaying;

    public SoundEngine(CsoundAdapter csoundAdapter) {
        isPlaying = false;
        this.csoundAdapter = csoundAdapter;
    }

    public static MidiTrack getTrack(MidiFile midiFile, int trackNo) {
        if (trackNo < 0 || trackNo > midiFile.getTrackCount()) {
            return null;
        }
        MidiTrack track = midiFile.getTracks().get(trackNo);
        Iterator<MidiEvent> it = track.getEvents().iterator();
        List<MidiEvent> eventsToRemove = new ArrayList<MidiEvent>();
        while (it.hasNext()) {
            MidiEvent event = it.next();
            if (!(event instanceof NoteOn)
                    && !(event instanceof NoteOff)
                    && !(event instanceof Tempo)
                    && !(event instanceof KeySignature)) {
                eventsToRemove.add(event);
            }
        }
        for (MidiEvent event : eventsToRemove) {
            track.removeEvent(event);
        }
        return track;
    }

    public static Key getFirstKeySig(EventMap eventMap) {
        for (MidiEvent event : eventMap.events) {
            if (event.getTick() == 0) {
                if (event instanceof KeySignature) {
                    KeySignature keySig = (KeySignature) event;
                    return new Key(PitchClass.C.successor(keySig.getKey()));
                }
            } else {
                break;
            }
        }
        return new Key(PitchClass.C);
    }

    private void readNotes() {
        eventMap = new EventMap();
        MidiTrack mainTrack = SoundEngine.getTrack(midiFile, 0);
        eventMap.addTrackEvents(mainTrack, true);
        for (int i = 1; i < midiFile.getTrackCount(); i++) {
            MidiTrack otherTrack = SoundEngine.getTrack(midiFile, 1);
            eventMap.addTrackEvents(otherTrack, false);
        }
    }

    public void init(String filename) {
        csoundAdapter.init();
        try {
            midiFile = CsoundAdapter.loadMidi(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        midiFile.getResolution();
        readNotes();
        csoundAdapter.load();
    }

    public double getCurrentTick() {
        return currentTick;
    }

    @Override
    public void run() {
        EventVisitor visitor = new PerformEventVisitor(this);
        csoundAdapter.readScore();
        isPlaying = true;
        csoundAdapter.start();
        long prevTime = TimeUtils.millis();
        key = getFirstKeySig(eventMap);
        Iterator<MidiEvent> it = eventMap.events.iterator();
        MidiEvent nextEvent = it.next(); //assumes first note exists
        while (isPlaying) {
            long newTime = TimeUtils.millis();
            currentTick += secondsToTicks((newTime - prevTime) / (double) MILLIS_PER_S);
            prevTime = newTime;
            if (nextEvent != null) {
                if (nextEvent.getTick() < currentTick
                        || (nextEvent instanceof KeySignature)) {
                    nextEvent.accept(visitor);
                    if (it.hasNext()) {
                        nextEvent = it.next();
                    } else {
                        nextEvent = null;
                    }
                }
            }
        }
    }

    public float getBpm() {
        return bpm;
    }

    public void setBpm(float bpm) {
        this.bpm = bpm;
    }

    public double secondsToTicks(double seconds) {
        // S / S/M = M
        // M * Q/M = Q
        // Q * T/Q = T
        return (seconds / (double) SECONDS_PER_MINUTE) * (double) bpm * (double) ppq;
    }

    public void playNote(int channel, int pitch, int velocity) {
        csoundAdapter.playNote(channel + 11, -1, pitch, velocity);
    }

    public void playNote(NoteOn noteOn) {
        playNote(noteOn.getChannel(), noteOn.getNoteValue(), noteOn.getVelocity());
    }

    public void playNote(NoteOff noteOff) {
        playNote(noteOff.getChannel(), noteOff.getNoteValue(), 0);
    }

    public EventMap getEventMap() {
        return eventMap;
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }
}
