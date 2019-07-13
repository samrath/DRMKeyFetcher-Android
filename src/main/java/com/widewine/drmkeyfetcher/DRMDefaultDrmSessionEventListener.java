package com.widewine.drmkeyfetcher;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;

/** Listener of {@link DefaultDrmSessionManager} events. */
public interface DRMDefaultDrmSessionEventListener {

    /** Called each time a drm session is acquired. */
    void onDrmSessionAcquired();

    /** Called each time keys are loaded. */
    void onDrmKeysLoaded(byte[] keys);

    /**
     * Called when a drm error occurs.
     *
     * <p>This method being called does not indicate that playback has failed, or that it will fail.
     * The player may be able to recover from the error and continue. Hence applications should
     * <em>not</em> implement this method to display a user visible error or initiate an application
     * level retry ({@link Player.EventListener#onPlayerError} is the appropriate place to implement
     * such behavior). This method is called to provide the application with an opportunity to log the
     * error if it wishes to do so.
     *
     * @param error The corresponding exception.
     */
    void onDrmSessionManagerError(Exception error);

    /** Called each time offline keys are restored. */
    void onDrmKeysRestored();

    /** Called each time offline keys are removed. */
    void onDrmKeysRemoved();

    /** Called each time a drm session is released. */
    void onDrmSessionReleased();
}
