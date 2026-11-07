package com.alieducational.inventory.data;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="drafts")
public class DraftRecord {
    @PrimaryKey @NonNull public String id = "foundation-note";
    @NonNull public String text = "";
    public long updatedAt;
}
