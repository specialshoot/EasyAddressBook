package com.woozzu.android;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.woozzu.android.indexablelistview.R;
import com.woozzu.android.model.ContactMember;
import com.woozzu.android.util.ContactUtils;
import com.woozzu.android.util.StringMatcher;
import com.woozzu.android.util.ToastUtils;
import com.woozzu.android.view.CustomDialog;
import com.woozzu.android.widget.ContactContentObservers;
import com.woozzu.android.widget.IndexableListView;

import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.VCardComposer;
import a_vcard.android.syncml.pim.vcard.VCardException;

public class IndexableListViewActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "IndexableActivity";
    public static final int MSG_ALL = 0x1;
    public static final int MSG_PART = 0x2;
    public static final int MSG_CONTACT = 0x3;
    private ArrayList<String> mItems;
    private IndexableListView mListView;
    private ImageView ivDeleteText;
    private EditText etSearch;
    private ArrayList<ContactMember> list = null;
    private ContentAdapter adapter = null;
    private Button btnExported;
    private String path = Environment.getExternalStorageDirectory() + "/contacts.vcf";

    //检测通讯录是否有变化
    private ContactContentObservers contactobserver;
    ArrayList<String> changedContacts = new ArrayList<String>();    //改变列表
    ArrayList<String> deletedContacts = new ArrayList<String>();    //删除列表
    ArrayList<String> addedContacts = new ArrayList<String>();      //增加列表
    private static final String[] PHONES_PROJECTION = new String[]{
            ContactsContract.RawContacts._ID,
            ContactsContract.RawContacts.VERSION};

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initView();
        initAction();
    }

    private void initView() {
        mListView = (IndexableListView) findViewById(R.id.listview);
        ivDeleteText = (ImageView) findViewById(R.id.ivDeleteText);
        etSearch = (EditText) findViewById(R.id.etSearch);
        btnExported = (Button) findViewById(R.id.btnExported);
        btnExported.setOnClickListener(this);
    }

    private void initAction() {
        list = new ArrayList<ContactMember>();
        list = ContactUtils.getContact(this);
        if (adapter == null) {
            adapter = new ContentAdapter(this, list);
        }
        mListView.setAdapter(adapter);
        mListView.setFastScrollEnabled(true);
        ivDeleteText.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                etSearch.setText("");
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    ivDeleteText.setVisibility(View.GONE);
                    handler.obtainMessage(MSG_ALL).sendToTarget();
                } else {
                    ivDeleteText.setVisibility(View.VISIBLE);
                    handler.obtainMessage(MSG_PART).sendToTarget();
                }
            }
        });
        contactobserver = new ContactContentObservers(this, handler);
        queryIdAndVersion();
        registerContentObservers();
    }

    /**
     * 注册ContentObservers
     */
    private void registerContentObservers() {
        //注册一个监听数据库的监听器
        this.getContentResolver().registerContentObserver(ContactsContract.RawContacts.CONTENT_URI, true, contactobserver);

    }

    //程序刚开始时运行，存入sd.xml后面使用
    private void queryIdAndVersion() {
        Log.d("TAG", "queryIdAndVersion");
        String id = "";
        String version = "";
        ContentResolver resolver = this.getContentResolver();
        Cursor phoneCursor = resolver.query(ContactsContract.RawContacts.CONTENT_URI, PHONES_PROJECTION, ContactsContract.RawContacts.DELETED + "==0 and 1==" + ContactsContract.RawContacts.DIRTY, null, null);
        if (phoneCursor != null) {
            while (phoneCursor.moveToNext()) {

                id += phoneCursor.getString(0) + "#";
                version += phoneCursor.getString(1) + "#";

                Log.v(TAG, "ID: " + phoneCursor.getString(0));
                Log.v(TAG, "Version: " + phoneCursor.getString(1));
            }
            SharedPreferences sp = this.getSharedPreferences("sd", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("id", id);
            editor.putString("version", version);
            editor.commit();
        }
        phoneCursor.close();
    }

    //进一步判断是否修改通讯录。注意：打电话时会触发到此方法，因为监听的URi的关系
    private void bChange() {
        Log.d("TAG", "bChange");
        changedContacts.clear();
        deletedContacts.clear();
        deletedContacts.clear();
        String idStr;
        String versionStr;
        ArrayList<String> newid = new ArrayList<String>();
        ArrayList<String> newversion = new ArrayList<String>();
        SharedPreferences sp = this.getSharedPreferences("sd", MODE_PRIVATE);
        idStr = sp.getString("id", "");
        versionStr = sp.getString("version", "");
        String[] mid = idStr.split("#");
        String[] mversion = versionStr.split("#");
        ContentResolver resolver = this.getContentResolver();
        Cursor phoneCursor = resolver.query(ContactsContract.RawContacts.CONTENT_URI, PHONES_PROJECTION, ContactsContract.RawContacts.DELETED + "==0 and 1==" + ContactsContract.RawContacts.DIRTY, null, null);
        while (phoneCursor.moveToNext()) {
            newid.add(phoneCursor.getString(0));
            newversion.add(phoneCursor.getString(1));
        }
        phoneCursor.close();
        for (int i = 0; i < mid.length; i++) {
            int k = newid.size();
            int j;
            for (j = 0; j < k; j++) {
                //找到了，但是版本不一样，说明更新了此联系人的信息
                if (mid[i].equals(newid.get(j))) {
                    if (!(mversion[i].equals(newversion.get(j)))) {
                        changedContacts.add(newid.get(j) + "#" + newversion.get(j));
                        newid.remove(j);
                        newversion.remove(j);
                        break;

                    }
                    if (mversion[i].equals(newversion.get(j))) {
                        newid.remove(j);
                        newversion.remove(j);
                        break;
                    }
                }
            }
            //如果没有在新的链表中找到联系人
            if (j >= k) {
                deletedContacts.add(mid[i] + "#" + mversion[i]);
                Log.v("DEL", mid[i] + " " + mversion[i]);
            }
        }
        //查找新增加的人员
        int n = newid.size();
        for (int m = 0; m < n; m++) {
            addedContacts.add(newid.get(m) + "#" + newversion.get(m));
        }
        notifyMessage();
    }

    //通知栏消息
    private void notifyMessage() {
        Log.d("TAG", "enter notify");
        Log.d("TAG", "修改" + changedContacts.size() + "  删除" + deletedContacts.size() + "  增加" + addedContacts.size());
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle("My notification")
                        .setAutoCancel(true)//设置可以清除
                        .setContentText("通讯录有变化,点击更新");
        changedContacts.clear();
        deletedContacts.clear();
        addedContacts.clear();
        Intent resultIntent = new Intent(IndexableListViewActivity.this, IndexableListViewActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
        Log.d("TAG", "notify");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnExported:
                export();
                break;
            default:
                break;
        }
    }

    private void export() {
        btnExported.setClickable(false);
        ArrayList<ContactMember> listMember = ContactUtils.getContact(this);
        backupContacts(this, listMember);
    }

    /**
     * 备份联系人
     */
    public void backupContacts(Activity context, List<ContactMember> infos) {

        if (infos.size() <= 0) {
            return;
        }
        final CustomDialog.Builder builder = new CustomDialog.Builder(this);
        builder.setTitle("导出");
        builder.setPositiveButton("发送", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                share(path);
            }
        });
        CustomDialog dialog = builder.create();
        dialog.show();

        try {

            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(path), "UTF-8");
            VCardComposer composer = new VCardComposer();
            int count = 0;
            for (ContactMember info : infos) {
                ContactStruct contact = new ContactStruct();
                contact.name = info.getContact_name();
                contact.addPhone(2, info.getContact_phone(), null, true);
                contact.addContactmethod(1, 0, info.getEmail(), null, true);    //1代表邮箱
                String vcardString = composer.createVCard(contact, VCardComposer.VERSION_VCARD30_INT);
                writer.write(vcardString);
                writer.write("\n");
                writer.flush();
                ++count;
                dialog.setProgress(count);
            }
            writer.close();
            ToastUtils.showShort(context, "备份成功！");
            btnExported.setClickable(true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (VCardException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtils.showShort(context, "备份失败！");
        }
    }

    public void share(String path) {
        if (!path.equals("")) {
            Uri uri = Uri.parse("file:///" + path);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, "分享"));
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ALL:  //所有数据
                    getFuzzyQueryByName(etSearch.getText().toString());
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("TAG", "adapter null");
                    }
                    break;
                case MSG_PART:
                    getFuzzyQueryByName(etSearch.getText().toString());
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("TAG", "adapter null");
                    }
                    break;
                case MSG_CONTACT:
                    bChange();
                    break;
                default:
                    break;
            }
        }
    };

    private void getFuzzyQueryByName(String key) {

        if (key.equals("")) {
            list.clear();
            ArrayList<ContactMember> listMember = ContactUtils.getContact(this);
            for (ContactMember l : listMember) {
                list.add(l);
            }
        } else {
            Log.d("TAG", "进入getFuzzyQueryByName");
            ContentResolver cr = getContentResolver();
            String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.CONTACT_ID};
            Cursor cursor = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    ContactsContract.Contacts.DISPLAY_NAME + " like " + "'%" + key + "%'",
                    null, null);
            if (list != null) {
                list.clear();
            }
            if (cursor.getCount() <= 0) {
                ContactMember cm = new ContactMember();
                cm.setContact_name("无搜索结果");
                list.add(cm);
            } else {
                while (cursor.moveToNext()) {
                    ContactMember cm = new ContactMember();
                    cm.setContact_name(cursor.getString(
                            cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
                    cm.setContact_phone(cursor.getString(
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    cm.setContact_id(cursor
                            .getInt(cursor
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)));
                    list.add(cm);
                }
            }
            cursor.close();
        }
        Log.d("TAG", list.toString());
    }

    private class ContentAdapter extends ArrayAdapter<ContactMember> implements SectionIndexer {

        private String mSections = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        private Context context;
        List<ContactMember> items;

        public ContentAdapter(Context context, List<ContactMember> items) {
            super(context, 0, items);
            this.context = context;
            this.items = items;
        }

        @Override
        public int getPositionForSection(int section) {
            // If there is no item for current section, previous section will be selected
            for (int i = section; i >= 0; i--) {
                for (int j = 0; j < getCount(); j++) {
                    if (i == 0) {
                        // For numeric section
                        for (int k = 0; k <= 9; k++) {
                            if (StringMatcher.match(String.valueOf(getItem(j).getSortKey().charAt(0)), String.valueOf(k)))
                                return j;
                        }
                    } else {
                        if (StringMatcher.match(String.valueOf(getItem(j).getSortKey().charAt(0)), String.valueOf(mSections.charAt(i))))
                            return j;
                    }
                }
            }
            return 0;
        }

        @Override
        public int getSectionForPosition(int position) {
            return position;
        }

        @Override
        public Object[] getSections() {
            String[] sections = new String[mSections.length()];
            for (int i = 0; i < mSections.length(); i++)
                sections[i] = String.valueOf(mSections.charAt(i));
            return sections;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.contact_list, null);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.contact_item_name);
                holder.phone = (TextView) convertView.findViewById(R.id.contact_item_phone);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(items.get(position).getContact_name());
            holder.phone.setText(items.get(position).getContact_phone());
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ToastUtils.showShort(context, items.get(position).getContact_name());
                }
            });
            return convertView;
        }

        class ViewHolder {
            private TextView name;
            private TextView phone;
        }

    }
}