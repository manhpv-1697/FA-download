package jp.juggler.fadownloader;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import it.sephiroth.android.library.exif2.ExifInterface;
import jp.juggler.fadownloader.targets.FlashAir;
import jp.juggler.fadownloader.targets.PentaxKP;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadWorker extends WorkerBase{

	public static final boolean RECORD_QUEUED_STATE = false;


	public interface Callback{

		void releaseWakeLock();

		void acquireWakeLock();

		void onThreadStart();

		void onThreadEnd( boolean allow_stop_service );

		Location getLocation();
	}

	final DownloadService service;
	public final Callback callback;

	public final boolean repeat;
	public String flashair_url;
	public final String folder_uri;
	public final int interval;
	final String file_type;
	final LogWriter log;
	public final ArrayList<Pattern> file_type_list;
	final boolean force_wifi;
	final String ssid;
	public final int target_type;

	public DownloadWorker( DownloadService service, Intent intent, Callback callback ){
		this.service = service;
		this.callback = callback;
		this.log = new LogWriter( service );

		log.i( R.string.thread_ctor_params );
		this.repeat = intent.getBooleanExtra( DownloadService.EXTRA_REPEAT, false );
		this.flashair_url = intent.getStringExtra( DownloadService.EXTRA_TARGET_URL );
		this.folder_uri = intent.getStringExtra( DownloadService.EXTRA_LOCAL_FOLDER );
		this.interval = intent.getIntExtra( DownloadService.EXTRA_INTERVAL, 86400 );
		this.file_type = intent.getStringExtra( DownloadService.EXTRA_FILE_TYPE );
		this.force_wifi = intent.getBooleanExtra( DownloadService.EXTRA_FORCE_WIFI, false );
		this.ssid = intent.getStringExtra( DownloadService.EXTRA_SSID );
		this.target_type = intent.getIntExtra( DownloadService.EXTRA_TARGET_TYPE, 0 );

		LocationTracker.Setting location_setting = new LocationTracker.Setting();
		location_setting.interval_desired = intent.getLongExtra( DownloadService.EXTRA_LOCATION_INTERVAL_DESIRED, LocationTracker.DEFAULT_INTERVAL_DESIRED );
		location_setting.interval_min = intent.getLongExtra( DownloadService.EXTRA_LOCATION_INTERVAL_MIN, LocationTracker.DEFAULT_INTERVAL_MIN );
		location_setting.mode = intent.getIntExtra( DownloadService.EXTRA_LOCATION_MODE, LocationTracker.DEFAULT_MODE );

		Pref.pref( service ).edit()
			.putBoolean( Pref.WORKER_REPEAT, repeat )
			.putInt( Pref.WORKER_TARGET_TYPE, target_type )
			.putString( Pref.WORKER_FLASHAIR_URL, flashair_url )
			.putString( Pref.WORKER_FOLDER_URI, folder_uri )
			.putInt( Pref.WORKER_INTERVAL, interval )
			.putString( Pref.WORKER_FILE_TYPE, file_type )
			.putLong( Pref.WORKER_LOCATION_INTERVAL_DESIRED, location_setting.interval_desired )
			.putLong( Pref.WORKER_LOCATION_INTERVAL_MIN, location_setting.interval_min )
			.putInt( Pref.WORKER_LOCATION_MODE, location_setting.mode )
			.putBoolean( Pref.WORKER_FORCE_WIFI, force_wifi )
			.putString( Pref.WORKER_SSID, ssid )
			.apply();

		file_type_list = file_type_parse();

		service.wifi_tracker.updateSetting( force_wifi, ssid, target_type, flashair_url );

		service.location_tracker.updateSetting( location_setting );
	}

	public DownloadWorker( DownloadService service, String cause, Callback callback ){
		this.service = service;
		this.callback = callback;
		this.log = new LogWriter( service );

		log.i( R.string.thread_ctor_restart, cause );
		SharedPreferences pref = Pref.pref( service );
		this.repeat = pref.getBoolean( Pref.WORKER_REPEAT, false );
		this.flashair_url = pref.getString( Pref.WORKER_FLASHAIR_URL, null );
		this.folder_uri = pref.getString( Pref.WORKER_FOLDER_URI, null );
		this.interval = pref.getInt( Pref.WORKER_INTERVAL, 86400 );
		this.file_type = pref.getString( Pref.WORKER_FILE_TYPE, null );

		this.force_wifi = pref.getBoolean( Pref.WORKER_FORCE_WIFI, false );
		this.ssid = pref.getString( Pref.WORKER_SSID, null );
		this.target_type = pref.getInt( Pref.WORKER_TARGET_TYPE, 0 );

		LocationTracker.Setting location_setting = new LocationTracker.Setting();
		location_setting.interval_desired = pref.getLong( Pref.WORKER_LOCATION_INTERVAL_DESIRED, LocationTracker.DEFAULT_INTERVAL_DESIRED );
		location_setting.interval_min = pref.getLong( Pref.WORKER_LOCATION_INTERVAL_MIN, LocationTracker.DEFAULT_INTERVAL_MIN );
		location_setting.mode = pref.getInt( Pref.WORKER_LOCATION_MODE, LocationTracker.DEFAULT_MODE );

		file_type_list = file_type_parse();

		service.wifi_tracker.updateSetting( force_wifi, ssid, target_type, flashair_url );

		service.location_tracker.updateSetting( location_setting );
	}

	public final HTTPClient client = new HTTPClient( 30000, 4, "HTTP Client", this );

	public boolean cancel( String reason ){
		boolean rv = super.cancel( reason );
		if( rv ) log.i( R.string.thread_cancelled, reason );
		try{
			client.cancel( log );
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
		return rv;
	}

	public static final Pattern reJPEG = Pattern.compile( "\\.jp(g|eg?)\\z", Pattern.CASE_INSENSITIVE );
	static final Pattern reFileType = Pattern.compile( "(\\S+)" );

	private ArrayList<Pattern> file_type_parse(){
		ArrayList<Pattern> list = new ArrayList<>();
		Matcher m = reFileType.matcher( file_type );
		while( m.find() ){
			try{
				String spec = m.group( 1 ).replaceAll( "(\\W)", "\\\\$1" ).replaceAll( "\\\\\\*", ".*?" );
				list.add( Pattern.compile( spec + "\\z", Pattern.CASE_INSENSITIVE ) );
			}catch( Throwable ex ){
				ex.printStackTrace();
				log.e( R.string.file_type_parse_error, m.group( 1 ), ex.getClass().getSimpleName(), ex.getMessage() );
			}
		}
		return list;
	}

	public static class ErrorAndMessage{

		public boolean bError;
		public String message;

		ErrorAndMessage( boolean b, String s ){
			bError = b;
			message = s;
		}
	}

	@NonNull public ErrorAndMessage updateFileLocation( final Location location, LocalFile file ){
		ErrorAndMessage em = null;
		try{

			LocalFile tmp_path = new LocalFile( file.getParent(), "tmp-" + currentThread().getId() + "-" + android.os.Process.myPid() + "-" + file.getName() );
			if( ! tmp_path.prepareFile( log, true ) ){
				return new ErrorAndMessage( true, "file creation failed." );
			}

			try{
				OutputStream os = tmp_path.openOutputStream( service );
				try{
					InputStream is = file.openInputStream( service );
					try{
						ExifInterface.modifyExifTag( is, ExifInterface.Options.OPTION_ALL
							, os, new ExifInterface.ModifyExifTagCallback(){
								@Override public boolean modify( ExifInterface ei ){
									String s = ei.getLatitude();
									if( s != null ) return false;

									double latitude = location.getLatitude();
									double longitude = location.getLongitude();

									ei.addGpsTags( latitude, longitude );

									return true;
								}
							} );
					}finally{
						try{
							if( is != null ) is.close();
						}catch( Throwable ignored ){
						}

					}
				}finally{
					try{
						if( os != null ) os.close();
					}catch( Throwable ignored ){
					}
				}
			}catch( ExifInterface.ModifyExifTagFailedException ignored ){
				// 既にEXIF情報があった
				em = new ErrorAndMessage( false, "既に位置情報が含まれています" );
			}catch( Throwable ex ){
				ex.printStackTrace();
				log.e( ex, "exif mangling failed." );

				// 変更失敗
				em = new ErrorAndMessage( true, LogWriter.formatError( ex, "exif mangling failed." ) );
			}

			if( em != null ){
				tmp_path.delete();
				return em;
			}

			// 更新後の方がファイルが小さいことがあるのか？
			if( tmp_path.length( log, false ) < file.length( log, false ) ){
				log.e( "EXIF付与したファイルの方が小さい!付与前後のファイルを残しておく" );
				// この場合両方のファイルを残しておく
				return new ErrorAndMessage( true, "EXIF付与したファイルの方が小さい" );
			}

			if( ! file.delete() || ! tmp_path.renameTo( file.getName() ) ){
				log.e( "EXIF追加後のファイル操作に失敗" );
				return new ErrorAndMessage( true, "EXIF追加後のファイル操作に失敗" );
			}

			log.i( "%s に位置情報を付与しました", file.getName() );
			return new ErrorAndMessage( false, "embedded" );

		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "exif mangling failed." );
			return new ErrorAndMessage( true, LogWriter.formatError( ex, "exif mangling failed." ) );
		}
	}

	boolean isForcedSSID(){
		if( ! force_wifi ) return true;
		WifiManager wm = (WifiManager) service.getApplicationContext().getSystemService( Context.WIFI_SERVICE );
		WifiInfo wi = wm.getConnectionInfo();
		String current_ssid = wi.getSSID().replace( "\"", "" );
		return ! TextUtils.isEmpty( current_ssid ) && current_ssid.equals( this.ssid );
	}

	@SuppressWarnings( "deprecation" ) public Object getWiFiNetwork(){
		ConnectivityManager cm = (ConnectivityManager) service.getApplicationContext().getSystemService( Context.CONNECTIVITY_SERVICE );
		if( Build.VERSION.SDK_INT >= 21 ){
			for( Network n : cm.getAllNetworks() ){
				NetworkInfo info = cm.getNetworkInfo( n );
				if( info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI ){
					if( isForcedSSID() ) return n;
				}
			}
		}else{
			for( NetworkInfo info : cm.getAllNetworkInfo() ){
				if( info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI ){
					if( isForcedSSID() ) return info;
					return info;
				}
			}
		}
		return null;
	}

	public void checkHostError(){
		if( client.last_error.contains( "UnknownHostException" ) ){
			client.last_error = service.getString( R.string.target_host_error );
			cancel( service.getString( R.string.target_host_error_short ) );
		}else if( client.last_error.contains( "ENETUNREACH" ) ){
			client.last_error = service.getString( R.string.target_unreachable );
			cancel( service.getString( R.string.target_unreachable ) );
		}
	}

	public final ContentValues cv = new ContentValues();
	public boolean file_error = false;

	public boolean allow_stop_service = false;
	public LinkedList<QueueItem> job_queue = null;

	public AtomicLong queued_byte_count_max = new AtomicLong();

	AtomicLong queued_file_count = new AtomicLong();
	AtomicLong queued_byte_count = new AtomicLong();

	private final AtomicReference<String> _status = new AtomicReference<>( "?" );

	public void setStatus( boolean bShowQueueCount, String s ){
		if( ! bShowQueueCount ){
			queued_file_count.set( 0L );
			queued_byte_count.set( 0L );
		}else{
			long fc = 0L;
			long bc = 0L;
			if( job_queue != null ){
				for( QueueItem item : job_queue ){
					if( item.is_file ){
						++ fc;
						bc += item.size;
					}
				}
			}
			queued_file_count.set( fc );
			queued_byte_count.set( bc );
			if( bc > 0 && queued_byte_count_max.get() == Long.MAX_VALUE ){
				queued_byte_count_max.set( bc );
			}
		}
		_status.set( s );
	}

	public String getStatus(){
		long fc = queued_file_count.get();
		String s = _status.get();
		if( fc > 0L ){
			long bc = queued_byte_count.get();
			long bcm = queued_byte_count_max.get();
			return s + String.format( "\n%s %d%%, %s %dfile %sbyte"
				, service.getString( R.string.progress )
				, bcm <= 0 ? 0 : 100L * ( bcm - bc ) / bcm
				, service.getString( R.string.remain )
				, fc
				, Utils.formatBytes( bc )
			);
		}else{
			return s;
		}
	}

	@SuppressWarnings( "ConstantConditions" ) @Override public void run(){
		setStatus( false, service.getString( R.string.thread_start ) );
		callback.onThreadStart();

		switch( target_type ){
		default:
			log.e( "unsupported target type %s", target_type );
			break;
		case Pref.TARGET_TYPE_FLASHAIR_AP:
		case Pref.TARGET_TYPE_FLASHAIR_STA:
			new FlashAir( service, this ).run();
			break;
		case Pref.TARGET_TYPE_PENTAX_KP:
			new PentaxKP( service, this ).run();
			break;

		}

		// 未取得状態のファイルを履歴から消す
		if( DownloadWorker.RECORD_QUEUED_STATE ){
			service.getContentResolver().delete(
				DownloadRecord.meta.content_uri
				, DownloadRecord.COL_STATE_CODE + "=?"
				, new String[]{
					Integer.toString( DownloadRecord.STATE_QUEUED )
				}
			);
		}
		setStatus( false, service.getString( R.string.thread_end ) );
		callback.releaseWakeLock();
		callback.onThreadEnd( allow_stop_service );
	}

	public void record(
		String file_name
		, String remote_path
		, String local_uri
		, long size
		, long lap_time
		, int state
		, String state_message
	){
		if( ! DownloadWorker.RECORD_QUEUED_STATE ){
			if( state == DownloadRecord.STATE_QUEUED ) return;
		}
		DownloadRecord.insert(
			service.getContentResolver()
			, cv
			, file_name
			, remote_path
			, local_uri
			, state
			, state_message
			, lap_time
			, size
		);
	}

}
