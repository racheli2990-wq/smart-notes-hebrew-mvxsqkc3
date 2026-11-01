package com.generated.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class NoteEditorActivity extends Activity {

    private static final int REQ_PICK_IMAGE = 201;
    private static final int REQ_PERM_RECORD = 202;

    private NoteDatabaseHelper dbHelper;
    private Note note;
    private boolean isNewNote = false;

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private LinearLayout editorTagsLayout;
    private LinearLayout checklistContainer;
    private View imageContainer;
    private ImageView ivNoteImage;
    private ImageButton btnRemoveImage;

    private View audioContainer;
    private TextView tvAudioStatus;
    private Button btnAudioAction;
    private Button btnTranscribeAudio;
    private ImageButton btnRemoveAudio;

    private View transcriptionContainer;
    private TextView tvTranscriptionText;

    private Button btnRecordMic;
    private Button btnAddImage;
    private Button btnAddTodoBlock;
    private ImageButton btnBack;
    private ImageButton btnShare;
    private ImageButton btnDelete;

    // Checklist models
    private static class ChecklistItem {
        String text;
        boolean done;

        ChecklistItem(String text, boolean done) {
            this.text = text;
            this.done = done;
        }
    }
    private final ArrayList<ChecklistItem> checklistItems = new ArrayList<ChecklistItem>();

    // Recording & Audio Playback
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private SpeechRecognizer speechRecognizer;
    private String tempAudioPath;

    private final String[] TAGS = {"עבודה", "אישי", "רעיונות", "הקלטות"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        dbHelper = NoteDatabaseHelper.getInstance(this);

        initViews();
        loadNoteData();
        setupTags();
        setupListeners();

        if (getIntent().getBooleanExtra("auto_record", false)) {
            // Auto start recording if opened via Quick Mic
            startVoiceRecordingWorkflow();
        }
    }

    private void initViews() {
        etNoteTitle = (EditText) findViewById(R.id.etNoteTitle);
        etNoteContent = (EditText) findViewById(R.id.etNoteContent);
        editorTagsLayout = (LinearLayout) findViewById(R.id.editorTagsLayout);
        checklistContainer = (LinearLayout) findViewById(R.id.checklistContainer);

        imageContainer = findViewById(R.id.imageContainer);
        ivNoteImage = (ImageView) findViewById(R.id.ivNoteImage);
        btnRemoveImage = (ImageButton) findViewById(R.id.btnRemoveImage);

        audioContainer = findViewById(R.id.audioContainer);
        tvAudioStatus = (TextView) findViewById(R.id.tvAudioStatus);
        btnAudioAction = (Button) findViewById(R.id.btnAudioAction);
        btnTranscribeAudio = (Button) findViewById(R.id.btnTranscribeAudio);
        btnRemoveAudio = (ImageButton) findViewById(R.id.btnRemoveAudio);

        transcriptionContainer = findViewById(R.id.transcriptionContainer);
        tvTranscriptionText = (TextView) findViewById(R.id.tvTranscriptionText);

        btnRecordMic = (Button) findViewById(R.id.btnRecordMic);
        btnAddImage = (Button) findViewById(R.id.btnAddImage);
        btnAddTodoBlock = (Button) findViewById(R.id.btnAddTodoBlock);
        btnBack = (ImageButton) findViewById(R.id.btnBack);
        btnShare = (ImageButton) findViewById(R.id.btnShare);
        btnDelete = (ImageButton) findViewById(R.id.btnDelete);
    }

    private void loadNoteData() {
        long noteId = getIntent().getLongExtra("note_id", -1);
        if (noteId != -1) {
            note = dbHelper.getNote(noteId);
        }

        if (note == null) {
            note = new Note();
            isNewNote = true;
            String defaultTag = getIntent().getStringExtra("default_tag");
            if (defaultTag != null) {
                note.setTag(defaultTag);
            }
        }

        etNoteTitle.setText(note.getTitle());
        etNoteContent.setText(note.getContent());

        // Load image
        refreshImageView();

        // Load audio
        refreshAudioView();

        // Load transcription
        refreshTranscriptionView();

        // Parse Checklist
        parseChecklist(note.getChecklistJson());
        renderChecklist();
    }

    private void setupTags() {
        editorTagsLayout.removeAllViews();
        for (final String tag : TAGS) {
            Button tagBtn = new Button(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(36)
            );
            lp.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            tagBtn.setLayoutParams(lp);
            tagBtn.setText(tag);
            tagBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tagBtn.setPadding(dpToPx(12), 0, dpToPx(12), 0);
            tagBtn.setTransformationMethod(null);

            boolean isSelected = tag.equals(note.getTag());
            tagBtn.setBackgroundResource(isSelected ? R.drawable.bg_tag_selected : R.drawable.bg_tag_unselected);
            tagBtn.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF334155);

            tagBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    note.setTag(tag);
                    setupTags();
                }
            });
            editorTagsLayout.addView(tagBtn);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndFinish();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteDialog();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShareDialog();
            }
        });

        btnAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });

        btnRemoveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                note.setImagePath("");
                refreshImageView();
            }
        });

        btnAddTodoBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptNewTodoItem();
            }
        });

        btnRecordMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopVoiceRecordingWorkflow();
                } else {
                    startVoiceRecordingWorkflow();
                }
            }
        });

        btnAudioAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    stopAudioPlayback();
                } else {
                    startAudioPlayback();
                }
            }
        });

        btnRemoveAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAudioPlayback();
                note.setAudioPath("");
                refreshAudioView();
            }
        });

        btnTranscribeAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOfflineSpeechRecognition();
            }
        });
    }

    // --- Media & Checklist UI Rendering ---

    private void refreshImageView() {
        if (note.getImagePath() != null && !note.getImagePath().isEmpty()) {
            File file = new File(note.getImagePath());
            if (file.exists()) {
                Bitmap bmp = MainActivity.decodeSampledBitmap(note.getImagePath(), 600, 400);
                if (bmp != null) {
                    ivNoteImage.setImageBitmap(bmp);
                    imageContainer.setVisibility(View.VISIBLE);
                    return;
                }
            }
        }
        imageContainer.setVisibility(View.GONE);
    }

    private void refreshAudioView() {
        if (note.getAudioPath() != null && !note.getAudioPath().isEmpty() && new File(note.getAudioPath()).exists()) {
            audioContainer.setVisibility(View.VISIBLE);
            tvAudioStatus.setText("קובץ הקלטה מצורף");
            btnAudioAction.setText(isPlaying ? "השהה" : "נגן הקלטה");
        } else {
            audioContainer.setVisibility(View.GONE);
        }
    }

    private void refreshTranscriptionView() {
        if (note.getTranscription() != null && !note.getTranscription().isEmpty()) {
            transcriptionContainer.setVisibility(View.VISIBLE);
            tvTranscriptionText.setText(note.getTranscription());
        } else {
            transcriptionContainer.setVisibility(View.GONE);
        }
    }

    private void parseChecklist(String json) {
        checklistItems.clear();
        if (json == null || json.isEmpty()) return;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                checklistItems.add(new ChecklistItem(
                        obj.optString("text", ""),
                        obj.optBoolean("done", false)
                ));
            }
        } catch (Exception ignored) {}
    }

    private String serializeChecklist() {
        JSONArray array = new JSONArray();
        for (ChecklistItem item : checklistItems) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("text", item.text);
                obj.put("done", item.done);
                array.put(obj);
            } catch (Exception ignored) {}
        }
        return array.toString();
    }

    private void renderChecklist() {
        checklistContainer.removeAllViews();
        for (int i = 0; i < checklistItems.size(); i++) {
            final int index = i;
            final ChecklistItem item = checklistItems.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpToPx(4), 0, dpToPx(4));

            final CheckBox cb = new CheckBox(this);
            cb.setChecked(item.done);

            final TextView tv = new TextView(this);
            tv.setText(item.text);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tv.setTextColor(item.done ? 0xFF94A3B8 : 0xFF0F172A);
            if (item.done) {
                tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvLp.setMargins(dpToPx(8), 0, dpToPx(8), 0);
            tv.setLayoutParams(tvLp);

            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.done = isChecked;
                    if (isChecked) {
                        tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        tv.setTextColor(0xFF94A3B8);
                    } else {
                        tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                        tv.setTextColor(0xFF0F172A);
                    }
                }
            });

            ImageButton btnDelItem = new ImageButton(this);
            btnDelItem.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)));
            btnDelItem.setBackgroundResource(0);
            btnDelItem.setImageResource(R.drawable.ic_delete);
            btnDelItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checklistItems.remove(index);
                    renderChecklist();
                }
            });

            row.addView(cb);
            row.addView(tv);
            row.addView(btnDelItem);
            checklistContainer.addView(row);
        }
    }

    private void promptNewTodoItem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("משימה חדשה לבדיקה");

        final EditText input = new EditText(this);
        input.setHint(R.string.new_todo_hint);
        input.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));
        container.addView(input, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton("הוסף", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString().trim();
                if (!text.isEmpty()) {
                    checklistItems.add(new ChecklistItem(text, false));
                    renderChecklist();
                }
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    // --- Audio Recording & Speech Transcription Workflow ---

    private void startVoiceRecordingWorkflow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQ_PERM_RECORD);
                return;
            }
        }

        try {
            File audioDir = getDir("recordings", MODE_PRIVATE);
            File audioFile = new File(audioDir, "rec_" + System.currentTimeMillis() + ".m4a");
            tempAudioPath = audioFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(tempAudioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            btnRecordMic.setText("⏹️ עצור הקלטה");
            btnRecordMic.setBackgroundResource(R.drawable.bg_button_rounded);

            Toast.makeText(this, "ההקלטה החלה! מדברים עכשיו...", Toast.LENGTH_SHORT).show();

            // Check if real-time transcription is requested in settings
            SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
            if (prefs.getBoolean(MainActivity.PREF_TRANSCRIBE_REALTIME, true)) {
                startOfflineSpeechRecognition();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה בפתיחת מיקרופון: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isRecording = false;
        }
    }

    private void stopVoiceRecordingWorkflow() {
        if (!isRecording) return;
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            isRecording = false;
            btnRecordMic.setText("🎤 הקלט ותמלל");
            btnRecordMic.setBackgroundResource(R.drawable.bg_fab_record);

            note.setAudioPath(tempAudioPath);
            refreshAudioView();
            Toast.makeText(this, "ההקלטה נשמרה בהצלחה!", Toast.LENGTH_SHORT).show();

            // If not real-time, run transcription on finish
            SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
            if (!prefs.getBoolean(MainActivity.PREF_TRANSCRIBE_REALTIME, true)) {
                startOfflineSpeechRecognition();
            }
        } catch (Exception e) {
            e.printStackTrace();
            isRecording = false;
        }
    }

    private void startOfflineSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "מנוע תמלול קולי אינו זמין במכשיר", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString(MainActivity.PREF_SPEECH_LANG, "he-IL");

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, lang);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        // Offline recognition flag for on-device processing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        }

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvAudioStatus.setText("תמלול פעיל ומקשיב...");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                tvAudioStatus.setText("התמלול מוכן");
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String transcribed = matches.get(0);
                    applyTranscription(transcribed);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String partial = matches.get(0);
                    applyTranscription(partial);
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(recognizerIntent);
    }

    private void applyTranscription(String text) {
        String existing = note.getTranscription();
        if (existing == null || existing.isEmpty()) {
            note.setTranscription(text);
        } else if (!existing.contains(text)) {
            note.setTranscription(existing + " " + text);
        }
        refreshTranscriptionView();

        // Also suggest title if empty
        if (etNoteTitle.getText().toString().trim().isEmpty() && text.length() > 0) {
            String candidate = text.length() > 30 ? text.substring(0, 30) + "..." : text;
            etNoteTitle.setText(candidate);
        }
    }

    // --- Audio Playback ---

    private void startAudioPlayback() {
        if (note.getAudioPath() == null || !new File(note.getAudioPath()).exists()) {
            Toast.makeText(this, "אין קובץ קול להשמעה", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(note.getAudioPath());
                mediaPlayer.prepare();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopAudioPlayback();
                    }
                });
            }
            mediaPlayer.start();
            isPlaying = true;
            btnAudioAction.setText("השהה");
            tvAudioStatus.setText("מנגן הקלטה...");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה בנגינת קובץ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            stopAudioPlayback();
        }
    }

    private void stopAudioPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        btnAudioAction.setText("נגן הקלטה");
        tvAudioStatus.setText("קובץ הקלטה מצורף");
    }

    // --- Image Handling ---

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "בחר תמונה לפתק"), REQ_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                copyImageToPrivateStorage(imageUri);
            }
        }
    }

    private void copyImageToPrivateStorage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            File imagesDir = getDir("note_images", MODE_PRIVATE);
            File targetFile = new File(imagesDir, "img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(targetFile);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            is.close();

            note.setImagePath(targetFile.getAbsolutePath());
            refreshImageView();
            Toast.makeText(this, "תמונה צורפה בהצלחה!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה בצירוף תמונה", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Share & Export ---

    private void showShareDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.share);

        String[] options = {"שיתוף תוכן ותמלול כטקסט", "שיתוף קובץ הקול (Audio)"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    shareNoteAsText();
                } else if (which == 1) {
                    shareAudioFile();
                }
            }
        });
        builder.show();
    }

    private void shareNoteAsText() {
        StringBuilder sb = new StringBuilder();
        String title = etNoteTitle.getText().toString().trim();
        if (!title.isEmpty()) {
            sb.append("📌 ").append(title).append("\n\n");
        }

        String content = etNoteContent.getText().toString().trim();
        if (!content.isEmpty()) {
            sb.append(content).append("\n\n");
        }

        if (note.getTranscription() != null && !note.getTranscription().isEmpty()) {
            sb.append("🎙️ תמלול קולי:\n").append(note.getTranscription()).append("\n\n");
        }

        if (!checklistItems.isEmpty()) {
            sb.append("☑️ משימות:\n");
            for (ChecklistItem item : checklistItems) {
                sb.append(item.done ? "[V] " : "[ ] ").append(item.text).append("\n");
            }
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "שתף פתק"));
    }

    private void shareAudioFile() {
        if (note.getAudioPath() == null || note.getAudioPath().isEmpty() || !new File(note.getAudioPath()).exists()) {
            Toast.makeText(this, "אין קובץ קול לשיתוף בפתק זה", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
            }
            File file = new File(note.getAudioPath());
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            startActivity(Intent.createChooser(shareIntent, "שתף הקלטת קול"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה בשיתוף הקובץ", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Save & Lifecycle ---

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (note.getId() != -1) {
                            dbHelper.deleteNote(note.getId());
                        }
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveAndFinish() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        String checklistJson = serializeChecklist();

        // If completely empty, ignore saving
        boolean isEmpty = title.isEmpty() && content.isEmpty() &&
                checklistItems.isEmpty() &&
                (note.getImagePath() == null || note.getImagePath().isEmpty()) &&
                (note.getAudioPath() == null || note.getAudioPath().isEmpty());

        if (isEmpty) {
            finish();
            return;
        }

        note.setTitle(title);
        note.setContent(content);
        note.setChecklistJson(checklistJson);
        note.setUpdatedAt(System.currentTimeMillis());

        if (isNewNote) {
            long id = dbHelper.insertNote(note);
            note.setId(id);
        } else {
            dbHelper.updateNote(note);
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudioPlayback();
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception ignored) {}
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
