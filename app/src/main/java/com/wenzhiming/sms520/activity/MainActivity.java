package com.wenzhiming.sms520.activity;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wenzhiming.sms520.R;
import com.wenzhiming.sms520.util.CommonAsyncQuery;
import com.wenzhiming.sms520.util.SmsUtils;
import com.wenzhiming.sms520.util.UriConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener,
        View.OnClickListener, AdapterView.OnItemLongClickListener {

    private final int THREAD_ID_COLUMN_INDEX = 0;
    private final int ADDRESS_COLUMN_INDEX = 1;
    private final int DATE_COLUMN_INDEX = 2;
    private final int BODY_COLUMN_INDEX = 3;
    private final int COUNT_COLUMN_INDEX = 4;

    private boolean isEditMode = false;
    private HashSet<Integer> mMultiDeleteSet;
    private ProgressDialog deleteDialog;
    private boolean isStopDeleting = false;
    private ConversationAdapter mAdapter;
    private final String[] CONVERSATION_PROJECTION = {
            "sms.thread_id AS _id",
            "sms.address AS address",
            "sms.date AS date",
            "sms.body AS body",
            "groups.msg_count AS count"
    };

    private TextView tv_DeleteMsg;
    private FrameLayout fl_delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        prepareData();
    }

    private void initView() {
        mMultiDeleteSet = new HashSet<>();

        ListView lvConversation = (ListView) findViewById(R.id.lv_msg);
        fl_delete = (FrameLayout) findViewById(R.id.fl_delete);
        tv_DeleteMsg = (TextView) findViewById(R.id.tv_DeleteMsg);

        tv_DeleteMsg.setOnClickListener(this);

        mAdapter = new ConversationAdapter(this, null);
        lvConversation.setAdapter(mAdapter);
        lvConversation.setOnItemClickListener(this);
        lvConversation.setOnItemLongClickListener(this);
    }

    private String checkNull(String s) {
        return s != null && s.length() > 0 ? s : "";
    }

    private final String[] columnsForSms = new String[]{"_id", "address",
            "date", "read", "type", "body"};

    public String getSmsList(Context context) {
        Uri uri = Uri.parse("content://sms");
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(uri, columnsForSms, null, null, null);
        JSONArray list = new JSONArray();
        Log.d("getSmsList", ">>>>>>" + cursor.getCount());
        while (cursor.moveToNext()) {
            Map<String, Object> dataObj = new HashMap<String, Object>();
            for (int i = 0; i < columnsForSms.length; i++) {
                dataObj.put(columnsForSms[i], checkNull(cursor.getString(i)));
            }
            list.put(new JSONObject(dataObj));
        }
        cursor.close();

        return list.toString();
    }

    public boolean deleteSmsById(Context context, String id) {
        try {
            if (TextUtils.isEmpty(id)) {
                Toast.makeText(this, "请输入短信id", Toast.LENGTH_SHORT).show();
                return false;
            }
            String s = "content://sms/" + id;
            int count = context.getContentResolver().delete(Uri.parse(s), null, null);
            Toast.makeText(this, "实际删除短信" + count + "条", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfDefaultSMS();
    }

    /**
     * 检测是否为默认短信应用，否的话弹框请用户选择
     */
    private void checkIfDefaultSMS() {
        final String myPackageName = getPackageName();
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
            // App is not default.
            // Show the "not currently set as the default SMS app" interface
            View viewGroup = findViewById(R.id.ll_not_default_app);
            viewGroup.setVisibility(View.VISIBLE);

            // Set up a button that allows the user to change the default SMS app
            View button = findViewById(R.id.tv_change_default_app);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
                    startActivity(intent);
                }
            });
        } else {
            // App is the default.
            // Hide the "not currently set as the default SMS app" interface
            View viewGroup = findViewById(R.id.ll_not_default_app);
            viewGroup.setVisibility(View.GONE);
        }
    }

    private boolean isDefaultSMS() {
        return Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_DeleteMsg:
                if (isDefaultSMS()) {
                    showConfirmDeleteDialog();
                } else {
                    Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                    startActivity(intent);

                    if (isEditMode) {
                        isEditMode = false;
                        mMultiDeleteSet.clear();
                        refreshMode();
                        mAdapter.notifyDataSetChanged();
                    }
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        int thread_id = cursor.getInt(THREAD_ID_COLUMN_INDEX);

        if (isEditMode) {
            CheckBox cb = (CheckBox) view.findViewById(R.id.cb_conversation_item);
            if (cb.isChecked()) {
                cb.setChecked(false);
                mMultiDeleteSet.remove(thread_id);
            } else {
                cb.setChecked(true);
                mMultiDeleteSet.add(thread_id);
            }
            refreshMode();
        } else {
//            String address = cursor.getString(ADDRESS_COLUMN_INDEX);
//            Intent intent = new Intent(this, MsgDetailActivity.class);
//            intent.putExtra("address", address);
//            intent.putExtra("thread_id", thread_id);
//            startActivity(intent);
        }
    }

    /**
     * 长按item进入删除的编辑模式
     * @param parent
     * @param view
     * @param position
     * @param id
     * @return
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        isEditMode = true;
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        int thread_id = cursor.getInt(THREAD_ID_COLUMN_INDEX);

        if (thread_id != -1) {
            CheckBox cb = (CheckBox) view.findViewById(R.id.cb_conversation_item);
            if (cb.isChecked()) {
                cb.setChecked(false);
                mMultiDeleteSet.remove(thread_id);
            } else {
                cb.setChecked(true);
                mMultiDeleteSet.add(thread_id);
            }
            refreshMode();
            mAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    class ConversationAdapter extends CursorAdapter {

        private ConversationViewHolder mHolder;

        public ConversationAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = View.inflate(context, R.layout.item_listview_msg, null);
            mHolder = new ConversationViewHolder();
            mHolder.checkBox = (CheckBox) view.findViewById(R.id.cb_conversation_item);
            mHolder.ivIcon = (ImageView) view.findViewById(R.id.iv_conversation_item_icon);
            mHolder.tvName = (TextView) view.findViewById(R.id.tv_conversation_item_name);
            mHolder.tvDate = (TextView) view.findViewById(R.id.tv_conversation_item_date);
            mHolder.tvBody = (TextView) view.findViewById(R.id.tv_conversation_item_body);
            view.setTag(mHolder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            mHolder = (ConversationViewHolder) view.getTag();

            int thread_id = cursor.getInt(THREAD_ID_COLUMN_INDEX);
            String address = cursor.getString(ADDRESS_COLUMN_INDEX);
            long date = cursor.getLong(DATE_COLUMN_INDEX);
            String body = cursor.getString(BODY_COLUMN_INDEX);
            int count = cursor.getInt(COUNT_COLUMN_INDEX);


            if (isEditMode) {
                mHolder.checkBox.setChecked(mMultiDeleteSet.contains(thread_id));
                mHolder.checkBox.setVisibility(View.VISIBLE);
                mHolder.checkBox.setTag(thread_id);
                mHolder.checkBox.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        int thread_id = (Integer) v.getTag();
                        if (((CheckBox) v).isChecked()) {
                            mMultiDeleteSet.add(thread_id);
                        } else {
                            mMultiDeleteSet.remove(thread_id);
                        }
                        refreshMode();
                    }
                });
            } else {
                mHolder.checkBox.setVisibility(View.GONE);
            }

            String contactName = SmsUtils.getContactName(getContentResolver(), address);
            if (TextUtils.isEmpty(contactName)) {
                mHolder.tvName.setText(address + "(" + count + ")");
                mHolder.ivIcon.setBackgroundResource(R.drawable.ic_unknow_contact_picture);
            } else {
                mHolder.tvName.setText(contactName + "(" + count + ")");

                Bitmap contactIcon = SmsUtils.getContactIcon(getContentResolver(), address);
                if (contactIcon != null) {
                    mHolder.ivIcon.setBackgroundDrawable(new BitmapDrawable(contactIcon));
                } else {
                    mHolder.ivIcon.setBackgroundResource(R.drawable.ic_contact_picture);
                }
            }

            String strDate = null;
            if (DateUtils.isToday(date)) {
                strDate = DateFormat.getTimeFormat(context).format(date);
            } else {
                strDate = DateFormat.getDateFormat(context).format(date);
            }
            mHolder.tvDate.setText(strDate);

            mHolder.tvBody.setText(body);
        }
    }

    public class ConversationViewHolder {
        public CheckBox checkBox;
        public ImageView ivIcon;
        public TextView tvName;
        public TextView tvDate;
        public TextView tvBody;
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            isEditMode = false;
            mMultiDeleteSet.clear();
            refreshMode();
            mAdapter.notifyDataSetChanged();
            return;
        }
        super.onBackPressed();
    }

    private void refreshMode() {
        if (isEditMode) {
            fl_delete.setVisibility(View.VISIBLE);
        } else {
            fl_delete.setVisibility(View.GONE);
        }
    }

    private void prepareData() {
        CommonAsyncQuery asyncQuery = new CommonAsyncQuery(getContentResolver(), null);

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String threadIDs = intent.getStringExtra("threadIDs");

        String selection = null;
        if (!TextUtils.isEmpty(title)) {
            selection = "thread_id in " + threadIDs;
            setTitle(title);
        }

        asyncQuery.startQuery(0, mAdapter, UriConstants.CONVERSATION_URI, CONVERSATION_PROJECTION, selection, null, "date desc");
    }

    private void showConfirmDeleteDialog() {
        Builder builder = new Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle("删除");
        builder.setMessage("确认删除选中的对话吗?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProgressDialog();
                isStopDeleting = false;
                new Thread(new DeleteRunnable()).start();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showProgressDialog() {
        deleteDialog = new ProgressDialog(this);
        deleteDialog.setMax(mMultiDeleteSet.size());
        deleteDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        deleteDialog.setButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                isStopDeleting = true;
            }
        });

        deleteDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                isEditMode = false;
                refreshMode();
            }
        });
        deleteDialog.show();
    }

    class DeleteRunnable implements Runnable {

        @Override
        public void run() {

            Iterator<Integer> iterator = mMultiDeleteSet.iterator();
            ContentResolver resolver = getContentResolver();

            int thread_id;
            while (iterator.hasNext()) {
                thread_id = iterator.next();
                if (isStopDeleting) {
                    break;
                }
                String where = "thread_id = ?";
                String whereArgs[] = {String.valueOf(thread_id)};
                resolver.delete(UriConstants.SMS_URI, where, whereArgs);

                deleteDialog.incrementProgressBy(1);
            }

            mMultiDeleteSet.clear();
            deleteDialog.dismiss();
        }
    }
}
