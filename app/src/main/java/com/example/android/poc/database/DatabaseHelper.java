package com.example.android.poc.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by gopal on 11/18/2015.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "PocDatabase";

    //  table name
    private static final String TABLE_ATTENDANCE = "attendance";
    //  Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_DATETIME = "date_time";
    private static final String KEY_MARK = "att_mark";

    private static final String TABLE_WIFI = "wifi_details";
    //  Table Columns names
    private static final String KEY_SSID = "wifi_ssid";
    private static final String KEY_BSSID = "wifi_bssid";


    private static final String DDL_ATTENDANCE = "CREATE TABLE " + TABLE_ATTENDANCE + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + KEY_DATETIME + " DOUBLE NOT NULL, " + KEY_MARK + " BOOLEAN )";
    private static final String DDL_WIFI = "CREATE TABLE " + TABLE_WIFI + "(" + KEY_ID + " INTEGER PRIMARY KEY, " + KEY_SSID + " TEXT NOT NULL, " + KEY_BSSID + " TEXT NOT NULL )";


    Context context;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //3rd argument to be passed is CursorFactory instance
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DDL_WIFI);
        db.execSQL(DDL_ATTENDANCE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        // Create tables again
        onCreate(db);
    }

    public boolean saveWIFI(String ssid, String bssid) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(KEY_ID, 1);
            contentValues.put(KEY_SSID, ssid);
            contentValues.put(KEY_BSSID, bssid);
            db.insertWithOnConflict(TABLE_WIFI, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }

    }

    public boolean isWifiSaved() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return db.compileStatement("SELECT COUNT(*) FROM " + TABLE_WIFI).simpleQueryForLong() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
    }

    public String getWifiBSSID() {
        String bssid = "";
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            Cursor cursor = db.query(TABLE_WIFI, new String[]{KEY_BSSID}, null, null, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    bssid = cursor.getString(cursor.getColumnIndex(KEY_BSSID));
                }
                cursor.close();
            }
            return bssid;
        } catch (Exception e) {
            e.printStackTrace();
            return bssid;
        } finally {
            db.close();
        }
    }


    public void saveAttendance() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_DATETIME, System.currentTimeMillis());
        cv.put(KEY_MARK, true);
        try {
            db.insert(TABLE_ATTENDANCE, null, cv);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public long getAttendance() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            Cursor cursor = db.rawQuery("SELECT " + KEY_DATETIME + " FROM " + TABLE_ATTENDANCE + " WHERE " + KEY_MARK + " = 1 ORDER BY " + KEY_DATETIME + " DESC LIMIT 1", null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndex(KEY_DATETIME));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            db.close();
        }
        return 0L;
    }

}
