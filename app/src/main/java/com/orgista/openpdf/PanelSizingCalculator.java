package com.orgista.openpdf;

public final class PanelSizingCalculator {
    static final int MIN_PERCENT = 35;
    static final int MAX_PERCENT = 96;

    public Size calculate(int availableWidth, int availableHeight, int widthProgress, int heightProgress) {
        int width = Math.max(320, scale(availableWidth, widthProgress));
        int height = Math.max(220, scale(availableHeight, heightProgress));
        return new Size(width, height);
    }

    private int scale(int availableSpace, int progress) {
        int percent = MIN_PERCENT + ((MAX_PERCENT - MIN_PERCENT) * clamp(progress)) / 100;
        return Math.max(1, (availableSpace * percent) / 100);
    }

    private int clamp(int progress) {
        return Math.max(0, Math.min(100, progress));
    }

    public static final class Size {
        public final int width;
        public final int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
