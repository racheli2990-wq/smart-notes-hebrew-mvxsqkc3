package com.generated.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "SmartNotesPrefs";
    public static final String PREF_VIEW_MODE = "pref_view_mode"; // "grid" or "list"
    public static final String PREF_SPEECH_LANG = "pref_speech_lang"; // "he-IL" or "en-US"
    public static final String PREF_TRANSCRIBE_REALTIME = "pref_transcribe_realtime"; // boolean

    private static final int REQ_PERM_MIC = 101;

    private NoteDatabaseHelper dbHelper;
    private TextView tvNotesCount;
    private EditText etSearch;
    private LinearLayout tagsLayout;
    private LinearLayout columnLeft;
    private LinearLayout columnRight;
    private View emptyStateLayout;
    private ImageButton btnToggleView;
    private ImageButton btnSettings;
    private ImageButton fabNewNote;
    private ImageButton fabRecord;

    private String currentSelectedTag = "הכל";
    private String currentSearchText = "";
    private boolean isGridMode = true;

    private final String[] TAGS = {"הכל", "עבודה", "אישי", "רעיונות", "הקלטות"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = NoteDatabaseHelper.getInstance(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isGridMode = prefs.getString(PREF_VIEW_MODE, "grid").equals("grid");

        initViews();
        setupTagsBar();
        setupListeners();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void initViews() {
        tvNotesCount = (TextView) findViewById(R.id.tvNotesCount);
        etSearch = (EditText) findViewById(R.id.etSearch);
        tagsLayout = (LinearLayout) findViewById(R.id.tagsLayout);
        columnLeft = (LinearLayout) findViewById(R.id.columnLeft);
        columnRight = (LinearLayout) findViewById(R.id.columnRight);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        btnToggleView = (ImageButton) findViewById(R.id.btnToggleView);
        btnSettings = (ImageButton) findViewById(R.id.btnSettings);
        fabNewNote = (ImageButton) findViewById(R.id.fabNewNote);
        fabRecord = (ImageButton) findViewById(R.id.fabRecord);
    }

    private void setupTagsBar() {
        tagsLayout.removeAllViews();
        for (final String tag : TAGS) {
            Button tagBtn = new Button(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(38)
            );
            lp.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            tagBtn.setLayoutParams(lp);
            tagBtn.setText(tag);
            tagBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tagBtn.setPadding(dpToPx(14), 0, dpToPx(14), 0);
            tagBtn.setTransformationMethod(null);

            boolean isSelected = tag.equals(currentSelectedTag);
            tagBtn.setBackgroundResource(isSelected ? R.drawable.bg_tag_selected : R.drawable.bg_tag_unselected);
            tagBtn.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF334155);

            tagBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentSelectedTag = tag;
                    setupTagsBar();
                    loadNotes();
                }
            });
            tagsLayout.addView(tagBtn);
        }
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString();
                loadNotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnToggleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isGridMode = !isGridMode;
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putString(PREF_VIEW_MODE, isGridMode ? "grid" : "list")
                        .apply();
                loadNotes();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        fabNewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                if (!currentSelectedTag.equals("הכל")) {
                    intent.putExtra("default_tag", currentSelectedTag);
                }
                startActivity(intent);
            }
        });

        fabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                intent.putExtra("auto_record", true);
                intent.putExtra("default_tag", "הקלטות");
                startActivity(intent);
            }
        });
    }

    private void loadNotes() {
        columnLeft.removeAllViews();
        columnRight.removeAllViews();

        List<Note> notes = dbHelper.searchNotes(currentSearchText, currentSelectedTag);
        int total = notes.size();
        tvNotesCount.setText(total + " פתקים חכמים");

        if (total == 0) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            return;
        }
        emptyStateLayout.setVisibility(View.GONE);

        if (isGridMode) {
            columnRight.setVisibility(View.VISIBLE);
            for (int i = 0; i < total; i++) {
                Note note = notes.get(i);
                View card = createNoteCard(note);
                if (i % 2 == 0) {
                    columnLeft.addView(card);
                } else {
                    columnRight.addView(card);
                }
            }
        } else {
            // Single column List mode
            columnRight.setVisibility(View.GONE);
            for (Note note : notes) {
                View card = createNoteCard(note);
                columnLeft.addView(card);
            }
        }
    }

    private View createNoteCard(final Note note) {
        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        card.setLayoutParams(lp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        card.setElevation(dpToPx(2));

        // Optional attached Image Thumbnail
        if (note.getImagePath() != null && !note.getImagePath().isEmpty()) {
            File imgFile = new File(note.getImagePath());
            if (imgFile.exists()) {
                ImageView iv = new ImageView(this);
                LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(120)
                );
                ivLp.setMargins(0, 0, 0, dpToPx(10));
                iv.setLayoutParams(ivLp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Bitmap bitmap = decodeSampledBitmap(imgFile.getAbsolutePath(), 300, 200);
                if (bitmap != null) {
                    iv.setImageBitmap(bitmap);
                    card.addView(iv);
                }
            }
        }

        // Tag Badge
        if (note.getTag() != null && !note.getTag().isEmpty()) {
            TextView tvTag = new TextView(this);
            tvTag.setText(note.getTag());
            tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvTag.setTextColor(0xFF4F46E5);
            tvTag.setBackgroundResource(R.drawable.bg_audio_box);
            tvTag.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
            LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            tagLp.setMargins(0, 0, 0, dpToPx(8));
            tvTag.setLayoutParams(tagLp);
            card.addView(tvTag);
        }

        // Note Title
        if (note.getTitle() != null && !note.getTitle().isEmpty()) {
            TextView tvTitle = new TextView(this);
            tvTitle.setText(note.getTitle());
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvTitle.setTextColor(0xFF0F172A);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setMaxLines(2);
            card.addView(tvTitle);
        }

        // Transcription snippet if available
        if (note.getTranscription() != null && !note.getTranscription().isEmpty()) {
            TextView tvVoice = new TextView(this);
            tvVoice.setText("🎙️ " + note.getTranscription());
            tvVoice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvVoice.setTextColor(0xFFEC4899);
            tvVoice.setMaxLines(2);
            LinearLayout.LayoutParams voiceLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            voiceLp.setMargins(0, dpToPx(6), 0, 0);
            tvVoice.setLayoutParams(voiceLp);
            card.addView(tvVoice);
        }

        // Note Content snippet
        if (note.getContent() != null && !note.getContent().isEmpty()) {
            TextView tvBody = new TextView(this);
            tvBody.setText(note.getContent());
            tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvBody.setTextColor(0xFF475569);
            tvBody.setMaxLines(4);
            LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            bodyLp.setMargins(0, dpToPx(6), 0, 0);
            tvBody.setLayoutParams(bodyLp);
            card.addView(tvBody);
        }

        // Checklist snippet preview
        if (note.getChecklistJson() != null && !note.getChecklistJson().isEmpty()) {
            try {
                JSONArray ja = new JSONArray(note.getChecklistJson());
                if (ja.length() > 0) {
                    TextView tvTodo = new TextView(this);
                    JSONObject firstItem = ja.getJSONObject(0);
                    boolean done = firstItem.optBoolean("done", false);
                    String itemText = firstItem.optString("text", "");
                    tvTodo.setText((done ? "☑ " : "☐ ") + itemText + (ja.length() > 1 ? " (+" + (ja.length() - 1) + ")" : ""));
                    tvTodo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    tvTodo.setTextColor(0xFF10B981);
                    LinearLayout.LayoutParams todoLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    todoLp.setMargins(0, dpToPx(6), 0, 0);
                    tvTodo.setLayoutParams(todoLp);
                    card.addView(tvTodo);
                }
            } catch (Exception ignored) {}
        }

        // Date Footer
        TextView tvDate = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(note.getUpdatedAt())));
        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvDate.setTextColor(0xFF94A3B8);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateLp.setMargins(0, dpToPx(10), 0, 0);
        tvDate.setLayoutParams(dateLp);
        card.addView(tvDate);

        // Click to Open & Edit
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
                intent.putExtra("note_id", note.getId());
                startActivity(intent);
            }
        });

        // Long click to quick delete
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showQuickDeleteDialog(note);
                return true;
            }
        });

        return card;
    }

    private void showQuickDeleteDialog(final Note note) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת פתק")
                .setMessage("האם למחוק את הפתק \"" + (note.getTitle().isEmpty() ? "ללא כותרת" : note.getTitle()) + "\"?")
                .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteNote(note.getId());
                        loadNotes();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("הגדרות פתקים חכמים");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));
        layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 1. View Mode Option
        TextView tvMode = new TextView(this);
        tvMode.setText("מבנה מסך הבית:");
        tvMode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvMode.setTextColor(0xFF0F172A);
        tvMode.setTypeface(null, Typeface.BOLD);
        layout.addView(tvMode);

        final RadioGroup rgMode = new RadioGroup(this);
        RadioButton rbGrid = new RadioButton(this);
        rbGrid.setText("תצוגת כרטיסים ויזואלית (Grid / כרטיסים)");
        rbGrid.setId(View.generateViewId());
        RadioButton rbList = new RadioButton(this);
        rbList.setText("תצוגת רשימה נקייה (List)");
        rbList.setId(View.generateViewId());
        rgMode.addView(rbGrid);
        rgMode.addView(rbList);
        if (prefs.getString(PREF_VIEW_MODE, "grid").equals("grid")) {
            rbGrid.setChecked(true);
        } else {
            rbList.setChecked(true);
        }
        layout.addView(rgMode);

        // Separator
        View sep1 = new View(this);
        sep1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        sep1.setBackgroundColor(0xFFE2E8F0);
        LinearLayout.LayoutParams seplp = (LinearLayout.LayoutParams) sep1.getLayoutParams();
        seplp.setMargins(0, dpToPx(12), 0, dpToPx(12));
        layout.addView(sep1);

        // 2. Speech Language Option
        TextView tvLang = new TextView(this);
        tvLang.setText("שפת תמלול דיבור מקומי:");
        tvLang.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvLang.setTextColor(0xFF0F172A);
        tvLang.setTypeface(null, Typeface.BOLD);
        layout.addView(tvLang);

        final RadioGroup rgLang = new RadioGroup(this);
        RadioButton rbHe = new RadioButton(this);
        rbHe.setText("עברית (ישראל - he-IL)");
        rbHe.setId(View.generateViewId());
        RadioButton rbEn = new RadioButton(this);
        rbEn.setText("אנגלית (English - en-US)");
        rbEn.setId(View.generateViewId());
        rgLang.addView(rbHe);
        rgLang.addView(rbEn);
        if (prefs.getString(PREF_SPEECH_LANG, "he-IL").equals("he-IL")) {
            rbHe.setChecked(true);
        } else {
            rbEn.setChecked(true);
        }
        layout.addView(rgLang);

        // Separator
        View sep2 = new View(this);
        sep2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        sep2.setBackgroundColor(0xFFE2E8F0);
        LinearLayout.LayoutParams seplp2 = (LinearLayout.LayoutParams) sep2.getLayoutParams();
        seplp2.setMargins(0, dpToPx(12), 0, dpToPx(12));
        layout.addView(sep2);

        // 3. Transcribe Mode Option
        TextView tvTransMode = new TextView(this);
        tvTransMode.setText("אופן תמלול קולי:");
        tvTransMode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTransMode.setTextColor(0xFF0F172A);
        tvTransMode.setTypeface(null, Typeface.BOLD);
        layout.addView(tvTransMode);

        final RadioGroup rgTransMode = new RadioGroup(this);
        RadioButton rbLive = new RadioButton(this);
        rbLive.setText("בזמן אמת תוך כדי דיבור");
        rbLive.setId(View.generateViewId());
        RadioButton rbPost = new RadioButton(this);
        rbPost.setText("בסיום ההקלטה");
        rbPost.setId(View.generateViewId());
        rgTransMode.addView(rbLive);
        rgTransMode.addView(rbPost);
        if (prefs.getBoolean(PREF_TRANSCRIBE_REALTIME, true)) {
            rbLive.setChecked(true);
        } else {
            rbPost.setChecked(true);
        }
        layout.addView(rgTransMode);

        builder.setView(layout);
        builder.setPositiveButton("שמור הגדרות", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean grid = rgMode.getCheckedRadioButtonId() == rgMode.getChildAt(0).getId();
                String lang = rgLang.getCheckedRadioButtonId() == rgLang.getChildAt(0).getId() ? "he-IL" : "en-US";
                boolean live = rgTransMode.getCheckedRadioButtonId() == rgTransMode.getChildAt(0).getId();

                isGridMode = grid;
                prefs.edit()
                        .putString(PREF_VIEW_MODE, grid ? "grid" : "list")
                        .putString(PREF_SPEECH_LANG, lang)
                        .putBoolean(PREF_TRANSCRIBE_REALTIME, live)
                        .apply();

                loadNotes();
                Toast.makeText(MainActivity.this, "ההגדרות נשמרו בהצלחה", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("סגור", null);
        builder.show();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQ_PERM_MIC);
            }
        }
    }

    public static Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
