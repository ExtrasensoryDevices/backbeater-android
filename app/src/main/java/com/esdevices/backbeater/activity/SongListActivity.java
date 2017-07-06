package com.esdevices.backbeater.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ListView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.esdevices.backbeater.App;
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.adapter.SongListAdapter;
import com.esdevices.backbeater.model.Song;
import com.esdevices.backbeater.ui.widgets.BBTextView;
import com.esdevices.backbeater.utils.Preferences;
import java.util.List;

/**
 * Created by Alina Kholcheva on 2017-06-28.
 */

public class SongListActivity extends Activity implements SongListAdapter.OnSongListChangeListener {
    
    @Bind(R.id.listView) ListView listView;
    @Bind(R.id.hintText) BBTextView hintText;
    
    private List<Song> songList;
    private SongListAdapter adapter;
    
    boolean dataChanged = false;
    
    
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);
    
        ButterKnife.bind(this);
    
        songList = Preferences.getSongList();
        updateView();
        
        adapter = new SongListAdapter(this, songList, this);
        listView.setAdapter(adapter);
    }
    
    
    
    @OnClick(R.id.backButton)
    public void onBackButtonClick(View v) {
        if (dataChanged) {
            Preferences.putSongList(adapter.getObjects());
        }
        onBackPressed();
    }
    
    public void onDataChange(){
        dataChanged = true;
        Preferences.putSongList(adapter.getObjects());
        updateView();
    }

    private void updateView() {
        hintText.setVisibility(songList.size() == 0 ? View.VISIBLE : View.GONE);
    }
}
