package com.wjx.forge.projectexa.gui.animation;

/** Reversible drawer motion and in-place content transitions for station panels. */
public final class StationPanelAnimation {
    public static final long DURATION_MS = 180;
    private int station;
    private int target;
    private float progress;
    private int previousStation;
    private float switchProgress;
    private float craftingFrom;
    private long lastUpdate;

    public StationPanelAnimation(int station, long now) {
        reset(station, now);
    }

    public void reset(int value, long now) {
        station = target = value;
        progress = value < 0 ? 0 : 1;
        previousStation = -1;
        switchProgress = 1;
        craftingFrom = value == 0 ? 1 : 0;
        lastUpdate = now;
    }

    public void select(int value, long now) {
        update(now);
        target = target == value ? -1 : value;
        if (target >= 0 && station != target) {
            // Switching replaces the open contents directly. Only reselecting
            // the active station targets the collapsed state.
            craftingFrom = craftingWeight();
            previousStation = station;
            switchProgress = station < 0 ? 1 : 0;
            station = target;
        }
        update(now);
    }

    public void update(long now) {
        float remaining = Math.max(0, now - lastUpdate) / (float) DURATION_MS;
        lastUpdate = now;
        switchProgress = Math.min(1, switchProgress + remaining);
        if (switchProgress >= 1) previousStation = -1;
        if (target < 0) {
            progress = Math.max(0, progress - remaining);
            if (progress <= 0) {
                station = previousStation = -1;
                switchProgress = 1;
            }
        } else progress = Math.min(1, progress + remaining);
    }

    public int station() { return station; }
    public int target() { return target; }
    public int previousStation() { return previousStation; }
    public float switchProgress() { return ease(switchProgress); }
    public float craftingWeight() {
        float next = station == 0 ? 1 : 0;
        return previousStation < 0 ? next : craftingFrom + (next - craftingFrom) * switchProgress();
    }
    public static float ease(float value) { return value * value * (3 - 2 * value); }
    public float openness() { return ease(progress); }
    public boolean isMoving() { return station != target || station >= 0 && (progress < 1 || switchProgress < 1); }
}
