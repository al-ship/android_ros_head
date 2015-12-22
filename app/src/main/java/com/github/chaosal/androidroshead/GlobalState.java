package com.github.chaosal.androidroshead;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class GlobalState {

    private static final int SILENCE_FROM_HOURS = 22;
    private static final int SILENCE_TO_HOURS = 9;
    private boolean speaking;
    private boolean listening;
    private StateResolver<Boolean> speakingResolver;

    public synchronized boolean isSpeaking() {
        return speaking | (speakingResolver != null && speakingResolver.getState());
    }

    public synchronized boolean isListening() {
        return listening;
    }

    public synchronized void setSpeaking(boolean speaking) {
        this.speaking = speaking;
    }

    public synchronized void setListening(boolean listening) {
        this.listening = listening;
    }

    public interface StateResolver<T> {
        T getState();
    }

    public synchronized void setSpeakingStateResolver(StateResolver<Boolean> resolver) {
        this.speakingResolver = resolver;
    }

    public boolean canNotify()
    {
        final Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(new Date());
        final int hour = cal.get(Calendar.HOUR);
        return hour < SILENCE_FROM_HOURS && hour >= SILENCE_TO_HOURS;
    }

}
