package com.alieducational.inventory.data;
import androidx.room.*;

@Dao
public interface FoundationDao {
    @Query("SELECT * FROM settings WHERE id=1") SettingsRecord settings();
    @Query("SELECT * FROM drafts WHERE id=:id") DraftRecord draft(String id);
    @Insert(onConflict=OnConflictStrategy.REPLACE) void saveSettings(SettingsRecord settings);
    @Insert(onConflict=OnConflictStrategy.REPLACE) void saveDraft(DraftRecord draft);
}
