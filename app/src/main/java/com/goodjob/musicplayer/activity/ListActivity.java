package com.goodjob.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.goodjob.musicplayer.R;
import com.goodjob.musicplayer.adapter.AudioListAdapter;
import com.goodjob.musicplayer.entity.Audio;
import com.goodjob.musicplayer.entity.AudioListItem;
import com.goodjob.musicplayer.service.AudioPlayService;
import com.goodjob.musicplayer.util.AudioList;
import com.goodjob.musicplayer.util.MediaUtils;

import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {
    private int mLastPlay = -1;
    private ListView listView;
    private ArrayAdapter adapter;

    List<AudioListItem> audioItemList;

    private Intent getAudioIntent(Audio audio) {
        Intent intent = new Intent();
        intent.putExtra(AudioPlayService.AUDIO_PATH_STR, audio.getPath());
        intent.putExtra(AudioPlayService.AUDIO_TITLE_STR, audio.getTitle());
        intent.putExtra(AudioPlayService.AUDIO_ARTIST_STR, audio.getArtist());
        intent.putExtra(AudioPlayService.AUDIO_ALBUM_ID_INT, audio.getAlbumId());
        intent.putExtra(AudioPlayService.AUDIO_DURATION_INT, audio.getDuration());
        intent.putExtra(AudioPlayService.AUDIO_CURRENT_INT, 0);
        return intent;
    }

    private void play(int position) {
        AudioListItem item = audioItemList.get(position);
        if (position != mLastPlay) {
            Audio audio = item.getAudio();
            Intent serviceIntent = getAudioIntent(audio);
            serviceIntent.putExtra(AudioPlayService.ACTION_KEY, AudioPlayService.PLAY_ACTION);
            serviceIntent.setClass(this, AudioPlayService.class);

            item.setPlayStatus(AudioListItem.PLAYING);

            if (mLastPlay != -1) {
                ((AudioListItem) listView.getItemAtPosition(mLastPlay)).setPlayStatus(AudioListItem.DEFAULT);
            }
            mLastPlay = position;

            startService(serviceIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        List<Audio> audioList = AudioList.getAudioList(this);
        audioItemList = new ArrayList<>();
        for (Audio audio : audioList) {
            audioItemList.add(new AudioListItem(audio));
        }

        listView = (ListView) findViewById(R.id.list_view);
        adapter = new AudioListAdapter(this, R.layout.list_music, audioItemList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                play(position);
                Audio audio = audioItemList.get(position).getAudio();
                Intent activityIntent = getAudioIntent(audio);
                activityIntent.setClass(ListActivity.this, PlayerActivity.class);
                startActivity(activityIntent);
                adapter.notifyDataSetChanged();
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra(AudioPlayService.EVENT_KEY);
                switch (event) {
                    case AudioPlayService.FINISHED_EVENT:
                        Log.d("eventReceiver", "finished");
                        play((mLastPlay + 1) % audioItemList.size());
                        adapter.notifyDataSetChanged();
                        break;
                }
            }
        }, new IntentFilter(AudioPlayService.BROADCAST_EVENT_FILTER));
    }
}
