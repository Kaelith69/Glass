package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        MovieEntity::class,
        ReviewEntity::class,
        MovieListEntity::class,
        BadgeEntity::class,
        NotificationEntity::class,
        MovieEditHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun userDao(): UserDao
    abstract fun reviewDao(): ReviewDao
    abstract fun movieListDao(): MovieListDao
    abstract fun badgeDao(): BadgeDao
    abstract fun notificationDao(): NotificationDao
    abstract fun editHistoryDao(): EditHistoryDao
}
