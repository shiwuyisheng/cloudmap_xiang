package com.hxd0214.cloudmap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class BackupTask extends AsyncTask<String, Void, Integer> {
	private static final String COMMAND_BACKUP = "backupDatabase";
	public static final String COMMAND_RESTORE = "restroeDatabase";
	private Context mContext;

	public BackupTask(Context context) {
		this.mContext = context;
	}

	@Override
	protected Integer doInBackground(String... params) {
		// TODO Auto-generated method stub

		// �������ʹ�õ����ݿ�·��,Ĭ��·���� /data/data/(����)/databases/*.db
		File dbFile = mContext.getDatabasePath("/data/data/com.hxd0214.cloudmap/databases/myLocation.db");
		File exportDir = new File(Environment.getExternalStorageDirectory(),
				"myLocationBackup");
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		File backup = new File(exportDir, dbFile.getName());
		String command = params[0];
		if (command.equals(COMMAND_BACKUP)) {
			try {
				backup.createNewFile();
				fileCopy(dbFile, backup);
//				Toast.makeText(mContext, "���ݳɹ�", 0).show();      //��AsyncTask��ʹ��Toast�ᱨ��
				return Log.d("backup", "ok");
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
//				Toast.makeText(mContext, "����ʧ��", 0).show();
				return Log.d("backup", "fail");
			}
		} else if (command.equals(COMMAND_RESTORE)) {
			try {
				fileCopy(backup, dbFile);
//				Toast.makeText(mContext, "�ָ��ɹ�", 0).show();
				return Log.d("restore", "success");
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
//				Toast.makeText(mContext, "�ָ�ʧ��", 0).show();
				return Log.d("restore", "fail");
			}
		} else {
			return null;
		}
	}

	private void fileCopy(File dbFile, File backup) throws IOException {
		// TODO Auto-generated method stub
		FileChannel inChannel = new FileInputStream(dbFile).getChannel();
		FileChannel outChannel = new FileOutputStream(backup).getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (inChannel != null) {
				inChannel.close();
			}
			if (outChannel != null) {
				outChannel.close();
			}
		}
	}
}
