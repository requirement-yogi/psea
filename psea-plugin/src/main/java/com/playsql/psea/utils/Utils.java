package com.playsql.psea.utils;

import java.util.concurrent.TimeUnit;

public class Utils {

    public static class Clock {
        private final String prefix;
        private final long start;
        private long lap;
        private long lapDuration = 0L;

        private Clock(String prefix) {
            this.prefix = prefix;
            start = System.nanoTime();
            lap = start;
        }

        public static Clock start() {
            return start("");
        }
        public static Clock start(String prefix) {
            return new Clock(prefix);
        }

        public String time() {
            long now = System.nanoTime();
            String result = TimeUnit.NANOSECONDS.toMillis(now - start) + "ms";
            lap = now;
            return result;
        }

        public long timeMillis() {
            long now = System.nanoTime();
            return TimeUnit.NANOSECONDS.toMillis(now - start);
        }

        public Clock lap() {
            long now = System.nanoTime();
            lapDuration = now - lap;
            lap = now;
            return this;
        }

        public String lapTime() {
            return TimeUnit.NANOSECONDS.toMillis(lapDuration) + "ms";
        }

        public String time(String message) {
            long now = System.nanoTime();
            String result = prefix + message + " at " + TimeUnit.NANOSECONDS.toMillis(now - start) + "ms.";
            lap = now;
            return result;
        }
    }
}
