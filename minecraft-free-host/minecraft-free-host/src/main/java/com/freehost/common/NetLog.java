package com.freehost.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Logs réseau : écrits dans les logs du jeu + gardés en mémoire pour l'écran Host. */
public final class NetLog {
    private static final Logger LOG = LoggerFactory.getLogger("FreeHost");
    private static final Deque<String> LINES = new ArrayDeque<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private NetLog() {}

    public static synchronized void add(String msg) {
        LOG.info(msg);
        LINES.addLast(LocalTime.now().format(FMT) + " " + msg);
        while (LINES.size() > 100) LINES.removeFirst();
    }

    public static synchronized List<String> last(int n) {
        List<String> all = new ArrayList<>(LINES);
        return all.subList(Math.max(0, all.size() - n), all.size());
    }
}
