package com.usth.hospital.security;

public interface CurrentUserProvider {
    String username();

    default CurrentUserSnapshot snapshot() {
        String currentUsername = username();
        return new CurrentUserSnapshot(currentUsername, currentUsername, null);
    }
}
