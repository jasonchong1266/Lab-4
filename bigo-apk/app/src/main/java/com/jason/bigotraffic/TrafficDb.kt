package com.jason.bigotraffic
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
data class TrafficSample(val ts:Long,val viewers:Int)
class TrafficDb(c:Context):SQLiteOpenHelper(c,"traffic.db",null,1){override fun onCreate(db:SQLiteDatabase){db.execSQL("CREATE TABLE samples(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, viewers INTEGER NOT NULL)")};override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int)=Unit;fun insert(v:Int,ts:Long=System.currentTimeMillis()){writableDatabase.insert("samples",null,ContentValues().apply{put("ts",ts);put("viewers",v)})};fun stats():Triple<Int?,Int?,Double?>{readableDatabase.rawQuery("SELECT COUNT(*),MAX(viewers),AVG(viewers) FROM samples",null).use{c->if(!c.moveToFirst()||c.getInt(0)==0)return Triple(null,null,null);return Triple(c.getInt(0),c.getInt(1),c.getDouble(2))}};fun latest():TrafficSample?=readableDatabase.rawQuery("SELECT ts,viewers FROM samples ORDER BY id DESC LIMIT 1",null).use{c->if(c.moveToFirst())TrafficSample(c.getLong(0),c.getInt(1))else null};fun all():List<TrafficSample>{val o=mutableListOf<TrafficSample>();readableDatabase.rawQuery("SELECT ts,viewers FROM samples ORDER BY ts ASC",null).use{c->while(c.moveToNext())o+=TrafficSample(c.getLong(0),c.getInt(1))};return o}}
