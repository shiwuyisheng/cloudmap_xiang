package com.hxd0214.cloudmap;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.LocationService;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Line;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.hxd0214.cloudmap.dao.MyLocationDao;
import com.hxd0214.cloudmap.domain.MyLocation;

/*
 * ������汾
 */
public class CloudMapActivity extends Activity {
	MapView map;
	String rootDir;
	String tpkPath;
	Polygon extent; // used to maintain extent when switching basemaps
	private GraphicsLayer graphicsLayer;
	private MapTouchListener mapTouchListener;
	private Layer[] layers;
	private LocationService locationService = null;
	final static double SEARCH_RADIUS = 5;
	private ArcGISLocalTiledLayer googleLayer;
	private ArcGISLocalTiledLayer dljxLayer;
	private ArcGISLocalTiledLayer dltbLayer;
	private ArcGISLocalTiledLayer tkLayer;
	private ArcGISLocalTiledLayer jbntLayer;
	private TextView lonview; // ������ʾ��������
	private TextView latview; // ������ʾγ������
	private MyLocation myLoaction;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		map = (MapView) findViewById(R.id.map);
		lonview = (TextView) findViewById(R.id.lonview);
		latview = (TextView) findViewById(R.id.latview);

		// ��������ͼ��
		importLayers();

		// ���ش�Ҫͼ�㲢����������ʱҪ��
		clearAll();

		// ��ʼ����ʾ��Χ
		initExtent();

		mapTouchListener = new MapTouchListener(getApplicationContext(), map);
		map.setOnTouchListener(mapTouchListener);

