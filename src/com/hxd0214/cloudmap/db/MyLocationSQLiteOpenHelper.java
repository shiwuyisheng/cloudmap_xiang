package com.hxd0214.cloudmap.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MyLocationSQLiteOpenHelper extends SQLiteOpenHelper {

	/**
	 * ���ݿ�Ĺ��췽�� �����������ݿ������ ���ݿ��ѯ�Ľ���� ���ݿ�İ汾
	 * @param context
	 */
	public MyLocationSQLiteOpenHelper(Context context) {
		super(context, "myLocation.db", null, 1);
		
	}

	/**
	 * ���ݿ��һ�α�����ʱ���õķ���
	 * @param db �����������ݿ�
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		//��ʼ�����ݿ�ı�ṹ
		db.execSQL("create table mylocation (id integer primary key autoincrement, name varchar(20), longitude varchar(20), latitude varchar(20))");

	}

	/**
	 * �����ݿ�İ汾�ŷ����仯(����)ʱ�����
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
