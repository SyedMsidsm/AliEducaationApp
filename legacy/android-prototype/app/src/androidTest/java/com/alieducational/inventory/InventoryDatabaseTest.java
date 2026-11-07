package com.alieducational.inventory;
import android.content.Context;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.alieducational.inventory.data.*;
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class) public class InventoryDatabaseTest {
    Context context;String name;ShopDatabase db;InventoryRepository repo;
    @Before public void setup(){
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        name="instrument-inventory-"+UUID.randomUUID()+".db";
        System.loadLibrary("sqlcipher");
        db=Room.databaseBuilder(context,ShopDatabase.class,name).openHelperFactory(new SupportOpenHelperFactory("disposable-test-key".getBytes())).build();
        repo=new InventoryRepository(db);
    }
    @After public void cleanup(){db.close();context.deleteDatabase(name);}
    ProductRecord product(String id,String sku){ProductRecord p=new ProductRecord();p.id=id;p.name="TEST Pen";p.sku=sku;p.packSize=10;return p;}
    @Test public void packsDamageAndHistoryReconcile(){
        repo.create(product("p","PEN"),30,30000L,"open");
        repo.move("p",2,true,"SHOP_USE","test use",false,"out");
        assertEquals(10,db.inventory().sellable("p"));
        repo.move("p",3,false,"DAMAGE","test damage",false,"damage");
        StockRow row=db.inventory().stock().get(0);assertEquals(7,row.sellable);assertEquals(3,row.damaged);
        assertEquals(3,db.inventory().history("p").size());
        assertEquals(10,db.inventory().movement("out").packSizeSnapshot);
    }
    @Test public void retriesDoNotDoublePost(){
        ProductRecord p=product("p","PEN");repo.create(p,30,null,"open");repo.create(p,30,null,"open");
        repo.move("p",2,true,"GIVEAWAY","test",false,"out");repo.move("p",2,true,"GIVEAWAY","test",false,"out");
        assertEquals(10,db.inventory().sellable("p"));assertEquals(2,db.inventory().history("p").size());
    }
    @Test public void duplicateSkuRollsBackProductAndOpening(){
        repo.create(product("p","pen"),10,null,"open");
        try{repo.create(product("other"," PEN "),30,null,"open2");fail("Duplicate SKU accepted");}catch(Exception expected){}
        assertEquals(1,db.inventory().stock().size());assertNull(db.inventory().movement("open2"));
    }
    @Test public void shortageRequiresExplicitConfirmation(){
        repo.create(product("p",null),3,null,"open");
        try{repo.move("p",5,false,"SHOP_USE","test",false,"out");fail("Shortage accepted");}catch(IllegalArgumentException expected){}
        assertEquals(3,db.inventory().sellable("p"));assertNull(db.inventory().movement("out"));
        repo.move("p",5,false,"SHOP_USE","test",true,"out");assertEquals(-2,db.inventory().sellable("p"));
    }
    @Test public void unknownOpeningCostStaysUnknown(){
        repo.create(product("p",null),10,null,"open");assertNull(db.inventory().movement("open").openingCostPaise);
    }
    @Test public void packSellingPriceIsIndependentAndOptional(){
        ProductRecord p=product("p",null);p.sellingPaise=1500;p.packSellingPaise=14000L;
        repo.create(p,30,null,"open");
        assertEquals(Long.valueOf(14000),db.inventory().product("p").packSellingPaise);
        assertEquals(1500,db.inventory().product("p").sellingPaise);
        ProductRecord changed=db.inventory().product("p");changed.packSellingPaise=null;repo.edit(changed,"edit");
        assertNull(db.inventory().product("p").packSellingPaise);
        assertEquals(30,db.inventory().sellable("p"));
    }
    @Test public void versionTwoMigrationPreservesProductsAndMovements(){
        db.close();context.deleteDatabase(name);
        net.zetetic.database.sqlcipher.SQLiteDatabase old=net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name),"disposable-test-key",null,null,null);
        old.execSQL("CREATE TABLE settings (id INTEGER NOT NULL PRIMARY KEY, shopName TEXT NOT NULL, language TEXT NOT NULL, dark INTEGER NOT NULL, lockSeconds INTEGER NOT NULL, setupComplete INTEGER NOT NULL)");
        old.execSQL("CREATE TABLE drafts (id TEXT NOT NULL PRIMARY KEY, text TEXT NOT NULL, updatedAt INTEGER NOT NULL)");
        old.close();
        // Build the exact version-2 tables through the already tested migration.
        androidx.sqlite.db.SupportSQLiteOpenHelper helper=new SupportOpenHelperFactory("disposable-test-key".getBytes()).create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(new androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2){
                @Override public void onCreate(androidx.sqlite.db.SupportSQLiteDatabase d){DatabaseMigrations.V1_TO_V2.migrate(d);}
                @Override public void onUpgrade(androidx.sqlite.db.SupportSQLiteDatabase d,int a,int b){DatabaseMigrations.V1_TO_V2.migrate(d);}
            }).build());
        androidx.sqlite.db.SupportSQLiteDatabase d=helper.getWritableDatabase();
        d.execSQL("INSERT INTO products VALUES('p','TEST prior','SKU','','','',10,1500,5,0)");
        d.execSQL("INSERT INTO stock_movements VALUES('open','p','OPENING',23,3,10,123,'prior stock',23000)");
        helper.close();
        db=Room.databaseBuilder(context,ShopDatabase.class,name).openHelperFactory(new SupportOpenHelperFactory("disposable-test-key".getBytes())).addMigrations(DatabaseMigrations.V2_TO_V3).build();
        assertEquals(23,db.inventory().sellable("p"));
        assertEquals(3,db.inventory().stock().get(0).damaged);
        assertNull(db.inventory().product("p").packSellingPaise);
        assertEquals(1500,db.inventory().product("p").sellingPaise);
        assertEquals(Long.valueOf(23000),db.inventory().movement("open").openingCostPaise);
    }
    @Test public void countedTotalRecordsDifferenceAndRetryIsIdempotent(){
        repo.create(product("p",null),30,null,"open");
        repo.setCount("p",23,"Shelf count","count");
        assertEquals(23,db.inventory().sellable("p"));
        assertEquals(-7,db.inventory().movement("count").sellableDelta);
        repo.move("p",1,false,"SHOP_USE","test",false,"use");
        repo.setCount("p",23,"Shelf count","count");
        assertEquals(22,db.inventory().sellable("p"));
        repo.setCount("p",0,"Empty shelf","zero");
        assertEquals(0,db.inventory().sellable("p"));
    }
    @Test public void editPreservesStockAndHistoricPackSize(){
        repo.create(product("p","PEN"),30,null,"open");
        repo.move("p",1,true,"SHOP_USE","test",false,"use");
        ProductRecord changed=db.inventory().product("p");
        changed.packSize=12;changed.name="TEST renamed";
        repo.edit(changed,"edit");
        assertEquals(20,db.inventory().sellable("p"));
        assertEquals(10,db.inventory().movement("use").packSizeSnapshot);
        assertEquals(12,db.inventory().product("p").packSize);
        assertTrue(db.inventory().movement("edit").note.contains("before"));
    }
    @Test public void archiveRetainsHistoryAndRejectsNewMovements(){
        repo.create(product("p",null),30,null,"open");
        ProductRecord changed=db.inventory().product("p");changed.archived=true;repo.edit(changed,"archive");
        assertEquals(30,db.inventory().sellable("p"));
        assertEquals(2,db.inventory().history("p").size());
        try{repo.move("p",1,false,"SHOP_USE","test",false,"use");fail("Archived movement accepted");}catch(IllegalStateException expected){}
        assertNull(db.inventory().movement("use"));
        changed.archived=false;repo.edit(changed,"restore");
        assertFalse(db.inventory().product("p").archived);
    }
    @Test public void migrationPreservesOldSettingsAndDraft() {
        db.close();context.deleteDatabase(name);
        net.zetetic.database.sqlcipher.SQLiteDatabase old=net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name),"disposable-test-key",null,null,null);
        old.execSQL("CREATE TABLE settings (id INTEGER NOT NULL PRIMARY KEY, shopName TEXT NOT NULL, language TEXT NOT NULL, dark INTEGER NOT NULL, lockSeconds INTEGER NOT NULL, setupComplete INTEGER NOT NULL)");
        old.execSQL("CREATE TABLE drafts (id TEXT NOT NULL PRIMARY KEY, text TEXT NOT NULL, updatedAt INTEGER NOT NULL)");
        old.execSQL("INSERT INTO settings VALUES(1,'MIGRATION TEST','ur',0,60,1)");
        old.execSQL("INSERT INTO drafts VALUES('foundation-note','PRESERVE ME',123)");
        old.setVersion(1);old.close();
        db=Room.databaseBuilder(context,ShopDatabase.class,name).openHelperFactory(new SupportOpenHelperFactory("disposable-test-key".getBytes())).addMigrations(DatabaseMigrations.V1_TO_V2,DatabaseMigrations.V2_TO_V3).build();
        assertEquals("MIGRATION TEST",db.foundation().settings().shopName);
        assertEquals("PRESERVE ME",db.foundation().draft("foundation-note").text);
        assertTrue(db.inventory().stock().isEmpty());
        new InventoryRepository(db).create(product("new","NEW"),23,23000L,"new-open");
        assertEquals(23,db.inventory().sellable("new"));
    }
}
