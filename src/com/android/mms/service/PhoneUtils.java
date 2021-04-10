/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.service;

import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.mms.service.R;
import java.util.Locale;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.content.Context;
import android.content.res.XmlResourceParser;
import com.android.internal.util.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
/**
 * Utility to handle phone numbers.
 */
public class PhoneUtils {

    /**
     * Get a canonical national format phone number. If parsing fails, just return the
     * original number.
     *
     * @param telephonyManager
     * @param subId The SIM ID associated with this number
     * @param phoneText The input phone number text
     * @return The formatted number or the original phone number if failed to parse
     */

    public static int mSubId;
    public static Context mContext;
    public static  SubscriptionManager mSubscriptionManager;
    public static String getNationalNumber(TelephonyManager telephonyManager, int subId,
            String phoneText) {
        final String country = getSimOrDefaultLocaleCountry(telephonyManager, subId);
        final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        final Phonenumber.PhoneNumber parsed = getParsedNumber(phoneNumberUtil, phoneText, country);
        if (parsed == null) {
            return phoneText;
        }
        return phoneNumberUtil
                .format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
                .replaceAll("\\D", "");
    }

    // Parse the input number into internal format
    private static Phonenumber.PhoneNumber getParsedNumber(PhoneNumberUtil phoneNumberUtil,
            String phoneText, String country) {
        try {
            final Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(phoneText, country);
            if (phoneNumberUtil.isValidNumber(phoneNumber)) {
                return phoneNumber;
            } else {
                LogUtil.e("getParsedNumber: not a valid phone number"
                        + " for country " + country);
                return null;
            }
        } catch (final NumberParseException e) {
            LogUtil.e("getParsedNumber: Not able to parse phone number", e);
            return null;
        }
    }

    // Get the country/region either from the SIM ID or from locale
    private static String getSimOrDefaultLocaleCountry(TelephonyManager telephonyManager,
            int subId) {
        String country = getSimCountry(telephonyManager, subId);
        if (TextUtils.isEmpty(country)) {
            country = Locale.getDefault().getCountry();
        }

        return country;
    }

    // Get country/region from SIM ID
    private static String getSimCountry(TelephonyManager telephonyManager, int subId) {
        final String country = telephonyManager.getSimCountryIso(subId);
        if (TextUtils.isEmpty(country)) {
            return null;
        }
        return country.toUpperCase();
    }
    public static boolean isOperatorSupport(Context context,int subId){
        LogUtil.d(" isOperatorSupport");
        return getSupportMccMncFromVowifiProvider(context,subId);
    }


     public static final String CONTENT_URI = "content://com.spreadtrum.vowifi.accountsettings";
     public static final String FUN_MESSAGE  = "message";
     private static final Uri URI_MESSAGE =
               Uri.parse(CONTENT_URI + "/" + FUN_MESSAGE);
    private static boolean getSupportMccMncFromVowifiProvider(Context context,int subId) {

        Builder builder = URI_MESSAGE.buildUpon();
        Uri queryUri = builder.appendQueryParameter("subId", String.valueOf(subId)).build();

        Cursor cursor = context.getContentResolver().query(queryUri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow("mmsSupport");
                LogUtil.d(" getSupportMccMncFromVowifiProvider"+index);
                if (index > -1) {

                    return cursor.getInt(index) == 1 ? true : false;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

  
}
