package com.wenzhiming.sms520.util;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;

public class SmsUtils {

	private static final String TAG = "SmsUtils";

	public static void printCursor(Cursor cursor) {
		if(cursor != null && cursor.getCount() > 0) {
			
			int columnCount;
			String columnName;
			String columnValue;
			
			while(cursor.moveToNext()) {
				columnCount = cursor.getColumnCount();
				
				for (int i = 0; i < columnCount; i++) {
					columnName = cursor.getColumnName(i);
					columnValue = cursor.getString(i);
					Log.i(TAG, "��ǰ�ǵ�" + cursor.getPosition() + "��: " + columnName + " = " + columnValue);
				}
			}
			cursor.moveToPosition(-1);
//			cursor.close();
		}
	}
	
	public static String getContactName(ContentResolver resolver, String address) {
		// content://com.android.contacts/phone_lookup/95555
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address));
		Cursor cursor = resolver.query(uri, new String[]{Phone.DISPLAY_NAME}, null, null, null);
		
		if(cursor != null && cursor.moveToFirst()) {
			String contactName = cursor.getString(0);
			cursor.close();
			return contactName;
		}
		return null;
	}
	
	public static Bitmap getContactIcon(ContentResolver resolver, String address) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address));
		Cursor cursor = resolver.query(uri, new String[]{Phone._ID}, null, null, null);
		if(cursor != null && cursor.moveToFirst()) {
			long contactID = cursor.getLong(0);
			cursor.close();
			
			uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactID);
			InputStream is = Contacts.openContactPhotoInputStream(resolver, uri);
			
			Bitmap contactIcon = BitmapFactory.decodeStream(is);
			return contactIcon;
		}
		return null;
	}
	
	public static void sendMessage(Context context, String address, String content) {
		android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
		
		ArrayList<String> divideMessage = smsManager.divideMessage(content);
		
		Intent intent = new Intent("com.wenzhiming.sms520.receive.SendMessageBroadcastReceive");
		PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		
		for (String text : divideMessage) {
			smsManager.sendTextMessage(
					address,
					null,
					text, 
					sentIntent,
					null);
		}
		
		writeMessage(context, address, content);
	}
	
	private static void writeMessage(Context context, String address, String content) {
		ContentValues values = new ContentValues();
		values.put("address", address);
		values.put("body", content);
		context.getContentResolver().insert(UriConstants.SENT_URI, values);
	}
	
	public static int getContactID(ContentResolver resolver, Uri uri) {
		Cursor cursor = resolver.query(uri, new String[]{"has_phone_number", "_id"}, null, null, null);
		if(cursor != null && cursor.moveToFirst()) {
			int hasPhoneNumber = cursor.getInt(0);
			if(hasPhoneNumber > 0) {
				int contactID = cursor.getInt(1);
				cursor.close();
				return contactID;
			}
		}
		return -1;
	}
	
	public static String getContactAddress(ContentResolver resolver, int contact_id) {
		String selection = "contact_id = ?";
		String[] selectionArgs = {String.valueOf(contact_id)};
		Cursor cursor = resolver.query(Phone.CONTENT_URI, new String[]{Phone.NUMBER}, selection, selectionArgs, null);
		
		if(cursor != null && cursor.moveToFirst()) {
			String address = cursor.getString(0);
			cursor.close();
			return address;
		}
		return null;
	}
	
	public static Uri getTypeUri(int position) {
		switch (position) {
		case 0:
			return UriConstants.INBOX_URI;
		case 1:
			return UriConstants.OUTBOX_URI;
		case 2:
			return UriConstants.SENT_URI;
		case 3:
			return UriConstants.DRAFT_URI;
		default:
			break;
		}
		return null;
	}
}
