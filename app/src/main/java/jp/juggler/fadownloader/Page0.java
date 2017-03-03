package jp.juggler.fadownloader;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class Page0 extends PagerAdapterBase.PageViewHolder implements View.OnClickListener{

	EditText etURL;
	TextView tvFolder;
	EditText etInterval;
	EditText etFileType;

	public Page0( Activity activity, View ignored ){
		super( activity, ignored );
	}

	@Override protected void onPageCreate( int page_idx, View root ) throws Throwable{

		etURL = (EditText) root.findViewById( R.id.etURL );
		tvFolder = (TextView) root.findViewById( R.id.tvFolder );
		etInterval = (EditText) root.findViewById( R.id.etInterval );
		etFileType = (EditText) root.findViewById( R.id.etFileType );

		root.findViewById( R.id.btnFolderPicker ).setOnClickListener( this );
		root.findViewById( R.id.btnFolderPickerHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnFlashAirURLHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnIntervalHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnFileTypeHelp ).setOnClickListener( this );

		ui_value_load();
		folder_view_update();
	}

	@Override protected void onPageDestroy( int page_idx, View root ) throws Throwable{
		ui_value_save();
	}

	@Override public void onClick( View view ){
		switch( view.getId() ){
		case R.id.btnFolderPicker:
			folder_pick();
			break;
		case R.id.btnFolderPickerHelp:
			openHelp( R.layout.help_saf );
			break;
		case R.id.btnFlashAirURLHelp:
			openHelp( R.layout.help_flashair_url );
			break;
		case R.id.btnIntervalHelp:
			openHelp( R.layout.help_interval );
			break;
		case R.id.btnFileTypeHelp:
			openHelp( R.layout.help_file_type );
			break;
		}
	}

	void openHelp( int layout_id ){
		View v = activity.getLayoutInflater().inflate( layout_id, null, false );
		final Dialog d = new Dialog( activity );
		d.setContentView( v );
		d.getWindow().setLayout( WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT );
		d.show();
		v.findViewById( R.id.btnClose ).setOnClickListener( new View.OnClickListener(){
			@Override public void onClick( View view ){
				d.dismiss();
			}
		} );
	}

	// UIフォームの値を設定から読み出す
	void ui_value_load(){
		SharedPreferences pref = Pref.pref( activity );
		String sv;
		//
		sv = pref.getString( Pref.UI_FLASHAIR_URL, null );
		if( sv != null ) etURL.setText( sv );
		//
		sv = pref.getString( Pref.UI_INTERVAL, null );
		if( sv != null ) etInterval.setText( sv );
		//
		sv = pref.getString( Pref.UI_FILE_TYPE, null );
		if( sv != null ) etFileType.setText( sv );

	}

	// UIフォームの値を設定ファイルに保存
	void ui_value_save(){
		Pref.pref( activity ).edit()
			.putString( Pref.UI_FLASHAIR_URL, etURL.getText().toString() )
			.putString( Pref.UI_INTERVAL, etInterval.getText().toString() )
			.putString( Pref.UI_FILE_TYPE, etFileType.getText().toString() )
			.apply();
	}

	// 転送先フォルダの選択を開始
	void folder_pick(){
		Intent intent = new Intent( Intent.ACTION_OPEN_DOCUMENT_TREE );
		activity.startActivityForResult( intent, ActMain.REQUEST_CODE_DOCUMENT );
	}

	// フォルダの表示を更新
	String folder_view_update(){
		String folder_uri = null;

		String sv = Pref.pref( activity ).getString( Pref.UI_FOLDER_URI, null );
		if( ! TextUtils.isEmpty( sv ) ){
			DocumentFile folder = DocumentFile.fromTreeUri( activity, Uri.parse( sv ) );
			if( folder != null ){
				if( folder.exists() && folder.canWrite() ){
					folder_uri = sv;
					tvFolder.setText( folder.getName() );
				}
			}
		}

		if( folder_uri == null ){
			tvFolder.setText( R.string.not_selected );
		}

		return folder_uri;
	}
}
