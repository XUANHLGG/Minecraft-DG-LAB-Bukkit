package com.tendoarisu.dglab.script;

import org.mozilla.javascript.Context;

public final class SandboxContextFactory {

    private static final SandboxContextFactory INSTANCE = new SandboxContextFactory();

    private SandboxContextFactory() {}

    public static SandboxContextFactory instance() {
        return INSTANCE;
    }


    public Context enter() {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.setOptimizationLevel(-1);
        cx.setClassShutter(new SandboxClassShutter());
        return cx;
    }

    public void exit() {
        Context.exit();
    }
}
