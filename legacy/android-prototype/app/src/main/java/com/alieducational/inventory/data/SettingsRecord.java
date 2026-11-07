package com.alieducational.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="settings")
public class SettingsRecord {
    @PrimaryKey public int id = 1;
    @NonNull public String shopName = "Ali Educational";
    @NonNull public String language = "en";
    public boolean dark = true;
    public int lockSeconds = 60;
    public boolean setupComplete = false;
}
