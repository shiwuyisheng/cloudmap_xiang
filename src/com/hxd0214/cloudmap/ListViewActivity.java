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
		// ������Ŀ�����¼�
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

		// ���������˵�
		lv.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				menu.setHeaderTitle("�˵�");
				menu.add(0, 0, 0, "�༭");
				menu.add(0, 1, 0, "ɾ��");

			}
		});

	}

	// ������Ŀ�˵��¼�
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final View layout;

		switch (item.getItemId()) {

		case 0:
			LayoutInflater inflater = getLayoutInflater();
			layout = inflater.inflate(R.layout.edit_name, null);
			new AlertDialog.Builder(this).setTitle("������������").setView(layout)
					.setPositiveButton("ȷ��", new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							EditText newNameText = (EditText) layout
									.findViewById(R.id.edit_name);
							String newName = newNameText.getText().toString();
							MyLocation myLocation01 = myLocations
									.get(menuInfo.position);
							dao.updateName(myLocation01.getName(), newName);
							// ���ݿ����ݺ�listviewͬ��ˢ���޷����������������´�listview���������
							Intent intent = new Intent();
							intent.setClass(getApplicationContext(),
									ListViewActivity.class);
							startActivity(intent);

							Toast.makeText(getApplicationContext(), "�޸ĳɹ�",
									Toast.LENGTH_SHORT).show();
						}
					}).setNegativeButton("ȡ��", null).show();
			MyLocation myLocation0 = myLocations.get(menuInfo.position);
			EditText newNameText = (EditText) layout
					.findViewById(R.id.edit_name);
			newNameText.setSelectAllOnFocus(true);
			newNameText.setText(myLocation0.getName());
			break;

		case 1:
			MyLocation myLocation1 = myLocations.get(menuInfo.position);
			dao.delete(myLocation1.getName());

			// ���ݿ����ݺ�listviewͬ��ˢ���޷����������������´�listview���������
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), ListViewActivity.class);
			startActivity(intent);

			Toast.makeText(getApplicationContext(), "ɾ���ɹ�", Toast.LENGTH_SHORT)
					.show();

			break;
		default:
			return super.onContextItemSelected(item);
		}
		return true;
	}

	private class MyAdapter extends BaseAdapter {

		/**
		 * ����listview�����ܹ��ж��ٸ���Ŀ
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
		 * �õ�listview��Ŀ�Ŀؼ�
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = new TextView(getApplicationContext());
			tv.setTextSize(30);

			MyLocation myLocation = myLocations.get(position); // �õ���λ�ö�Ӧ�Ķ���
			tv.setText(myLocation.getName());

			return tv;
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		this.finish(); // ��onPause()�����е���finish()���������ڴ���һ��activity��ʱ��ɱ���Լ�
	}

}