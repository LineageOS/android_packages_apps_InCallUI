package com.android.callrecorder;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;
import android.util.Log;
import com.android.services.callrecorder.common.CallRecording;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Persistent data store for call recordings.  Usage:
 * open()
 * read/write operations
 * close()
 */
public class CallRecordingDataStore {

    private SQLiteOpenHelper mOpenHelper = null;
    private SQLiteDatabase mHandle = null;

    /**
     * Open before reading/writing.  Will not open handle if one is already open.
     */
    public void open(Context context) {
        if (mHandle == null) {
            mOpenHelper = new CallRecordingSQLiteOpenHelper(context);
            mHandle = mOpenHelper.getWritableDatabase();
        }
    }

    /**
     * close when finished reading/writing
     */
    public void close() {
        if (mHandle != null)
            mHandle.close();
        if (mOpenHelper != null)
            mOpenHelper.close();
        mHandle = null;
        mOpenHelper = null;
    }

    /**
     * Save a recording in the data store
     *
     * @param recording the recording to store
     */
    public void putRecording(CallRecording recording) {
        final String insertSql = "INSERT INTO " + CallRecordingsContract.CallRecording.TABLE_NAME + " (" +
                CallRecordingsContract.CallRecording.COLUMN_NAME_PHONE_NUMBER + ", " +
                CallRecordingsContract.CallRecording.COLUMN_NAME_CALL_DATE + ", " +
                CallRecordingsContract.CallRecording.COLUMN_NAME_RECORDING_FILENAME + ", " +
                CallRecordingsContract.CallRecording.COLUMN_NAME_CREATION_DATE + ") " +
                " VALUES (?, ?, ?, ?)";

        try {
            SQLiteStatement stmt = mHandle.compileStatement(insertSql);
            int idx = 1;
            stmt.bindString(idx++, recording.phoneNumber);
            stmt.bindLong(idx++, recording.creationTime);
            stmt.bindString(idx++, recording.fileName);
            stmt.bindLong(idx++, new Date().getTime());
            long id = stmt.executeInsert();
            Log.i(CallRecorder.TAG, "Saved recording " + recording + " with id " + id);
        }
        catch (SQLiteException e) {
            Log.w(CallRecorder.TAG, "Failed to save recording " + recording, e);
        }
    }

    /**
     * Get all recordings associated with a phone call
     *
     * @param phoneNumber phone number no spaces
     * @param callCreationDate time that the call was created
     * @return list of recordings
     */
    public List<CallRecording> getRecordings(String phoneNumber, Date callCreationDate) {
        List<CallRecording> resultList = new ArrayList<CallRecording>();

        final String query = "SELECT " +
                CallRecordingsContract.CallRecording.COLUMN_NAME_RECORDING_FILENAME +
                " FROM " + CallRecordingsContract.CallRecording.TABLE_NAME +
                " WHERE " + CallRecordingsContract.CallRecording.COLUMN_NAME_PHONE_NUMBER + " = ?" +
                " AND " + CallRecordingsContract.CallRecording.COLUMN_NAME_CALL_DATE + " = ?" +
                " ORDER BY " + CallRecordingsContract.CallRecording.COLUMN_NAME_CREATION_DATE;

        String args[] = {phoneNumber, String.valueOf(callCreationDate.getTime())};

        try {
            Cursor cursor = mHandle.rawQuery(query, args);
            while (cursor.moveToNext()) {
                String fileName = cursor.getString(0);
                CallRecording recording =
                        new CallRecording(phoneNumber, callCreationDate.getTime(), fileName, 0);
                if (recording.getFile().exists()) {
                    resultList.add(recording);
                }
            }
            cursor.close();
        }
        catch (SQLiteException e) {
            Log.w(CallRecorder.TAG, "Failed to fetch recordings for number " + phoneNumber +
                " date " + callCreationDate.getTime(), e);
        }

        return resultList;
    }

    static class CallRecordingsContract {
        public CallRecordingsContract() {}

        static abstract class CallRecording implements BaseColumns {
            static final String TABLE_NAME = "call_recordings";
            static final String COLUMN_NAME_PHONE_NUMBER = "phone_number";
            static final String COLUMN_NAME_CALL_DATE = "call_date";
            static final String COLUMN_NAME_RECORDING_FILENAME = "recording_filename";
            static final String COLUMN_NAME_CREATION_DATE = "creation_date";
        }
    }

    static class CallRecordingSQLiteOpenHelper extends SQLiteOpenHelper {

        private static final int VERSION = 1;
        private static final String DB_NAME = "callrecordings.db";

        public CallRecordingSQLiteOpenHelper(Context context) {
            super(context, DB_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + CallRecordingsContract.CallRecording.TABLE_NAME + " (" +
                CallRecordingsContract.CallRecording._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                CallRecordingsContract.CallRecording.COLUMN_NAME_PHONE_NUMBER + " TEXT," +
                CallRecordingsContract.CallRecording.COLUMN_NAME_CALL_DATE + " LONG," +
                CallRecordingsContract.CallRecording.COLUMN_NAME_RECORDING_FILENAME + " TEXT, " +
                CallRecordingsContract.CallRecording.COLUMN_NAME_CREATION_DATE + " LONG" +
                ");"
            );

            db.execSQL("CREATE INDEX IF NOT EXISTS phone_number_call_date_index ON " +
                CallRecordingsContract.CallRecording.TABLE_NAME + " (" +
                CallRecordingsContract.CallRecording.COLUMN_NAME_PHONE_NUMBER + ", " +
                CallRecordingsContract.CallRecording.COLUMN_NAME_CALL_DATE + ");"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // implement if we change the schema
        }
    }

}
