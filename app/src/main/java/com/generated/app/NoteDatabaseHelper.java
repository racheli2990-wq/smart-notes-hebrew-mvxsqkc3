package com.generated.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class NoteDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_notes.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NOTES = "notes";
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_TAG = "tag";
    public static final String COL_IMAGE_PATH = "image_path";
    public static final String COL_AUDIO_PATH = "audio_path";
    public static final String COL_TRANSCRIPTION = "transcription";
    public static final String COL_CHECKLIST = "checklist";
    public static final String COL_UPDATED_AT = "updated_at";

    private static NoteDatabaseHelper instance;

    public static synchronized NoteDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NoteDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private NoteDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createQuery = "CREATE TABLE " + TABLE_NOTES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_CONTENT + " TEXT, " +
                COL_TAG + " TEXT, " +
                COL_IMAGE_PATH + " TEXT, " +
                COL_AUDIO_PATH + " TEXT, " +
                COL_TRANSCRIPTION + " TEXT, " +
                COL_CHECKLIST + " TEXT, " +
                COL_UPDATED_AT + " INTEGER" +
                ");";
        db.execSQL(createQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    public long insertNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, note.getTitle());
        cv.put(COL_CONTENT, note.getContent());
        cv.put(COL_TAG, note.getTag());
        cv.put(COL_IMAGE_PATH, note.getImagePath());
        cv.put(COL_AUDIO_PATH, note.getAudioPath());
        cv.put(COL_TRANSCRIPTION, note.getTranscription());
        cv.put(COL_CHECKLIST, note.getChecklistJson());
        cv.put(COL_UPDATED_AT, note.getUpdatedAt());
        return db.insert(TABLE_NOTES, null, cv);
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, note.getTitle());
        cv.put(COL_CONTENT, note.getContent());
        cv.put(COL_TAG, note.getTag());
        cv.put(COL_IMAGE_PATH, note.getImagePath());
        cv.put(COL_AUDIO_PATH, note.getAudioPath());
        cv.put(COL_TRANSCRIPTION, note.getTranscription());
        cv.put(COL_CHECKLIST, note.getChecklistJson());
        cv.put(COL_UPDATED_AT, note.getUpdatedAt());
        return db.update(TABLE_NOTES, cv, COL_ID + "=?", new String[]{String.valueOf(note.getId())});
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NOTES, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public Note getNote(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOTES, null, COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Note note = cursorToNote(cursor);
            cursor.close();
            return note;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public List<Note> searchNotes(String query, String tagFilter) {
        List<Note> list = new ArrayList<Note>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_NOTES + " WHERE 1=1");
        List<String> args = new ArrayList<String>();

        if (tagFilter != null && !tagFilter.isEmpty() && !tagFilter.equals("הכל")) {
            sql.append(" AND " + COL_TAG + " = ?");
            args.add(tagFilter);
        }

        if (query != null && !query.trim().isEmpty()) {
            String wildcard = "%" + query.trim() + "%";
            sql.append(" AND (" + COL_TITLE + " LIKE ? OR " +
                    COL_CONTENT + " LIKE ? OR " +
                    COL_TRANSCRIPTION + " LIKE ? OR " +
                    COL_CHECKLIST + " LIKE ?)");
            args.add(wildcard);
            args.add(wildcard);
            args.add(wildcard);
            args.add(wildcard);
        }

        sql.append(" ORDER BY " + COL_UPDATED_AT + " DESC");

        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursorToNote(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    private Note cursorToNote(Cursor cursor) {
        Note note = new Note();
        note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
        note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)));
        note.setTag(cursor.getString(cursor.getColumnIndexOrThrow(COL_TAG)));
        note.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH)));
        note.setAudioPath(cursor.getString(cursor.getColumnIndexOrThrow(COL_AUDIO_PATH)));
        note.setTranscription(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSCRIPTION)));
        note.setChecklistJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHECKLIST)));
        note.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT)));
        return note;
    }
}
