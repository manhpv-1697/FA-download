package jp.juggler.fadownloader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class PageSetting extends PagerAdapterBase.PageViewHolder implements View.OnClickListener{

	EditText etURL;
	TextView tvFolder;
	EditText etInterval;
	EditText etFileType;
	Spinner spLocationMode;
	EditText etLocationIntervalDesired;
	EditText etLocationIntervalMin;
	Switch swForceWifi;
	EditText etSSID;
	Switch swThumbnailAutoRotate;
	Switch swCopyBeforeViewSend;
	Spinner spTargetType;
View btnSSIDPicker;

	public PageSetting( Activity activity, View ignored ){
		super( activity, ignored );
	}

	@Override protected void onPageCreate( int page_idx, View root ) throws Throwable{

		spTargetType =(Spinner) root.findViewById( R.id.spTargetType );
		etURL = (EditText) root.findViewById( R.id.etTargetUrl );
		tvFolder = (TextView) root.findViewById( R.id.tvFolder );
		etInterval = (EditText) root.findViewById( R.id.etRepeatInterval );
		etFileType = (EditText) root.findViewById( R.id.etFileType );
		spLocationMode = (Spinner) root.findViewById( R.id.spLocationMode );
		etLocationIntervalDesired = (EditText) root.findViewById( R.id.etLocationIntervalDesired );
		etLocationIntervalMin = (EditText) root.findViewById( R.id.etLocationIntervalMin );
		swForceWifi = (Switch) root.findViewById( R.id.swForceWifi );
		etSSID = (EditText) root.findViewById( R.id.etSSID );
		swThumbnailAutoRotate = (Switch) root.findViewById( R.id.swThumbnailAutoRotate );
		swCopyBeforeViewSend = (Switch) root.findViewById( R.id.swCopyBeforeViewSend );
		btnSSIDPicker = root.findViewById( R.id.btnSSIDPicker );

		root.findViewById( R.id.btnFolderPicker ).setOnClickListener( this );
		root.findViewById( R.id.btnHelpFolderPicker ).setOnClickListener( this );
		root.findViewById( R.id.btnHelpTargetUrl ).setOnClickListener( this );
		root.findViewById( R.id.btnIntervalHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnFileTypeHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnLocationModeHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnLocationIntervalDesiredHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnLocationIntervalMinHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnForceWifiHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnSSIDHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnSSIDPicker ).setOnClickListener( this );
		root.findViewById( R.id.btnThumbnailAutoRotateHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnCopyBeforeViewSendHelp ).setOnClickListener( this );
		root.findViewById( R.id.btnTargetTypeHelp ).setOnClickListener( this );

		ArrayAdapter<CharSequence> location_mode_adapter = new ArrayAdapter<>(
			activity
			, android.R.layout.simple_spinner_item
		);
		location_mode_adapter.setDropDownViewResource( R.layout.spinner_dropdown );

		location_mode_adapter.addAll(
			activity.getString( R.string.location_mode_0 ),
			activity.getString( R.string.location_mode_1 ),
			activity.getString( R.string.location_mode_2 ),
			activity.getString( R.string.location_mode_3 ),
			activity.getString( R.string.location_mode_4 )
		);
		spLocationMode.setAdapter( location_mode_adapter );
		spLocationMode.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener(){
			@Override public void onItemSelected( AdapterView<?> parent, View view, int position, long id ){
				updateFormEnabled();
			}

			@Override public void onNothingSelected( AdapterView<?> parent ){
				updateFormEnabled();
			}
		} );


		ArrayAdapter<CharSequence> target_type_adapter = new ArrayAdapter<>(
			activity
			, android.R.layout.simple_spinner_item
		);
		target_type_adapter.setDropDownViewResource( R.layout.spinner_dropdown );

		target_type_adapter.addAll(
			activity.getString( R.string.target_type_0 ),
			activity.getString( R.string.target_type_1 ),
			activity.getString( R.string.target_type_2 )
		);
		spTargetType.setAdapter( target_type_adapter );
		spTargetType.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener(){
			@Override public void onItemSelected( AdapterView<?> parent, View view, int position, long id ){
				updateFormEnabled();
			}

			@Override public void onNothingSelected( AdapterView<?> parent ){
				updateFormEnabled();
			}
		} );

		swForceWifi.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener(){
			@Override public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ){
				updateFormEnabled();
			}
		} );
		swThumbnailAutoRotate.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener(){
			@Override public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ){
				Pref.pref(activity).edit().putBoolean (Pref.UI_THUMBNAIL_AUTO_ROTATE,isChecked).apply();
				((ActMain)activity).reloadDownloadRecord();
				updateFormEnabled();
			}
		} );
		swCopyBeforeViewSend.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener(){
			@Override public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ){
				Pref.pref(activity).edit().putBoolean (Pref.UI_COPY_BEFORE_VIEW_SEND,isChecked).apply();
				updateFormEnabled();
			}
		} );
		ui_value_load();
		folder_view_update();
	}

	@Override protected void onPageDestroy( int page_idx, View root ) throws Throwable{
		SharedPreferences.Editor e= Pref.pref(activity).edit();
		ui_value_save(e);
		e.apply();
	}

	@Override public void onClick( View view ){
		switch( view.getId() ){
		case R.id.btnFolderPicker:
			folder_pick();
			break;
		case R.id.btnSSIDPicker:
			ssid_pick();
			break;

		case R.id.btnHelpFolderPicker:
			if( Build.VERSION.SDK_INT >= LocalFile.DOCUMENT_FILE_VERSION ){
				( (ActMain) activity ).openHelp( R.layout.help_local_folder );
			}else{
				( (ActMain) activity ).openHelp( activity.getString( R.string.help_local_folder_kitkat ) );
			}
			break;
		case R.id.btnHelpTargetUrl:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_flashair_url_text ) );
			break;
		case R.id.btnIntervalHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_repeat_interval_text ) );
			break;
		case R.id.btnFileTypeHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_file_type_text ) );
			break;
		case R.id.btnLocationModeHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_location_mode ) );
			break;
		case R.id.btnLocationIntervalDesiredHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_location_interval_desired ) );
			break;
		case R.id.btnLocationIntervalMinHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_location_interval_min ) );
			break;
		case R.id.btnForceWifiHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_force_wifi ) );
			break;
		case R.id.btnSSIDHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_ssid ) );
			break;
		case R.id.btnThumbnailAutoRotateHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_thumbnail_auto_rotate ) );
			break;
		case R.id.btnCopyBeforeViewSendHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_copy_before_view_send ) );
			break;
		case R.id.btnTargetTypeHelp:
			( (ActMain) activity ).openHelp( activity.getString( R.string.help_target_type ) );
			break;


		}
	}

	// UIフォームの値を設定から読み出す
	void ui_value_load(){
		SharedPreferences pref = Pref.pref( activity );
		String sv;
		int iv;
		//
		iv = pref.getInt( Pref.UI_TARGET_TYPE, - 1 );
		if( iv >= 0 && iv < spTargetType.getCount() ) spTargetType.setSelection( iv );
		//
		sv = pref.getString( Pref.UI_FLASHAIR_URL, null );
		if( sv != null ) etURL.setText( sv );
		//
		sv = pref.getString( Pref.UI_INTERVAL, null );
		if( sv != null ) etInterval.setText( sv );
		//
		sv = pref.getString( Pref.UI_FILE_TYPE, null );
		if( sv != null ) etFileType.setText( sv );
		//
		iv = pref.getInt( Pref.UI_LOCATION_MODE, - 1 );
		if( iv >= 0 && iv < spLocationMode.getCount() ) spLocationMode.setSelection( iv );
		//
		sv = pref.getString( Pref.UI_LOCATION_INTERVAL_DESIRED, null );
		if( sv != null ) etLocationIntervalDesired.setText( sv );
		//
		sv = pref.getString( Pref.UI_LOCATION_INTERVAL_MIN, null );
		if( sv != null ) etLocationIntervalMin.setText( sv );
		//
		boolean bv = pref.getBoolean( Pref.UI_FORCE_WIFI, false );
		swForceWifi.setChecked( bv );
		//
		etSSID.setText( pref.getString( Pref.UI_SSID, "" ) );
		//
		swThumbnailAutoRotate.setChecked( pref.getBoolean( Pref.UI_THUMBNAIL_AUTO_ROTATE,  Pref.DEFAULT_THUMBNAIL_AUTO_ROTATE ) );
		//
		swCopyBeforeViewSend.setChecked( pref.getBoolean( Pref.UI_COPY_BEFORE_VIEW_SEND, false ) );

		updateFormEnabled();
	}

	private void updateFormEnabled(){
		boolean location_enabled = ( spLocationMode.getSelectedItemPosition() > 0 );
		etLocationIntervalDesired.setEnabled( location_enabled );
		etLocationIntervalMin.setEnabled( location_enabled );

		boolean force_wifi_enabled = spTargetType.getSelectedItemPosition() == 0;
		swForceWifi.setEnabled( force_wifi_enabled );

		boolean ssid_enabled = force_wifi_enabled && swForceWifi.isChecked();
		etSSID.setEnabled( ssid_enabled );
		btnSSIDPicker.setEnabled( ssid_enabled );
	}

	// UIフォームの値を設定ファイルに保存
	void ui_value_save( SharedPreferences.Editor e ){
		e
			.putInt( Pref.UI_TARGET_TYPE, spTargetType.getSelectedItemPosition() )
			.putString( Pref.UI_FLASHAIR_URL, etURL.getText().toString() )
			.putString( Pref.UI_INTERVAL, etInterval.getText().toString() )
			.putString( Pref.UI_FILE_TYPE, etFileType.getText().toString() )
			.putInt( Pref.UI_LOCATION_MODE, spLocationMode.getSelectedItemPosition() )
			.putString( Pref.UI_LOCATION_INTERVAL_DESIRED, etLocationIntervalDesired.getText().toString() )
			.putString( Pref.UI_LOCATION_INTERVAL_MIN, etLocationIntervalMin.getText().toString() )
			.putBoolean( Pref.UI_FORCE_WIFI, swForceWifi.isChecked() )
			.putString( Pref.UI_SSID, etSSID.getText().toString() )
		;
	}

	// 転送先フォルダの選択を開始
	void folder_pick(){
		if( Build.VERSION.SDK_INT >= LocalFile.DOCUMENT_FILE_VERSION ){
			@SuppressLint( "InlinedApi" ) Intent intent = new Intent( Intent.ACTION_OPEN_DOCUMENT_TREE );
			activity.startActivityForResult( intent, ActMain.REQUEST_CODE_DOCUMENT );
		}else{
			FolderPicker.open( activity, ActMain.REQUEST_FOLDER_PICKER, tvFolder.getText().toString() );

		}
	}

	// フォルダの表示を更新
	void folder_view_update(){
		String name = null;
		String sv = Pref.pref( activity ).getString( Pref.UI_FOLDER_URI, null );
		if( ! TextUtils.isEmpty( sv ) ){
			if( Build.VERSION.SDK_INT >= LocalFile.DOCUMENT_FILE_VERSION ){
				DocumentFile folder;
				folder = DocumentFile.fromTreeUri( activity, Uri.parse( sv ) );
				if( folder != null ){
					if( folder.exists() && folder.canWrite() ){
						name = folder.getName();
					}
				}
			}else{
				name = sv;
			}
		}

		tvFolder.setText( TextUtils.isEmpty( name )
			? activity.getString( R.string.not_selected ) : name );
	}

	private void ssid_pick(){
		SSIDPicker.open( activity, ActMain.REQUEST_SSID_PICKER );

	}

	public void ssid_view_update(){
		SharedPreferences pref = Pref.pref( activity );
		etSSID.setText( pref.getString( Pref.UI_SSID, "" ) );
	}
}
