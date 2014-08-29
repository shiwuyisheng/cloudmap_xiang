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

	// 在构造方法里面完成helper的初始化
	public MyLocationDao(Context context) {
		helper = new MyLocationSQLiteOpenHelper(context);
	}

	// 添加一条记录到数据库
	public void add(String name, String longitude, String latitude) {
		SQLiteDatabase db = helper.getWritableDatabase(); // 写这条语句的时候记得先写close语句，再添加中间的语句
		db.execSQL("insert into mylocation (name, longitude, latitude) values (?,?,? )", new Object[] { name, longitude, latitude });
		db.close();
	}
	
	/**
	 * 查询记录是否存在
	 * @param name
	 * @return true代表存在，false代表不存在
	 */
	public boolean find(String name){
		SQLiteDatabase db = helper.getReadableDatabase();   //查询时，getReadableDatabase
		Cursor cursor = db.rawQuery("select * from mylocation where name = ?", new String[]{name});     //Cursor就是所谓的结果集
		boolean result = cursor.moveToNext();   //判断是否有下一条记录
		cursor.close();   //释放游标资源
		db.close();
		return result;
	}
	/**
	 * 修改一条记录
	 * @param name
	 * @param newLongitude 新的经度
	 * @param newLatitude  新的纬度
	 */
	public void update(String name, String newLongitude, String newLatitude){
		SQLiteDatabase db = helper.getWritableDatabase();
		db.execSQL("update mylocation set longitude = ?, latitude = ? where name = ?", new Object[]{newLongitude, newLatitude, name});
		db.close();
	}
	
	
	/**
	 * 修改记录的name字段
	 * @param name
	 * @param newName
	 */
	public void updateName(String name, String newName){
		SQLiteDatabase db = helper.getWritableDatabase();
		db.execSQL("update mylocation set name = ? where name = ?", new Object[]{newName, name});
		db.close();
	}
	
	
	
	/**
	 * 删除一条记录
	 * @param name
	 */
	public void delete(String name){
		SQLiteDatabase db = helper.getWritableDatabase();
		db.execSQL("delete from mylocation where name = ?", new Object[]{name});
		db.close();
	}
	
	/**
	 * 返回全部的数据库信息
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