		// honor the extent when switching basemaps
		map.setOnStatusChangedListener(new OnStatusChangedListener() {
			private static final long serialVersionUID = 1L;

			public void onStatusChanged(Object source, STATUS status) {
				map.setExtent(extent);
			}
		});
	}

	private void importLayers() {
		rootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		tpkPath = "file://" + rootDir + File.separator + "arcgis"
				+ File.separator;

		// ���عȸ��ͼ
		String path1 = tpkPath + "google.tpk";
		googleLayer = new ArcGISLocalTiledLayer(path1);
		map.addLayer(googleLayer);
		// ���ص������
		String path2 = tpkPath + "dljx.tpk";
		dljxLayer = new ArcGISLocalTiledLayer(path2);
		map.addLayer(dljxLayer);
		// ���ص���ͼ��
		String path3 = tpkPath + "dltb.tpk";
		dltbLayer = new ArcGISLocalTiledLayer(path3);
		map.addLayer(dltbLayer);
		// ���ػ���ũ��
		String path4 = tpkPath + "jbnt.tpk";
		jbntLayer = new ArcGISLocalTiledLayer(path4);
		map.addLayer(jbntLayer);
		// ����ͼ��
		String path5 = tpkPath + "tk.tpk";
		tkLayer = new ArcGISLocalTiledLayer(path5);
		map.addLayer(tkLayer);
		// ����GraphicsLayerͼ��
		graphicsLayer = new GraphicsLayer();
		map.addLayer(graphicsLayer);

		layers = map.getLayers();
	}

	private void clearAll() {
		if (mapTouchListener != null) {
			measureClear(mapTouchListener);
			mapTouchListener.geoType = Geometry.Type.POINT;
		}
		graphicsLayer.removeAll();
		for (int i = 0; i < layers.length - 1; i++) {
			if (i != 0 && i != 1) {
				layers[i].setVisible(false);
			}
		}

	}

	private void initExtent() {
		Point mapPoint = new Point(12531734.998871675, 4227613.322813992); // �糧��γ������
		Envelope zoomExtent = new Envelope(mapPoint, 5000, 5000);
		map.setExtent(zoomExtent);
	}

	class MapTouchListener extends MapOnTouchListener {
		private Geometry.Type geoType = Geometry.Type.POINT;// �����ж���ǰѡ��ļ���ͼ������
		private Point ptStart = null;// ���
		private Point ptPrevious = null;// ��һ����
		private ArrayList<Point> points = null;// ��¼ȫ����
		private Polygon tempPolygon = null;// ��¼���ƹ����еĶ����

		private SimpleMarkerSymbol markerSymbol;
		private SimpleLineSymbol lineSymbol;
		private SimpleFillSymbol fillSymbol;

		public MapTouchListener(Context context, MapView view) {
			super(context, view);

			points = new ArrayList<Point>();

			// ��ʼ������
			markerSymbol = new SimpleMarkerSymbol(Color.BLUE, 8,
					SimpleMarkerSymbol.STYLE.CIRCLE);
			lineSymbol = new SimpleLineSymbol(Color.RED, 2,
					SimpleLineSymbol.STYLE.DASH);
			fillSymbol = new SimpleFillSymbol(Color.RED);
			fillSymbol.setAlpha(33);
		}

		// �����û�ѡ�����õ�ǰ���Ƶļ���ͼ������
		public void setType(String geometryType) {
			if (geometryType.equalsIgnoreCase("Point"))
				this.geoType = Geometry.Type.POINT;
			else if (geometryType.equalsIgnoreCase("Polyline"))
				this.geoType = Geometry.Type.POLYLINE;
			else if (geometryType.equalsIgnoreCase("Polygon"))
				this.geoType = Geometry.Type.POLYGON;
		}

		@Override
		public boolean onSingleTap(MotionEvent point) {
			Point ptCurrent = map.toMapPoint(new Point(point.getX(), point
					.getY()));
			if (ptStart == null)
				graphicsLayer.removeAll();// ��һ�ο�ʼǰ�����ȫ��graphic

			if (geoType == Geometry.Type.POINT) { // ֱ�ӻ���,����ʾ��γ��
				graphicsLayer.removeAll();

				Graphic graphic = new Graphic(ptCurrent, markerSymbol);
				graphicsLayer.addGraphic(graphic);

				// Point wgspoint = (Point) GeometryEngine.project(ptCurrent,
				// map.getSpatialReference(),
				// SpatialReference.create(4326));
				// Toast.makeText(
				// map.getContext(),
				// "���ȣ�" + wgspoint.getX() + "\n" + "γ�ȣ�"
				// + wgspoint.getY(), Toast.LENGTH_SHORT).show();
				lonview.setText(Math.round(ptCurrent.getX() * 1000000)
						/ 1000000.0 + "");
				latview.setText(Math.round(ptCurrent.getY() * 1000000)
						/ 1000000.0 + "");
				return true;
			} else// �����߻�����
			{
				points.add(ptCurrent);// ����ǰ�����㼯����

				if (ptStart == null) {// ���߻����εĵ�һ����
					ptStart = ptCurrent;
					// ���Ƶ�һ����
					Graphic graphic = new Graphic(ptStart, markerSymbol);
					graphicsLayer.addGraphic(graphic);
				} else {// ���߻����ε�������
						// ����������
					Graphic graphic = new Graphic(ptCurrent, markerSymbol);
					graphicsLayer.addGraphic(graphic);

					// ���ɵ�ǰ�߶Σ��ɵ�ǰ�����һ���㹹�ɣ�
					Line line = new Line();
					line.setStart(ptPrevious);
					line.setEnd(ptCurrent);

					if (geoType == Geometry.Type.POLYLINE) {
						// ���Ƶ�ǰ�߶�
						Polyline polyline = new Polyline();
						polyline.addSegment(line, true);

						Graphic g = new Graphic(polyline, lineSymbol);
						graphicsLayer.addGraphic(g);

						// ���㵱ǰ�߶εĳ���
						// String length = Double.toString(Math.round(line
						// .calculateLength2D())) + " ��";

						// Toast.makeText(mMapView.getContext(), length,
						// Toast.LENGTH_SHORT).show();
					} else {
						// ������ʱ�����
						if (tempPolygon == null)
							tempPolygon = new Polygon();
						tempPolygon.addSegment(line, false);

						graphicsLayer.removeAll();
						Graphic g = new Graphic(tempPolygon, fillSymbol);
						graphicsLayer.addGraphic(g);

						// ���㵱ǰ���
						// String sArea = getAreaString(tempPolygon
						// .calculateArea2D());
						// Toast.makeText(mMapView.getContext(), sArea,
						// Toast.LENGTH_SHORT).show();
					}
				}

				ptPrevious = ptCurrent;
				return true;
			}
		}

		@Override
		public void onLongPress(MotionEvent point) {
			graphicsLayer.removeAll();
			if (geoType == Geometry.Type.POLYLINE) {
				Polyline polyline = new Polyline();

				Point startPoint = null;
				Point endPoint = null;

				// �����������߶�
				for (int i = 1; i < points.size(); i++) {
					startPoint = points.get(i - 1);
					endPoint = points.get(i);

					Line line = new Line();
					line.setStart(startPoint);
					line.setEnd(endPoint);

					polyline.addSegment(line, false);
				}

				Graphic g = new Graphic(polyline, lineSymbol);
				graphicsLayer.addGraphic(g);

				// �����ܳ���
				String length = Double.toString(Math.round(polyline
						.calculateLength2D()) / 1.25) + " ��"; // �����1.25���Լ��������������

				Toast.makeText(map.getContext(), length, Toast.LENGTH_SHORT)
						.show();
			} else if (geoType == Geometry.Type.POLYGON) {
				Polygon polygon = new Polygon();

				Point startPoint = null;
				Point endPoint = null;
				// ���������Ķ����
				for (int i = 1; i < points.size(); i++) {
					startPoint = points.get(i - 1);
					endPoint = points.get(i);

					Line line = new Line();
					line.setStart(startPoint);
					line.setEnd(endPoint);

					polygon.addSegment(line, false);
				}

				Graphic g = new Graphic(polygon, fillSymbol);
				graphicsLayer.addGraphic(g);

				// ���������
				String sArea = getAreaString(polygon.calculateArea2D() / 1.56); // �����1.56���Լ��������������

				Toast.makeText(map.getContext(), sArea, Toast.LENGTH_SHORT)
						.show();
			}
			measureClear(this);
			geoType = Geometry.Type.POINT;
			if (locationService != null) {
				locationService.setAutoPan(false);
			}
		}

		private String getAreaString(double dValue) {
			long area = Math.abs(Math.round(dValue));
			String sArea = "";
			// ˳ʱ����ƶ���Σ����Ϊ������ʱ����ƣ������Ϊ��
			if (area >= 1000000) {
				double dArea = area / 1000000.0;
				sArea = Double.toString(dArea) + " ƽ������";
			} else
				sArea = Double.toString(area) + " ƽ����";

			return sArea;
		}

	}

	public void measureClear(MapTouchListener ml) {
		// ���������ʷ����
		ml.ptStart = null;
		ml.ptPrevious = null;
		ml.points.clear();
		ml.tempPolygon = null;
		lonview.setText(null);
		latview.setText(null);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_cloudmap, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.saveLoc: // �����λ
			extent = map.getExtent();

			// ��ȡ�Ի��򲼾��ļ�
			LayoutInflater layoutInflater = LayoutInflater
					.from(CloudMapActivity.this);
			final View dilogView = layoutInflater
					.inflate(R.layout.dialog, null);
			// �����Ի���
			AlertDialog.Builder builder = new AlertDialog.Builder(
					CloudMapActivity.this);
			builder.setTitle("�����λ��");
			builder.setView(dilogView);
			builder.setPositiveButton("ȷ��",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							EditText nameText = (EditText) dilogView
									.findViewById(R.id.name);
							String name = nameText.getText().toString();

							EditText longitudeText = (EditText) dilogView
									.findViewById(R.id.longitude);
							String longitude = longitudeText.getText()
									.toString();

							EditText latitudeText = (EditText) dilogView
									.findViewById(R.id.latitude);
							String latitude = latitudeText.getText().toString();

							MyLocationDao dao = new MyLocationDao(
									getApplicationContext());
							dao.add(name, longitude, latitude);
							Toast.makeText(getApplicationContext(), "����ɹ�",
									Toast.LENGTH_SHORT).show();

						}
					});
			builder.setNegativeButton("ȡ��", null);
			AlertDialog dialog = builder.create();
			dialog.show();

			// �����Զ�����ͼ������ľ�γ��ֵ
			EditText longitudeText = (EditText) dilogView
					.findViewById(R.id.longitude);
			longitudeText.setText(lonview.getText());
			EditText latitudeText = (EditText) dilogView
					.findViewById(R.id.latitude);
			latitudeText.setText(latview.getText());
			// ���ð�ť��ʼ״̬
			final Button positiveButton = dialog
					.getButton(AlertDialog.BUTTON_POSITIVE);
			if ((longitudeText != null && longitudeText.getText().toString()
					.equals(""))
					|| (latitudeText != null && latitudeText.getText()
							.toString().equals(""))) {
				positiveButton.setEnabled(false);
			}

			return true;

		case R.id.searchLoc: // ���ҵ�λ
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), ListViewActivity.class);
			startActivity(intent);

			return true;

		case R.id.tk:
			extent = map.getExtent();

			if (map.isLoaded()) {
				if (tkLayer.isVisible()) {
					tkLayer.setVisible(false);
				} else {
					tkLayer.setVisible(true);
				}
			}
			return true;

		case R.id.measureLength: // ���������롱�˵�
			extent = map.getExtent();

			measureClear(mapTouchListener);
			mapTouchListener.setType("Polyline");
			return true;
		case R.id.measureArea: // ������������˵�
			extent = map.getExtent();

			measureClear(mapTouchListener);
			mapTouchListener.setType("Polygon");
			return true;
		case R.id.clear:
			extent = map.getExtent();

			clearAll();
			return true;
		case R.id.jbnt:
			extent = map.getExtent();

			if (map.isLoaded()) {
				if (jbntLayer.isVisible()) {
					jbntLayer.setVisible(false);
				} else {
					jbntLayer.setVisible(true);
				}
			}
			return true;

		case R.id.location:
			if (locationService != null) { // ���״ε����λ�˵�ʱ���Ŵ󵽵�ǰ��
				locationService.setAutoPan(true);

				Location loc = locationService.getLocation();
				double locy = loc.getLatitude();
				double locx = loc.getLongitude();
				Point wgspoint = new Point(locx, locy);
				Point mapPoint = (Point) GeometryEngine.project(wgspoint,
						SpatialReference.create(4326),
						map.getSpatialReference());
				map.zoomTo(mapPoint, 30);
				return true;
			}
			locationService = map.getLocationService();
			locationService.setLocationListener(new LocationListener() {
				boolean locationChanged = false;

				public void onLocationChanged(Location loc) {
					if (!locationChanged) {
						locationChanged = true;
						double locy = loc.getLatitude();
						double locx = loc.getLongitude();
						Point wgspoint = new Point(locx, locy);
						Point mapPoint = (Point) GeometryEngine.project(
								wgspoint, SpatialReference.create(4326),
								map.getSpatialReference());
						// Unit mapUnit = map.getSpatialReference().getUnit();
						// double zoomWidth = Unit.convertUnits(SEARCH_RADIUS,
						// Unit.create(LinearUnit.Code.MILE_US), mapUnit);
						// Envelope zoomExtent = new Envelope(mapPoint,
						// zoomWidth,
						// zoomWidth);
						// map.setExtent(zoomExtent);
						map.zoomTo(mapPoint, 30); // ��ͼ�Ŵ�
					}
				}

				public void onProviderDisabled(String arg0) {
				}

				public void onProviderEnabled(String arg0) {
				}

				public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
				}
			});
			locationService.start();
			return true;

		case R.id.dltb:
			extent = map.getExtent();

			if (map.isLoaded()) {
				if (dltbLayer.isVisible()) {
					dltbLayer.setVisible(false);
				} else {
					dltbLayer.setVisible(true);
				}
			}
			return true;

		case R.id.backupDatabase: // �������ݿ�˵�

			AlertDialog.Builder bdBuilder = new Builder(this);
			bdBuilder.setMessage("ȷ��ִ�иò���ô��");
			bdBuilder.setTitle("��ʾ");
			bdBuilder.setPositiveButton("ȷ��", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new BackupTask(getApplicationContext())
							.execute("backupDatabase");
					Toast.makeText(getApplicationContext(), "�������", 0).show();
				}
			});
			bdBuilder.setNegativeButton("ȡ��", null);
			bdBuilder.create().show();

			return true;

		case R.id.restroeDatabase: // �ָ����ݿ�˵�

			AlertDialog.Builder rdBuilder = new Builder(this);
			rdBuilder.setMessage("�ò����ᶪʧ���ݵ�֮����������ݣ�����ִ��ô��");
			rdBuilder.setTitle("��ʾ");
			rdBuilder.setPositiveButton("ȷ��", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new BackupTask(getApplicationContext())
							.execute("restroeDatabase");
					Toast.makeText(getApplicationContext(), "�ָ����", 0).show();
				}
			});
			rdBuilder.setNegativeButton("ȡ��", null);
			rdBuilder.create().show();

			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		map.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		map.unpause();

		if (getIntent().getExtras() != null) {
			double[] loc = getIntent().getExtras().getDoubleArray("location");
			double locx = loc[0];
			double locy = loc[1];

			Point mapPoint = new Point(locx, locy);
			SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol(Color.RED,
					8, SimpleMarkerSymbol.STYLE.SQUARE);
			Graphic graphic = new Graphic(mapPoint, markerSymbol);
			graphicsLayer.addGraphic(graphic);

			map.zoomTo(mapPoint, 90000000);

		}

	}

}