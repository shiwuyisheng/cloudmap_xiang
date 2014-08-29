package com.hxd0214.cloudmap.dao;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hxd0214.cloudmap.db.MyLocationSQLiteOpenHelper;
import com.hxd0214.cloudmap.domain.MyLocation;

public class MyLocationDao {       //Database access object
	private MyLocationSQLiteOpenHelper helper;

	// �ڹ��췽���������helper�ĳ�ʼ��
	public MyLocationDao(Context context) {
		helper = new MyLocationSQLiteOpenHelper(context);
	}

	// ���һ����¼�����ݿ�
	public void add(String name, String longitude, String latitude) {
		SQLiteDatabase db = helper.getWritableDatabase(); // д��������ʱ��ǵ���дclose��䣬������м�����
		db.execSQL("insert into mylocation (name, longitude, latitude) values (?,?,? )", new Object[] { name, longitude, latitude });
		db.close();
	}
	
	/**
	 * ��ѯ��¼�Ƿ����
	 * @param name
	 * @return true������ڣ�false��������
	 */
	public boolean find(String name){
		SQLiteDatabase db = helper.getReadableDatabase();   //��ѯʱ��getReadableDatabase
		Cursor cursor = db.rawQuery("select * from mylocation where name = ?", new String[]{name});     //Cursor������ν�Ľ����
		boolean result = cursor.moveToNext();   //�ж��Ƿ�����һ����¼
		cursor.close();   //�ͷ��α���Դ
		db.close();
		return result;
	}
	/**
	 * �޸�һ����¼
	 * @param name
	 * @param newLongitude �µľ���
	 * @param newLatitude  �µ�γ��
	 */
	public void update(String name, String newLongitude, String newLatitude){
		SQLiteDatabase db = helper.getWritableDatabase();
		db.execSQL("update mylocation set longitude = ?, latitude = ? where name = ?", new Object[]{newLongitude, newLatitude, name});
		db.close();
	}
	
	
	/**
	 * �޸ļ�¼��name�ֶ�
	 * @param name
	 * @param newName
	 */
	public void updateName(String name, String newName){
		SQLiteDatabase db = helper.getWritableDatabase();
		db.execSQL("update mylocation set name = ? where name = ?", new Object[]{newName, name});
		db.close();
	}
	
	
	
	/**
	 * ɾ��һ����¼
	 * @param name
	 */
	public void delete(String name){
		SQLiteDatabase db = helper.getWritableDatabase();
		db.execSQL("delete from mylocation where name = ?", new Object[]{name});
		db.close();
	}
	
	/**
	 * ����ȫ�������ݿ���Ϣ
	 * @return
	 */
	public List<MyLocation> findAll(){
		SQLiteDatabase db = helper.getReadableDatabase();
		List<MyLocation> myLocations = new ArrayList<MyLocation>();
		Cursor cursor = db.rawQuery("select * from mylocation", null); 
		while(cursor.moveToNext()){
			int id = cursor.getInt(cursor.getColumnIndex("id"));
			String name = cursor.getString(cursor.getColumnIndex("name"));
			String longitude = cursor.getString(cursor.getColumnIndex("longitude"));
			String latitude = cursor.getString(cursor.getColumnIndex("latitude"));
			MyLocation myLocation = new MyLocation(id, name, longitude, latitude);
			myLocations.add(myLocation);
		}
		cursor.close();
		db.close();
		return myLocations;
	}
	
}
