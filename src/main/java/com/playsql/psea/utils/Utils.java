package com.playsql.psea.utils;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 - 2021 Requirement Yogi S.A.S.U.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
