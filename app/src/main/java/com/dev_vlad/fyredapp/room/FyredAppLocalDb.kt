package com.dev_vlad.fyredapp.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dev_vlad.fyredapp.room.dao.MyContactsDao
import com.dev_vlad.fyredapp.room.entities.MyContacts


//up version must be incremented when you change schema
//exportSchema can be false if you do not wish to save the schema to a folder as a version history
@Database(
    entities = [MyContacts::class],
    version = 1,
    exportSchema = false
)
abstract class FyredAppLocalDb : RoomDatabase() {
    abstract val myContactsDao: MyContactsDao

    companion object {
        //annotated with volatile to make sure the value of instance is always up to date -never cached
        @Volatile
        private var INSTANCE: FyredAppLocalDb? = null

        fun fyredAppRoomDbInstance(context: Context): FyredAppLocalDb {
            //so that multiple instances do not request this instance at the same time
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    //create database
                    instance = Room.databaseBuilder(
                        context,
                        FyredAppLocalDb::class.java,
                        "fyred_app_sq_lite_db"
                    )
                        .fallbackToDestructiveMigration() //wipe existing db and re-create //TODO NOT GOOD
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }

}