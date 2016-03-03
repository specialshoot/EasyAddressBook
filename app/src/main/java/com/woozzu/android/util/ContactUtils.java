package com.woozzu.android.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.woozzu.android.model.ContactMember;

import java.util.ArrayList;

/**
 * Created by han on 15-11-27.
 */
public class ContactUtils {

    public static final String SORT_KEY_PRIMARY = "sort_key";

    private ContactUtils() {

    }

    public static ArrayList<ContactMember> getContact(Context context) {

        System.out.println("ENTER getContact");
        ArrayList<ContactMember> listMembers = new ArrayList<ContactMember>();
        Cursor cursor = null;
        try {

            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            // 这里是获取联系人表的电话里的信息  包括：名字，名字拼音，联系人id,电话号码；
            // 然后在根据"sort-key"排序
            cursor = context.getContentResolver().query(
                    uri,
                    new String[]{"display_name", "sort_key", "contact_id",
                            "data1"}, null, null, "sort_key");

            if (cursor.moveToFirst()) {
                do {
                    ContactMember contact = new ContactMember();
                    String contact_phone = cursor
                            .getString(cursor
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String name = cursor.getString(0);
                    String sortKey = getSortKey(PinYin.getPinYin(cursor.getString(1).substring(0, 1)));
                    String email=cursor.getString(cursor
                            .getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                    int contact_id = cursor
                            .getInt(cursor
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    contact.setContact_name(name);
                    contact.setSortKey(sortKey);
                    contact.setContact_phone(contact_phone);
                    contact.setContact_id(contact_id);
                    contact.setEmail(email);
                    if (name != null)
                        listMembers.add(contact);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return listMembers;
    }

    private static String getSortKey(String sortKeyString) {
        String key = sortKeyString.substring(0, 1).toUpperCase();
        if (key.matches("[A-Z]")) {
            return key;
        }
        return "#";
    }
}
