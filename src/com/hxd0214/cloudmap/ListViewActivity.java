package com.hxd0214.cloudmap;

import java.util.List;

import com.hxd0214.cloudmap.dao.MyLocationDao;
import com.hxd0214.cloudmap.domain.MyLocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ListViewActivity extends Activity {
	private ListView lv;
	private List<MyLocation> myLocations;
	private MyLocationDao dao;
	private MyAdapter adapter;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.listview);

		dao = new MyLocationDao(getApplicationContext());
		myLocations = dao.findAll();

		lv = (ListView) findViewById(R.id.lv);
		adapter = new MyAdapter();
		lv.setAdapter(adapter);
		// 设置条目单击事件
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				MyLocation myLocation = myLocations.get(position);
				double longitude = Double.valueOf(myLocation.getLongitude());
				double latitude = Double.valueOf(myLocation.getLatitude());
				double[] loc = new double[] { longitude, latitude };

				Bundle bundle = new Bundle();
				bundle.putDoubleArray("location", loc);
				Intent intent = new Intent(ListViewActivity.this,
						CloudMapActivity.class);
				intent.putExtras(bundle);
				startActivity(intent);

			}
		});

		// 长按弹出菜单
		lv.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				menu.setHeaderTitle("菜单");
				menu.add(0, 0, 0, "编辑");
				menu.add(0, 1, 0, "删除");

			}
		});

	}

	// 长按条目菜单事件
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final View layout;

		switch (item.getItemId()) {

		case 0:
			LayoutInflater inflater = getLayoutInflater();
			layout = inflater.inflate(R.layout.edit_name, null);
			new AlertDialog.Builder(this).setTitle("请输入新名称").setView(layout)
					.setPositiveButton("确定", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							EditText newNameText = (EditText) layout
									.findViewById(R.id.edit_name);
							String newName = newNameText.getText().toString();
							MyLocation myLocation01 = myLocations
									.get(menuInfo.position);
							dao.updateName(myLocation01.getName(), newName);
							// 数据库数据和listview同步刷新无法解决，这里采用重新打开listview窗口来解决
							Intent intent = new Intent();
							intent.setClass(getApplicationContext(),
									ListViewActivity.class);
							startActivity(intent);

							Toast.makeText(getApplicationContext(), "修改成功",
									Toast.LENGTH_SHORT).show();
						}
					}).setNegativeButton("取消", null).show();
			MyLocation myLocation0 = myLocations.get(menuInfo.position);
			EditText newNameText = (EditText) layout
					.findViewById(R.id.edit_name);
			newNameText.setSelectAllOnFocus(true);
			newNameText.setText(myLocation0.getName());
			break;

		case 1:
			MyLocation myLocation1 = myLocations.get(menuInfo.position);
			dao.delete(myLocation1.getName());

			// 数据库数据和listview同步刷新无法解决，这里采用重新打开listview窗口来解决
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), ListViewActivity.class);
			startActivity(intent);

			Toast.makeText(getApplicationContext(), "删除成功", Toast.LENGTH_SHORT)
					.show();

			break;
		default:
			return super.onContextItemSelected(item);
		}
		return true;
	}

	private class MyAdapter extends BaseAdapter {

		/**
		 * 控制listview里面总共有多少个条目
		 */
		public int getCount() {
			return myLocations.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		/**
		 * 得到listview条目的控件
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = new TextView(getApplicationContext());
			tv.setTextSize(30);

			MyLocation myLocation = myLocations.get(position); // 得到该位置对应的对象
			tv.setText(myLocation.getName());

			return tv;
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		this.finish(); // 在onPause()方法中调用finish()方法，会在打开另一个activity的时候，杀死自己
	}

}