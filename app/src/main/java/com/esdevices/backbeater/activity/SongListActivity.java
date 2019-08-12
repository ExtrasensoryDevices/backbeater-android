package com.esdevices.backbeater.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;

import com.esdevices.backbeater.R;
import com.esdevices.backbeater.model.Song;
import com.esdevices.backbeater.ui.widgets.BBEditTextView;
import com.esdevices.backbeater.ui.widgets.BBTextView;
import com.esdevices.backbeater.ui.widgets.DragLinearLayout;
import com.esdevices.backbeater.utils.Constants;
import com.esdevices.backbeater.utils.Preferences;
import com.flurry.android.FlurryAgent;

import java.util.Collections;
import java.util.List;

/**
 * Created by Alina Kholcheva on 2017-06-28.
 */

public class SongListActivity extends Activity  {
    
    @Bind(R.id.songListLayout) DragLinearLayout songListLayout;
    @Bind(R.id.hintText) BBTextView hintText;
    @Bind(R.id.scrollView) ScrollView scrollView;
    
    private List<Song> songList;
    
    boolean dataChanged = false;
    boolean oldSongLIstIsEmpty = false;
    int firstPos = -1, secondPos = -1;
    
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);
    
        ButterKnife.bind(this);
    
        songList = Preferences.getSongList();
        oldSongLIstIsEmpty = songList.isEmpty();
        updateView();
        updateSongListView();
//        songListLayout.setContainerScrollView(scrollView);
        songListLayout.setOnViewSwapListener(new DragLinearLayout.OnViewSwapListener() {
            @Override
            public void onSwap(View firstView, int firstPosition,
                               View secondView, int secondPosition) {
                if (firstPos == -1)
                    firstPos = firstPosition;
                secondPos = secondPosition;
            }

            @Override
            public void onSwapFinished() {
                if (firstPos != -1 && secondPos != -1 && firstPos != secondPos) {
                    Collections.swap(songList, firstPos, secondPos);
                }
                firstPos = -1;
                secondPos = -1;
            }

        });
    }
    

    @OnClick(R.id.backButton)
    public void onBackButtonClick(View v) {
        if (dataChanged) {
            Preferences.putSongList(songList);
        }
        onBackPressed();
    }
    
    @Override
    public void onBackPressed() {
        if (oldSongLIstIsEmpty && !songList.isEmpty()) {
            FlurryAgent.logEvent(Constants.FLURRY_TEMPO_LIST_CREATED);
        }
        super.onBackPressed();
    }
    
    @OnClick(R.id.addButton)
    public void onAddButtonClick(View v) {
        Song newSong = new Song("Song #"+(songList.size()+1), Constants.DEFAULT_TEMPO);
        songList.add(newSong);
        onDataChange();
        addSongRow(songList.size()-1, newSong, true);
    }
    
    public void onDataChange(){
        setResult(RESULT_OK);
        dataChanged = true;
        Preferences.putSongList(songList);
        updateView();
    }

    private void updateView() {
        hintText.setVisibility(songList.size() == 0 ? View.VISIBLE : View.GONE);
    }
    
    private void updateSongListView() {
        // remove song rows, keep plus row
        int totalCount = songListLayout.getChildCount();
        int songCount = totalCount-1;
        for (int i=0; i<songCount; i++) {
            songListLayout.removeViewAt(i);
        }
        // add new songs
        songCount = songList.size();
        for (int i=0; i<songCount; i++) {
            addSongRow(i, songList.get(i), false);
        }
    }
    
    private void addSongRow(int position, Song song, boolean startEdit) {
        View row = getLayoutInflater().inflate(R.layout.list_item_song, null);
        LinearLayout.LayoutParams payoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        ViewHolder viewHolder = new ViewHolder(position, song, row);
        row.setTag(viewHolder);

        songListLayout.addView(row, position, payoutParams);
        songListLayout.setViewDraggable(row, row);

        if (startEdit) {
            BBEditTextView songNameText = ((ViewHolder)row.getTag()).songNameText;
            songNameText.requestFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(songNameText, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    private void deleteSong(final int position) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
            .setMessage("Delete "+songList.get(position).name + "?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    View v = getCurrentFocus();
                    if (v instanceof EditText) {
                        v.clearFocus();
                    }
                    songList.remove(position);
                    songListLayout.removeViewAt(position);
                    onDataChange();
                }
            });
            dialog.create().show();
    }
    
    private int getPosition(View view) {
        return songListLayout.indexOfChild(view);
    }
    

    
    @OnClick({R.id.contentView, R.id.songListLayout})
    public void onContentViewClick(View v)
    {
        Log.d("Click", ""+v);
        getCurrentFocus().clearFocus();
        hideKeyboard();
    }
    
    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager) SongListActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getRootView().getWindowToken(), 0);
    }
    
    
    class ViewHolder {
    
        @Bind(R.id.songNameText) BBEditTextView songNameText;
        @Bind(R.id.tempoText) BBEditTextView tempoText;
        
        private final View parent;
    
        //public final int position;
    
        public ViewHolder(int position, Song song, View view) {
            ButterKnife.bind(this, view);
            this.parent = view;
            songNameText.setText(song.name.toUpperCase());
            tempoText.setText(""+song.tempo);

            tempoText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    Song song = songList.get(getPosition(parent));
                    String res = tempoText.getText().toString().trim();
                    try {
                        int value = Integer.parseInt(res);
                        if (value < Constants.MIN_TEMPO) {
                            return;

                        } else if (value > Constants.MAX_TEMPO) {
                            return;
                        }

                        if (song.tempo != value) {
                            song.tempo = value;
                            onDataChange();
                        }
                    } catch (NumberFormatException e) {
                        tempoText.setText("" + song.tempo);
                    }
                }
            });
        }
        
        @OnClick(R.id.deleteButton)
        public void onDeleteButtonClick(View v) {
            deleteSong(getPosition(parent));
        }
    
        @OnFocusChange(R.id.songNameText)
        public void onSongNameFocusChange(View v, boolean hasFocus) {
            if (hasFocus) { return;}
            Song song = songList.get(getPosition(parent));
            String res = songNameText.getText().toString().trim().toUpperCase();
            if (TextUtils.isEmpty(res)) {
                songNameText.setText(song.name.toUpperCase());
            } else {
                song.name = res;
                songNameText.setText(res);
                onDataChange();
            }
            songNameText.scrollTo(0,0);
        }
    
        
        @OnFocusChange(R.id.tempoText)
        public void onTempoFocusChange(View v, boolean hasFocus) {
            if (hasFocus){
                tempoText.selectAll();
            } else {
                Song song = songList.get(getPosition(parent));
                String res = tempoText.getText().toString().trim();
                try {
                    int value = Integer.parseInt(res);
                    if (value < Constants.MIN_TEMPO) {
                        value = Constants.MIN_TEMPO;
                    } else if (value > Constants.MAX_TEMPO) {
                        value = Constants.MAX_TEMPO;
                    }
                    tempoText.setText("" + value);
                    song.tempo = value;
                    onDataChange();
                } catch (NumberFormatException e) {
                    tempoText.setText("" + song.tempo);
                }
            }
        }

        @OnEditorAction(R.id.tempoText)
        boolean tempoEditorAction(int actionId) {
            if (tempoText.hasFocus()) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                    hideKeyboard();
                    tempoText.clearFocus();
                    return false;
                }
            }
            return false;
        }
    }
}
