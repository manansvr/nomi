package com.nomi.auth;

import java.io.Serializable;

public record AuthUser(String id, String name, String email) implements Serializable {}
