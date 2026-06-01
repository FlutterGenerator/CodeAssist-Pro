package com.tyron.builder.compiler;

public class ComposeStaticCallback {
    public static BuildResult callback = null;
    public static interface BuildResult{
        void done(Status s);
    }
    public static enum Status{
        SUCCESS("SUCCESS"),
        FAILED("FAILED");

        Status(String status) {
        }
    }
}
