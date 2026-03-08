package com.tendoarisu.dglab.script.wrappers;

import com.tendoarisu.dglab.core.DgLabConnection;

public final class StrengthWrapper {

    private final DgLabConnection conn;

    public StrengthWrapper(DgLabConnection conn) {
        this.conn = conn;
    }

    public int getACurrentStrength() {
        return conn.getACurrent();
    }

    public int getBCurrentStrength() {
        return conn.getBCurrent();
    }

    public int getAMaxStrength() {
        return conn.getAMax();
    }

    public int getBMaxStrength() {
        return conn.getBMax();
    }
}
