package com.hxd0214.cloudmap.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MyLocationSQLiteOpenHelper extends SQLiteOpenHelper {

	/**
	 * 数据库的构造方法 用来定义数据库的名称 数据库查询的结果集 数据库的版本
	 * @param context
	 */
	public MyLocationSQLiteOpenHelper(Context context) {
		super(context, "myLocation.db", null, 1);
		
	}

	/**
	 * 数据库第一次被创建时调用的方法
	 * @param db 被创建的数据库
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		//初始化数据库的表结构
		db.execSQL("create table mylocation (id integer primary key autoincrement, name varchar(20), longitude varchar(20), latitude varchar(20))");

	}

	/**
	 * 当数据库的版本号发生变化(增加)时候调用
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
