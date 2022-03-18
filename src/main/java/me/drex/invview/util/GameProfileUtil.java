package me.drex.invview.util;

import com.mojang.authlib.GameProfile;

public class GameProfileUtil {

    private final GameProfile profile;

    public GameProfileUtil(GameProfile profile) {
        this.profile = profile;
    }

    public String getName() {
        return profile.getName() != null ? profile.getName() : String.valueOf(profile.getId());
    }

    public String getUUID() {
        return (profile.getId() == null) ? "???" : profile.getId().toString();
    }

}
