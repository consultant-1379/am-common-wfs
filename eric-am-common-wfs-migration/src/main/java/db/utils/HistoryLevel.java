/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package db.utils;

public enum HistoryLevel {
    NONE("none", 0),
    ACTIVITY("activity", 1),
    AUDIT("audit", 2),
    FULL("full", 3);

    private final String name;
    private final int id;

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    HistoryLevel(String name, int id) {
        this.name = name;
        this.id = id;
    }
}
