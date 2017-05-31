package com.wenzhiming.sms520.util;

import android.net.Uri;

public class UriConstants {

	public static final Uri CONVERSATION_URI = Uri.parse("content://sms/conversations");
	
	public static final Uri SMS_URI = Uri.parse("content://sms/");

	public static final Uri SENT_URI = Uri.parse("content://sms/sent");
	
	public static final Uri INBOX_URI = Uri.parse("content://sms/inbox");
	
	public static final Uri OUTBOX_URI = Uri.parse("content://sms/outbox");
	
	public static final Uri DRAFT_URI = Uri.parse("content://sms/draft");
	
	public static final Uri GROUPS_INSERT_URI = Uri.parse("content://com.itheima23.smsmanager.provider.GroupContentProvider/groups/insert");
	
	public static final Uri GROUPS_QUERY_ALL_URI = Uri.parse("content://com.itheima23.smsmanager.provider.GroupContentProvider/groups/");
	
	public static final Uri GROUPS_UPDATE_URI = Uri.parse("content://com.itheima23.smsmanager.provider.GroupContentProvider/groups/update");
	
	public static final Uri GROUPS_DELETE_URI = Uri.parse("content://com.itheima23.smsmanager.provider.GroupContentProvider/groups/delete/#");
	
	public static final Uri THREAD_GROUP_INSERT_URI = Uri.parse("content://com.itheima23.smsmanager.provider.GroupContentProvider/thread_group/insert");
	
	public static final Uri THREAD_GROUP_QUERY_ALL_URI = Uri.parse("content://com.itheima23.smsmanager.provider.GroupContentProvider/thread_group");
	
	public static final int RECEIVE_TYPE = 1;
	public static final int SEND_TYPE = 2;
}
