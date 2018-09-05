package com.fearlessbear.pulltorefresh;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private RefreshListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = ((RefreshListView) findViewById(R.id.listview));
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 100;
            }

            @Override
            public Object getItem(int position) {
                return position + "";
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = View.inflate(parent.getContext(), android.R.layout.simple_list_item_1, null);
                }

                ((TextView) convertView.findViewById(android.R.id.text1)).setText(position + "");
                return convertView;
            }
        });

        listView.setOnRefreshListener(new RefreshListView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Toast.makeText(MainActivity.this, "onRefresh", Toast.LENGTH_SHORT).show();
                listView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        listView.stopRefresh();
                    }
                }, 5000);
            }
        });
        listView.setOnRefreshStateChangeListener(new RefreshListView.OnRefreshStateChangeListener() {
            @Override
            public void onStateChange(int state) {
                Log.d("TAG", "onStateChange: state is " + state);
            }
        });
    }
}
