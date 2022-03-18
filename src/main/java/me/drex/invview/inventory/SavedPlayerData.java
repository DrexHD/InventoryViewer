package me.drex.invview.inventory;

import java.util.UUID;

public record SavedPlayerData(UUID uuid, SavedInventory savedInventory, SavedEnderchest savedEnderchest) {
}
