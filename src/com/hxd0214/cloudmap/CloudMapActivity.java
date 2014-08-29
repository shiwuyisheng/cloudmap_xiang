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
 * 单乡镇版本
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
	private TextView lonview; // 用来显示经度坐标
	private TextView latview; // 用来显示纬度坐标
	private MyLocation myLoaction;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		map = (MapView) findViewById(R.id.map);
		lonview = (TextView) findViewById(R.id.lonview);
		latview = (TextView) findViewById(R.id.latview);

		// 导入所有图层
		importLayers();

		// 隐藏次要图层并清理所有临时要素
		clearAll();

		// 初始化显示范围
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

		// 加载谷歌底图
		String path1 = tpkPath + "google.tpk";
		googleLayer = new ArcGISLocalTiledLayer(path1);
		map.addLayer(googleLayer);
		// 加载地类界限
		String path2 = tpkPath + "dljx.tpk";
		dljxLayer = new ArcGISLocalTiledLayer(path2);
		map.addLayer(dljxLayer);
		// 加载地类图斑
		String path3 = tpkPath + "dltb.tpk";
		dltbLayer = new ArcGISLocalTiledLayer(path3);
		map.addLayer(dltbLayer);
		// 加载基本农田
		String path4 = tpkPath + "jbnt.tpk";
		jbntLayer = new ArcGISLocalTiledLayer(path4);
		map.addLayer(jbntLayer);
		// 加载图块
		String path5 = tpkPath + "tk.tpk";
		tkLayer = new ArcGISLocalTiledLayer(path5);
		map.addLayer(tkLayer);
		// 加载GraphicsLayer图层
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
		Point mapPoint = new Point(12531734.998871675, 4227613.322813992); // 电厂经纬度坐标
		Envelope zoomExtent = new Envelope(mapPoint, 5000, 5000);
		map.setExtent(zoomExtent);
	}

	class MapTouchListener extends MapOnTouchListener {
		private Geometry.Type geoType = Geometry.Type.POINT;// 用于判定当前选择的几何图形类型
		private Point ptStart = null;// 起点
		private Point ptPrevious = null;// 上一个点
		private ArrayList<Point> points = null;// 记录全部点
		private Polygon tempPolygon = null;// 记录绘制过程中的多边形

		private SimpleMarkerSymbol markerSymbol;
		private SimpleLineSymbol lineSymbol;
		private SimpleFillSymbol fillSymbol;

		public MapTouchListener(Context context, MapView view) {
			super(context, view);

			points = new ArrayList<Point>();

			// 初始化符号
			markerSymbol = new SimpleMarkerSymbol(Color.BLUE, 8,
					SimpleMarkerSymbol.STYLE.CIRCLE);
			lineSymbol = new SimpleLineSymbol(Color.RED, 2,
					SimpleLineSymbol.STYLE.DASH);
			fillSymbol = new SimpleFillSymbol(Color.RED);
			fillSymbol.setAlpha(33);
		}

		// 根据用户选择设置当前绘制的几何图形类型
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
				graphicsLayer.removeAll();// 第一次开始前，清空全部graphic

			if (geoType == Geometry.Type.POINT) { // 直接画点,并显示经纬度
				graphicsLayer.removeAll();

				Graphic graphic = new Graphic(ptCurrent, markerSymbol);
				graphicsLayer.addGraphic(graphic);

				// Point wgspoint = (Point) GeometryEngine.project(ptCurrent,
				// map.getSpatialReference(),
				// SpatialReference.create(4326));
				// Toast.makeText(
				// map.getContext(),
				// "经度：" + wgspoint.getX() + "\n" + "纬度："
				// + wgspoint.getY(), Toast.LENGTH_SHORT).show();
				lonview.setText(Math.round(ptCurrent.getX() * 1000000)
						/ 1000000.0 + "");
				latview.setText(Math.round(ptCurrent.getY() * 1000000)
						/ 1000000.0 + "");
				return true;
			} else// 绘制线或多边形
			{
				points.add(ptCurrent);// 将当前点加入点集合中

				if (ptStart == null) {// 画线或多边形的第一个点
					ptStart = ptCurrent;
					// 绘制第一个点
					Graphic graphic = new Graphic(ptStart, markerSymbol);
					graphicsLayer.addGraphic(graphic);
				} else {// 画线或多边形的其他点
						// 绘制其他点
					Graphic graphic = new Graphic(ptCurrent, markerSymbol);
					graphicsLayer.addGraphic(graphic);

					// 生成当前线段（由当前点和上一个点构成）
					Line line = new Line();
					line.setStart(ptPrevious);
					line.setEnd(ptCurrent);

					if (geoType == Geometry.Type.POLYLINE) {
						// 绘制当前线段
						Polyline polyline = new Polyline();
						polyline.addSegment(line, true);

						Graphic g = new Graphic(polyline, lineSymbol);
						graphicsLayer.addGraphic(g);

						// 计算当前线段的长度
						// String length = Double.toString(Math.round(line
						// .calculateLength2D())) + " 米";

						// Toast.makeText(mMapView.getContext(), length,
						// Toast.LENGTH_SHORT).show();
					} else {
						// 绘制临时多边形
						if (tempPolygon == null)
							tempPolygon = new Polygon();
						tempPolygon.addSegment(line, false);

						graphicsLayer.removeAll();
						Graphic g = new Graphic(tempPolygon, fillSymbol);
						graphicsLayer.addGraphic(g);

						// 计算当前面积
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

				// 绘制完整的线段
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

				// 计算总长度
				String length = Double.toString(Math.round(polyline
						.calculateLength2D()) / 1.25) + " 米"; // 这里的1.25是自己定义的修正参数

				Toast.makeText(map.getContext(), length, Toast.LENGTH_SHORT)
						.show();
			} else if (geoType == Geometry.Type.POLYGON) {
				Polygon polygon = new Polygon();

				Point startPoint = null;
				Point endPoint = null;
				// 绘制完整的多边形
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

				// 计算总面积
				String sArea = getAreaString(polygon.calculateArea2D() / 1.56); // 这里的1.56是自己定义的修正参数

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
			// 顺时针绘制多边形，面积为正，逆时针绘制，则面积为负
			if (area >= 1000000) {
				double dArea = area / 1000000.0;
				sArea = Double.toString(dArea) + " 平方公里";
			} else
				sArea = Double.toString(area) + " 平方米";

			return sArea;
		}

	}

	public void measureClear(MapTouchListener ml) {
		// 清理测量历史数据
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

		case R.id.saveLoc: // 保存点位
			extent = map.getExtent();

			// 获取对话框布局文件
			LayoutInflater layoutInflater = LayoutInflater
					.from(CloudMapActivity.this);
			final View dilogView = layoutInflater
					.inflate(R.layout.dialog, null);
			// 创建对话框
			AlertDialog.Builder builder = new AlertDialog.Builder(
					CloudMapActivity.this);
			builder.setTitle("保存点位置");
			builder.setView(dilogView);
			builder.setPositiveButton("确定",
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
							Toast.makeText(getApplicationContext(), "保存成功",
									Toast.LENGTH_SHORT).show();

						}
					});
			builder.setNegativeButton("取消", null);
			AlertDialog dialog = builder.create();
			dialog.show();

			// 设置自动填充地图单击点的经纬度值
			EditText longitudeText = (EditText) dilogView
					.findViewById(R.id.longitude);
			longitudeText.setText(lonview.getText());
			EditText latitudeText = (EditText) dilogView
					.findViewById(R.id.latitude);
			latitudeText.setText(latview.getText());
			// 设置按钮初始状态
			final Button positiveButton = dialog
					.getButton(AlertDialog.BUTTON_POSITIVE);
			if ((longitudeText != null && longitudeText.getText().toString()
					.equals(""))
					|| (latitudeText != null && latitudeText.getText()
							.toString().equals(""))) {
				positiveButton.setEnabled(false);
			}

			return true;

		case R.id.searchLoc: // 查找点位
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

		case R.id.measureLength: // “测量距离”菜单
			extent = map.getExtent();

			measureClear(mapTouchListener);
			mapTouchListener.setType("Polyline");
			return true;
		case R.id.measureArea: // “测量面积”菜单
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
			if (locationService != null) { // 非首次点击定位菜单时，放大到当前点
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
						map.zoomTo(mapPoint, 30); // 地图放大
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

		case R.id.backupDatabase: // 备份数据库菜单

			AlertDialog.Builder bdBuilder = new Builder(this);
			bdBuilder.setMessage("确认执行该操作么？");
			bdBuilder.setTitle("提示");
			bdBuilder.setPositiveButton("确认", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new BackupTask(getApplicationContext())
							.execute("backupDatabase");
					Toast.makeText(getApplicationContext(), "备份完成", 0).show();
				}
			});
			bdBuilder.setNegativeButton("取消", null);
			bdBuilder.create().show();

			return true;

		case R.id.restroeDatabase: // 恢复数据库菜单

			AlertDialog.Builder rdBuilder = new Builder(this);
			rdBuilder.setMessage("该操作会丢失备份点之后的所有数据，继续执行么？");
			rdBuilder.setTitle("提示");
			rdBuilder.setPositiveButton("确认", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new BackupTask(getApplicationContext())
							.execute("restroeDatabase");
					Toast.makeText(getApplicationContext(), "恢复完成", 0).show();
				}
			});
			rdBuilder.setNegativeButton("取消", null);
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