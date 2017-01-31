package info.nightscout.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import info.nightscout.androidaps.Config;

/**
 * Created by mike on 31.01.2017.
 */

public class SimpleResultCache<Calculation> {
    private static Logger log = LoggerFactory.getLogger(SimpleResultCache.class);
    Map<Long, Calculation> map = new HashMap<>();
    Long storedDataVersion = 0L;

    public void put(Calculation calculation, Long time, Long dataVersion) {
        // data in the caching period changed
        if (!dataVersion.equals(storedDataVersion)) {
            storedDataVersion = dataVersion;
            cleanup();
        }

        map.put(time, calculation);
//        log.debug("Caching " + calculation.getClass().toString() + " entry: " + time);
    }

    public Calculation get(Long time, Long dataVersion) {
        // data in the caching period changed
        if (!dataVersion.equals(storedDataVersion)) {
            storedDataVersion = dataVersion;
            cleanup();
        }

        Calculation entry = map.get(time);
        if (entry != null) { // data not changed
//            log.debug("Cached value for time " + time + " found");
            return entry;
        } else {
            return null;
        }
    }

    private void cleanup() {
        map = new HashMap<>();
    }
}
