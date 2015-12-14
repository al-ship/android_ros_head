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
        Date now = new Date();

        Calendar from = GregorianCalendar.getInstance();
        from.set(GregorianCalendar.HOUR, SILENCE_FROM_HOURS);

        Calendar to = GregorianCalendar.getInstance();
        to.add(GregorianCalendar.DAY_OF_MONTH, 1);
        to.set(GregorianCalendar.HOUR, SILENCE_TO_HOURS);

        return now.before(from.getTime()) || now.after(to.getTime());
    }

}
