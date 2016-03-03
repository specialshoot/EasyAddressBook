package com.woozzu.android.model;

/**
 * Created by han on 15-11-27.
 */
public class ContactMember {

    private String contact_name;
    private String sortKey;
    private String contact_phone;
    private int Contact_id;
    private String email;

    public String getContact_name() {
        return contact_name;
    }

    public void setContact_name(String contact_name) {
        this.contact_name = contact_name;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public String getContact_phone() {
        return contact_phone;
    }

    public void setContact_phone(String contact_phone) {
        this.contact_phone = contact_phone;
    }

    public int getContact_id() {
        return Contact_id;
    }

    public void setContact_id(int contact_id) {
        Contact_id = contact_id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "ContactMember{" +
                "contact_name='" + contact_name + '\'' +
                ", sortKey='" + sortKey + '\'' +
                ", contact_phone='" + contact_phone + '\'' +
                ", Contact_id=" + Contact_id +
                ", email='" + email + '\'' +
                '}';
    }
}
