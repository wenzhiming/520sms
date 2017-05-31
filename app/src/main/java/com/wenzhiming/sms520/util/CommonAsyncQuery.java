package com.wenzhiming.sms520.util;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.CursorAdapter;

public class CommonAsyncQuery extends AsyncQueryHandler {
	
	private OnQueryCompleteListener mOnQueryCompleteListener;
	
	public CommonAsyncQuery(ContentResolver cr, OnQueryCompleteListener listener) {
		this(cr);
		this.mOnQueryCompleteListener = listener;
	}

	private CommonAsyncQuery(ContentResolver cr) {
		super(cr);
	}

	@Override
	protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
		SmsUtils.printCursor(cursor);
		
		if(mOnQueryCompleteListener != null) {
			mOnQueryCompleteListener.onPreQueryComplete(token, cursor);
		}
		
		if(cookie != null) {
			CursorAdapter adapter = (CursorAdapter) cookie;
			adapter.changeCursor(cursor);
		}
		
		if(mOnQueryCompleteListener != null) {
			mOnQueryCompleteListener.onPostQueryComplete(token, cursor);
		}
	}
	
	public interface OnQueryCompleteListener {
		
		public void onPreQueryComplete(int token, Cursor cursor);
		
		public void onPostQueryComplete(int token, Cursor cursor);
	}
}
