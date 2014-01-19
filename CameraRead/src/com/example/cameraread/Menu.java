package com.example.cameraread;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Menu extends ListActivity {
	
	String menuItems[] = { "English", "Mandarine (Simplified)", "Japanese" }; 
	String menuLangs[] = { "eng", "chi_sim", "jpn" };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new ArrayAdapter<String>(Menu.this, android.R.layout.simple_list_item_1, menuItems));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		try {
			Intent intent = new Intent(Menu.this, Class.forName( ReadText.class.getName() )); 
			Bundle b = new Bundle();
			b.putString("lang", menuLangs[position]);
			intent.putExtras(b);
			startActivity(intent);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}	
}
