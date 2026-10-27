package xaeroplus.feature.extensions;

import xaeroplus.feature.render.async.AsyncTileBuffers;

/**
 * Per-chunk async buffer state, mixed into {@code MapTileChunk}.
 */
public interface AsyncBufferedMapTileChunk {
    /**
     * Bumped whenever the chunk's source data may have changed; a job built for an older value is stale.
     */
    int xaeroPlus$generation();

    AsyncTileBuffers.Job xaeroPlus$job();

    void xaeroPlus$setJob(AsyncTileBuffers.Job job);

    /**
     * Set when a job failed, so the next build runs through Xaero's own synchronous path once.
     */
    boolean xaeroPlus$syncFallback();

    void xaeroPlus$setSyncFallback(boolean syncFallback);
}
