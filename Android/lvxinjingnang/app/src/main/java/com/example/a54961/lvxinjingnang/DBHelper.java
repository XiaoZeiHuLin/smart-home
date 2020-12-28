package com.example.a54961.lvxinjingnang;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "my_database";
    public static final String TABLE_NAME = "older_GPS_location";
    public static final int DB_VERSION = 1;
    public static final String TIME = "time";
    public static final String CITY = "city";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";


    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "create table " + TABLE_NAME + "(_id integer primary key autoincrement, " + TIME + " varchar, " + CITY + " varchar, " + LONGITUDE + " varchar, " + LATITUDE + " varchar" + ")";
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
