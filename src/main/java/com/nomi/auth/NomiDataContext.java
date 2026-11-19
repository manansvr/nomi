package com.nomi.auth;

public final class NomiDataContext {
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private NomiDataContext() {}

    public static void setUserId(String userId) { USER_ID.set(userId); }
    public static String userId() { return USER_ID.get() == null ? "global" : USER_ID.get(); }
    public static void clear() { USER_ID.remove(); }
}
