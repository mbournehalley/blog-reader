package com.bourne.blogreader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class MainListActivity extends ListActivity {

	protected String[] mBlogPostTitle;
	public static final int NUMBER_OF_POST = 20;
	public static final String TAG = MainListActivity.class.getSimpleName();
	protected JSONObject mBlogData;
	protected ProgressBar mProgressBar;
	
	private final String KEY_TITLE = "title";
	private final String KEY_AUTHOR = "author";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);

        if(isNetworkAvailable()){
        	mProgressBar.setVisibility(View.VISIBLE);

        	GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
        	getBlogPostsTask.execute();
        }
        else {
        	Toast.makeText(this, "Network is unavailabe", Toast.LENGTH_LONG).show();
        }
//
//        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	super.onListItemClick(l, v, position, id);

		try {
			JSONArray jsonPosts = mBlogData.getJSONArray("posts");
			JSONObject jsonPost = jsonPosts.getJSONObject(position);
	    	String blogUrl = jsonPost.getString("url");
	    	
	    	Intent intent = new Intent(this, BlogWebViewActivity.class);
	    	intent.setData(Uri.parse(blogUrl));
	    	startActivity(intent);
	    	
		} catch (JSONException e) {
			logException(e);
		}
    	
    }

	private void logException(Exception e) {
		Log.e(TAG, "Exception caught!");
	}

    // Check if the network is available
    private boolean isNetworkAvailable() {
    	ConnectivityManager manager = 
    			(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo networkInfo = manager.getActiveNetworkInfo();
    	
    	boolean isAvailable = false;
    	if(networkInfo != null && networkInfo.isConnected()){
    		isAvailable = true;
    	}
		return isAvailable;
	}

    
    // Handle the blog response
    public void handleBlogResponse() {
    	
    	mProgressBar.setVisibility(View.INVISIBLE);
    	
    	if(mBlogData == null) {
    		updateDisplayForError();
    	}
    	else{
    		try {
    			JSONArray jsonPosts = mBlogData.getJSONArray("posts");
    			ArrayList<HashMap<String, String>> blogPosts = 
    					new ArrayList<HashMap<String,String>>();
    			
    			for(int i = 0; i < jsonPosts.length(); i++){
    				JSONObject post = jsonPosts.getJSONObject(i);
    				String title = post.getString(KEY_TITLE);
    				title = Html.fromHtml(title).toString();
    				String author = post.getString(KEY_AUTHOR);
    				author = Html.fromHtml(author).toString();
    				
    				HashMap<String, String> blogPost = new HashMap<String, String>();
    				blogPost.put(KEY_TITLE, title);
    				blogPost.put(KEY_AUTHOR, author);
    				
    				blogPosts.add(blogPost);
    			}
    			
    			String[] keys = { KEY_TITLE, KEY_AUTHOR };
    			int[] ids = { android.R.id.text1, android.R.id.text2 };
    			
    			SimpleAdapter adapter = 
    					new SimpleAdapter(this, blogPosts, android.R.layout.simple_list_item_2, keys, ids);
    			
    			setListAdapter(adapter);
    			
    		} catch (JSONException e) {
    			Log.d(TAG, "Exception caught!", e);	
    		}
    	}
	}

	private void updateDisplayForError() {
		// Build an AlertDialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.title));
		builder.setMessage(getString(R.string.error_message));
		builder.setPositiveButton(android.R.string.ok, null);
		
		AlertDialog dialog = builder.create();
		dialog.show();
		
		TextView emptyTextView = (TextView) getListView().getEmptyView();
		emptyTextView.setText(getString(R.string.no_items));
	}
    
    private class GetBlogPostsTask extends AsyncTask<Object, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Object... params) {
			int responseCode = -1;
			JSONObject jsonResponse = null;
			
			try {
				URL blogFeedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POST);
				HttpURLConnection connection = (HttpURLConnection) blogFeedUrl.openConnection();
				connection.connect();

				 responseCode = connection.getResponseCode();
				 if(responseCode == HttpURLConnection.HTTP_OK){
					 InputStream inputStream = connection.getInputStream();
					 Reader reader = new InputStreamReader(inputStream);
					 int contentLength = connection.getContentLength();
					 char[] charArray = new char[contentLength];
					 reader.read(charArray);
					 String responseData = new String(charArray);
					 
					 Log.v(TAG, responseData);
					 
					 //Retrieve the JSONObject 
					 jsonResponse = new JSONObject(responseData);
					 
				 }
				 else{
					 Log.i(TAG, "Unsuccessful HTTP Response Code: " + responseCode);
				 }
				Log.i(TAG, "Code " + responseCode);
			} catch (MalformedURLException e) {
				logException(e);
			} catch(IOException e) {
				logException(e);
			} catch(Exception e){
				logException(e);
			}
			
			return jsonResponse;
		}// Close doInBackground
		
		@Override
		protected void onPostExecute(JSONObject result){
			mBlogData = result;
			handleBlogResponse();
		}
    } // Close GetBlogPostsTask extends AsyncTask


}
