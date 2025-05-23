package com.callrecorder.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

public class ContactUtils {
    
    /**
     * Get contact name from phone number
     * @param context Application context
     * @param phoneNumber Phone number to lookup
     * @return Contact name if found, null otherwise
     */
    public static String getContactName(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }
        
        String contactName = null;
        
        // Query the contacts database
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return contactName;
    }
    
    /**
     * Format phone number for display
     * @param phoneNumber Raw phone number
     * @return Formatted phone number
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }
        
        // Remove non-digit characters
        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        
        // Format based on length
        if (digitsOnly.length() == 10) {
            // Format as XXX-XXX-XXXX
            return digitsOnly.substring(0, 3) + "-" + 
                   digitsOnly.substring(3, 6) + "-" + 
                   digitsOnly.substring(6);
        } else if (digitsOnly.length() > 10) {
            // Format as +X-XXX-XXX-XXXX for international numbers
            return "+" + digitsOnly.substring(0, digitsOnly.length() - 10) + "-" +
                   digitsOnly.substring(digitsOnly.length() - 10, digitsOnly.length() - 7) + "-" +
                   digitsOnly.substring(digitsOnly.length() - 7, digitsOnly.length() - 4) + "-" +
                   digitsOnly.substring(digitsOnly.length() - 4);
        }
        
        // Return as is if can't format
        return phoneNumber;
    }
}
