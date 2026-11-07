package com.alieducational.inventory;

import android.content.Context;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.alieducational.inventory.data.*;
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;
import static org.junit.Assert.*;

/** Tests SQLCipher/Room storage only. Does not bypass or claim to test the app's biometric gate. */
@RunWith(AndroidJUnit4.class)
public class FoundationDatabaseTest {
    private Context context;
    private String name;
    private byte[] key;
    private ShopDatabase database;
    @Before public void prepare() {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        name="instrument-"+UUID.randomUUID()+".db";
        key=new byte[32];new SecureRandom().nextBytes(key);
        System.loadLibrary("sqlcipher");
        database=open(key);
    }
    private ShopDatabase open(byte[] password) {
        return Room.databaseBuilder(context,ShopDatabase.class,name)
            .openHelperFactory(new SupportOpenHelperFactory(password))
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build();
    }
    @After public void finish() {
        if(database!=null)database.close();
        // Only this run's newly created disposable database; never shop.db or app-wide data.
        if(name!=null && name.startsWith("instrument-"))context.deleteDatabase(name);
        if(key!=null)java.util.Arrays.fill(key,(byte)0);
    }
    @Test public void settingsAndUrduDraftSurviveReopen() {
        SettingsRecord settings=new SettingsRecord();settings.shopName="TEST SHOP";settings.language="ur";settings.dark=false;
        DraftRecord draft=new DraftRecord();draft.text="ٹیسٹ قلم PEN-001 ₹550";draft.updatedAt=1234;
        database.runInTransaction(()->{database.foundation().saveSettings(settings);database.foundation().saveDraft(draft);});
        database.close();database=open(key);
        assertEquals("TEST SHOP",database.foundation().settings().shopName);
        assertEquals("ur",database.foundation().settings().language);
        assertFalse(database.foundation().settings().dark);
        assertEquals(draft.text,database.foundation().draft(draft.id).text);
        assertEquals(1234,database.foundation().draft(draft.id).updatedAt);
    }
    @Test public void exceptionRollsBackAllRelatedWrites() {
        SettingsRecord original=new SettingsRecord();database.foundation().saveSettings(original);
        try {
            database.runInTransaction(()->{
                SettingsRecord changed=new SettingsRecord();changed.shopName="SHOULD ROLL BACK";
                database.foundation().saveSettings(changed);
                database.foundation().saveDraft(new DraftRecord());
                throw new IllegalStateException("injected failure");
            });
            fail("Expected injected exception");
        } catch(IllegalStateException expected) {assertEquals("injected failure",expected.getMessage());}
        database.close();database=open(key);
        assertEquals("Ali Educational",database.foundation().settings().shopName);
        assertNull(database.foundation().draft("foundation-note"));
    }
    @Test public void fileIsEncryptedAndWrongKeyCannotReadIt() throws Exception {
        SettingsRecord settings=new SettingsRecord();settings.shopName="PRIVATE TEST MARKER 891234";
        database.foundation().saveSettings(settings);database.close();
        byte[] bytes;
        try(FileInputStream stream=new FileInputStream(context.getDatabasePath(name));
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;while((n=stream.read(buffer))!=-1)out.write(buffer,0,n);bytes=out.toByteArray();
        }
        assertFalse(new String(bytes,0,16,StandardCharsets.US_ASCII).startsWith("SQLite format 3"));
        assertFalse(new String(bytes,StandardCharsets.ISO_8859_1).contains(settings.shopName));
        byte[] wrong=new byte[32];new SecureRandom().nextBytes(wrong);
        database=open(wrong);
        boolean rejected=false;
        try {database.foundation().settings();} catch(Exception expected){rejected=true;}
        assertTrue("Wrong key must be rejected",rejected);
        database.close();database=open(key);
        assertEquals(settings.shopName,database.foundation().settings().shopName);
    }
    @Test public void draftReplacementDoesNotDuplicateRecord() {
        DraftRecord draft=new DraftRecord();draft.text="first";database.foundation().saveDraft(draft);
        draft.text="second";database.foundation().saveDraft(draft);
        database.close();database=open(key);
        assertEquals("second",database.foundation().draft(draft.id).text);
        try(android.database.Cursor cursor=database.query("SELECT count(*) FROM drafts",null)){
            assertTrue(cursor.moveToFirst());assertEquals(1,cursor.getInt(0));
        }
    }
}
