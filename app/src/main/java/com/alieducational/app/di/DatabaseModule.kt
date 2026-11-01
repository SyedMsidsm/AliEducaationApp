package com.alieducational.app.di

import android.content.Context
import androidx.room.Room
import com.alieducational.app.data.local.AppDatabase
import com.alieducational.app.data.local.dao.AccountPaymentDao
import com.alieducational.app.data.local.dao.CustomerDao
import com.alieducational.app.data.local.dao.HeldBillDao
import com.alieducational.app.data.local.dao.ImportBatchDao
import com.alieducational.app.data.local.dao.PigmyDepositDao
import com.alieducational.app.data.local.dao.CashCategoryDao
import com.alieducational.app.data.local.dao.PigmySchemeDao
import com.alieducational.app.data.local.dao.PigmyWithdrawalDao
import com.alieducational.app.data.local.dao.ProductBarcodeDao
import com.alieducational.app.data.local.dao.ProductDao
import com.alieducational.app.data.local.dao.ProductGiveawayDao
import com.alieducational.app.data.local.dao.PurchaseDao
import com.alieducational.app.data.local.dao.SaleDao
import com.alieducational.app.data.local.dao.ServiceDefinitionDao
import com.alieducational.app.data.local.dao.ShopExpenseDao
import com.alieducational.app.data.local.dao.StockMovementDao
import com.alieducational.app.data.local.dao.SupplierDao
import com.alieducational.app.data.local.dao.SyncEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ali_educational.db"
        )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )
        .build()
    }

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideProductBarcodeDao(db: AppDatabase): ProductBarcodeDao = db.productBarcodeDao()

    @Provides
    fun provideStockMovementDao(db: AppDatabase): StockMovementDao = db.stockMovementDao()

    @Provides
    fun provideSupplierDao(db: AppDatabase): SupplierDao = db.supplierDao()

    @Provides
    fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()

    @Provides
    fun providePurchaseDao(db: AppDatabase): PurchaseDao = db.purchaseDao()

    @Provides
    fun provideSaleDao(db: AppDatabase): SaleDao = db.saleDao()

    @Provides
    fun provideAccountPaymentDao(db: AppDatabase): AccountPaymentDao = db.accountPaymentDao()

    @Provides
    fun provideShopExpenseDao(db: AppDatabase): ShopExpenseDao = db.shopExpenseDao()

    @Provides
    fun provideImportBatchDao(db: AppDatabase): ImportBatchDao = db.importBatchDao()

    @Provides
    fun provideHeldBillDao(db: AppDatabase): HeldBillDao = db.heldBillDao()

    @Provides
    fun providePigmyDepositDao(db: AppDatabase): PigmyDepositDao = db.pigmyDepositDao()

    @Provides
    fun providePigmySchemeDao(db: AppDatabase): PigmySchemeDao = db.pigmySchemeDao()

    @Provides
    fun provideProductGiveawayDao(db: AppDatabase): ProductGiveawayDao = db.productGiveawayDao()

    @Provides
    fun provideServiceDefinitionDao(db: AppDatabase): ServiceDefinitionDao = db.serviceDefinitionDao()

    @Provides
    fun provideSyncEventDao(db: AppDatabase): SyncEventDao = db.syncEventDao()

    @Provides
    fun provideCashCategoryDao(db: AppDatabase): CashCategoryDao = db.cashCategoryDao()

    @Provides
    fun providePigmyWithdrawalDao(db: AppDatabase): PigmyWithdrawalDao = db.pigmyWithdrawalDao()
}