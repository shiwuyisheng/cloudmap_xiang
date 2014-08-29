package com.hxd0214.cloudmap.domain;

public class MyLocation {
	private int id;
	private String name;
	private String longitude;
	private String latitude;

	public MyLocation() {
	}

	public MyLocation(int id, String name, String longitude, String latitude) {
		this.id = id;
		this.name = name;
		this.longitude = longitude;
		this.latitude = latitude;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	@Override
	public String toString() {
		return "MyLocation [id=" + id + ", name=" + name + ", longitude="
				+ longitude + ", latitude=" + latitude + "]";
	}

	
}
