package com.esdevices.backbeater.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.ListView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.model.Song;
import com.esdevices.backbeater.ui.widgets.BBEditTextView;
import com.esdevices.backbeater.utils.Constants;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alina Kholcheva on 2017-06-29.
 */

public class SongListAdapter extends BaseAdapter {
    
    public interface OnSongListChangeListener {
        void onDataChange();
    }
    
    private Context context;
    private List<Song> objects = new ArrayList();
    
    private OnSongListChangeListener listener;
    
    
    public SongListAdapter(Context context, @NonNull List<Song> objects, OnSongListChangeListener listener){
        super();
        this.context = context;
        this.objects = objects;
        this.listener = listener;
    }
    
    @Override public int getCount() {
        return objects.size()+1;
    }
    
    public List<Song> getObjects() {
        return objects;
    }
    
    @Override public long getItemId(int position) {
        return position;
    }
    
    @Override public Song getItem(int position) {
        if (position < objects.size()) {
            return objects.get(position);
        } else {
            return null;
        }
    }
    
    @Override public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder holder;
        
        if (position == objects.size()) {
            // last row with ADD button
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_add, parent, false);
            convertView.setTag(null);
            View addButton = convertView.findViewById(R.id.addButton);
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    addSongButtonPressed(parent);
                }
            });
            return convertView;
        }
        
        // rows with songs
        if (convertView == null || convertView.getTag() == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_song, parent, false);
            holder = new ViewHolder(position, convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
    
        Song song = getItem(position);
        holder.position = position;
        holder.songNameText.setText(song.name);
        holder.tempoText.setText(""+song.tempo);
        
        return convertView;
    }
    
    
    private void addSongButtonPressed(ViewGroup parent){
        objects.add(new Song("Song #"+getCount(), Constants.DEFAULT_TEMPO));
        notifyDataSetChanged();
        notifyDataChanged();
        if (parent instanceof ListView) {
            ((ListView)parent).setSelection(getCount());
        }
        
    }
    
    private void deleteSongButtonPressed(final int position) {
    
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Delete "+getItem(position).name+"?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                if (position < objects.size()) {
                    objects.remove(position);
                    notifyDataSetChanged();
                    notifyDataChanged();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }
    
    private void notifyDataChanged(){
        if (listener != null) {
            listener.onDataChange();
        }
    }
    
    class ViewHolder {
        
        @Bind(R.id.songNameText) BBEditTextView songNameText;
        @Bind(R.id.tempoText) BBEditTextView tempoText;
    
        private int position;
    
        public ViewHolder(int position, View view) {
            ButterKnife.bind(this, view);
            this.position = position;
        }
        
        @OnClick(R.id.deleteButton)
        public void onDeleteButtonClick(View v) {
            deleteSongButtonPressed(position);
        }
        
        //@OnClick(R.id.songNameText)
        //public void onSongNameTextClick(View v) {
        //    songNameText.requestFocus();
        //}
        //
        //@OnClick(R.id.tempoText)
        //public void onTempoTextClick(View v) {
        //    tempoText.requestFocus();
        //}
        
    
        @OnFocusChange(R.id.songNameText)
        public void onNameFocusChange(View v, boolean hasFocus) {
            if (!hasFocus){
                Song song = objects.get(position);
                String res = songNameText.getText().toString().trim();
                if (TextUtils.isEmpty(res)) {
                    songNameText.setText(song.name);
                } else {
                    song.name = res;
                    //notifyDataChanged();
                }
            }
        }
    
        @OnFocusChange(R.id.tempoText)
        public void onTempoFocusChange(View v, boolean hasFocus) {
            if (!hasFocus){
                Song song = objects.get(position);
                String res = tempoText.getText().toString().trim();
                try {
                    int value = Integer.parseInt(res);
                    if (value < Constants.MIN_TEMPO || value > Constants.MAX_TEMPO) {
                        tempoText.setText(""+song.tempo);
                    }
                    song.tempo = value;
                    //notifyDataChanged();
                } catch (NumberFormatException e) {
                    tempoText.setText(""+song.tempo);
                }
            }
        }
    
    
        @OnEditorAction(R.id.songNameText)
        boolean songNameEditorAction(int actionId) {
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_NULL) {
                tempoText.requestFocus();
                return false;
            }
            return false;
        }
    }
}
    
