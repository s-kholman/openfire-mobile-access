package ru.krimm.openfire.mobileaccess.auth;

record StoredCredential(String algorithm, int iterations, String salt, String hash, boolean enabled) {
}
