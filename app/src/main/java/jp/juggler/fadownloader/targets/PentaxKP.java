package jp.juggler.fadownloader.targets;

import jp.juggler.fadownloader.CancelChecker;
import jp.juggler.fadownloader.DownloadRecord;
import jp.juggler.fadownloader.DownloadService;
import jp.juggler.fadownloader.DownloadWorker;

import android.location.Location;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONArray;
import org.json.JSONObject;

import jp.juggler.fadownloader.HTTPClientReceiver;
import jp.juggler.fadownloader.LocalFile;
import jp.juggler.fadownloader.LogWriter;
import jp.juggler.fadownloader.Pref;
import jp.juggler.fadownloader.QueueItem;
import jp.juggler.fadownloader.R;
import jp.juggler.fadownloader.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PentaxKP{

	final DownloadService service;
	final DownloadWorker thread;
	final LogWriter log;

	public PentaxKP( DownloadService service, DownloadWorker thread ){
		this.service = service;
		this.thread = thread;
		this.log = service.log;
	}

	static final Pattern reDateTime = Pattern.compile("(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)\\D+(\\d+)");

	boolean loadFolder( Object network ){
		String cgi_url = thread.flashair_url + "v1/photos";
		byte[] data = thread.client.getHTTP( log, network, cgi_url );
		if( thread.isCancelled() ) return false;
		if( data == null ){
			thread.checkHostError();
			log.e( R.string.folder_list_failed, "/", cgi_url, thread.client.last_error );
			return false;
		}
		// job_queue.add( new Item( "/", new LocalFile( service, folder_uri ), false, 0L ) );

		final byte[] buf = new byte[ 1 ];
		GregorianCalendar calendar = new GregorianCalendar( TimeZone.getDefault() );

		try{
			JSONObject info = new JSONObject( Utils.decodeUTF8( data ) );
			if( info.optInt( "errCode", 0 ) != 200 ){
				throw new RuntimeException( "server's errMsg:" + info.optString( "errMsg" ) );
			}
			JSONArray root_dir = info.optJSONArray( "dirs" );
			if( root_dir == null ){
				log.e( "missing root folder." );
				return false;
			}
			LocalFile local_root = new LocalFile( service, thread.folder_uri );
			thread.job_queue = new LinkedList<>();
			for( int i = 0, ie = root_dir.length() ; i < ie ; ++ i ){
				if( thread.isCancelled() ) return false;

				Object o = root_dir.opt( i );
				if( ! ( o instanceof JSONObject ) ) continue;
				JSONObject subdir = (JSONObject) o;

				String sub_dir_name = subdir.optString( "name", null );
				if( TextUtils.isEmpty( sub_dir_name ) ) continue;

				LocalFile sub_dir_local = new LocalFile( local_root, sub_dir_name );
				JSONArray files = subdir.optJSONArray( "files" );
				if( files == null ) continue;

				for( int j = 0, je = files.length() ; j < je ; ++ j ){
					if( thread.isCancelled() ) return false;
					String fname = files.optString( j );
					if( TextUtils.isEmpty( fname ) ) continue;
					// file type matching
					for( Pattern re : thread.file_type_list ){
						if( thread.isCancelled() ) return false;
						if( ! re.matcher( fname ).find() ) continue;
						// マッチした

						String remote_path = "/" + sub_dir_name + "/" + fname;
						LocalFile local_file = new LocalFile( sub_dir_local, fname );

						long size = 1L; // PentaxのAPIだとこの時点ではサイズ不明

						// ローカルにあるファイルのサイズが1以上ならスキップする
						final long local_size = local_file.length( log, false );
						if( local_size >= 1L ) continue;

						// 進捗表示用のファイルサイズは超適当
						size = 1000000L;


//						// ダウンロード進捗のためにサイズを調べる
//						try{
//							log.d("get file size for %s",remote_path);
//							final String get_url = thread.flashair_url + "v1/photos" + Uri.encode( remote_path, "/_" ) + "?size=full";
//							data = thread.client.getHTTP( log, network, get_url, new HTTPClientReceiver(){
//								public byte[] onHTTPClientStream( LogWriter log, CancelChecker cancel_checker, InputStream in, int content_length ){
//									return buf;
//								}
//							} );
//							if( thread.isCancelled() )return false;
//							if( data == null){
//								thread.checkHostError();
//								log.e( "can not get file size. %s",thread.client.last_error );
//							}else{
//								//// thread.client.dump_res_header( log );
//								String sv = thread.client.getHeaderString( "Content-Length" ,null);
//								if( !TextUtils.isEmpty( sv )){
//									size = Long.parseLong( sv,10 );
//								}
//							}
//						}catch( Throwable ex ){
//							log.e( ex, "can not get file size." );
//						}

						long time = 0L;
//						try{
//							log.d("get file time for %s",remote_path);
//							final String get_url = thread.flashair_url + "v1/photos" + Uri.encode( remote_path, "/_" ) + "/info";
//							data = thread.client.getHTTP( log, network, get_url );
//							if( thread.isCancelled() ) return false;
//							if( data == null){
//								thread.checkHostError();
//								log.e( "can not get file time. %s",thread.client.last_error );
//							}else{
//								JSONObject file_info = new JSONObject( Utils.decodeUTF8( data ) );
//								if( file_info.optInt( "errCode", 0 ) != 200 ){
//									throw new RuntimeException( "server's errMsg:" + info.optString( "errMsg" ) );
//								}
//								Matcher matcher = reDateTime.matcher(file_info.optString("datetime",""));
//								if( !matcher. find() ){
//									log.e( "can not get file time. missing 'datetime' property.");
//								}else{
//									int y = Integer.parseInt (matcher.group(1),10);
//									int m = Integer.parseInt (matcher.group(2),10);
//									int d =Integer.parseInt (matcher.group(3),10);
//									int h = Integer.parseInt (matcher.group(4),10);
//									int min =Integer.parseInt (matcher.group(5),10);
//									int s =Integer.parseInt (matcher.group(6),10);
//									log.f( "time=%s,%s,%s,%s,%s,%s", y, m, d, h, min, s );
//									calendar.set( y, m, d, h, min, s );
//									calendar.set( Calendar.MILLISECOND, 500 );
//									time = calendar.getTimeInMillis();
//								}
//							}
//						}catch( Throwable ex ){
//							log.e( ex, "can not get file time." );
//						}

						QueueItem item = new QueueItem( fname,remote_path, local_file, size ,time );

						// ファイルはキューの末尾に追加
						thread.job_queue.addLast(item);

						thread.record(
							item,0L
							, DownloadRecord.STATE_QUEUED
							, "queued."
						);

						break;
					}
				}
			}
			log.i( "%s files queued.", thread.job_queue.size() );
			return true;
		}catch( Throwable ex ){
			log.e( ex, R.string.remote_file_list_parse_error );
		}

		thread.job_queue = null;
		return false;
	}

	void loadFile( Object network, QueueItem item ){
		long time_start = SystemClock.elapsedRealtime();
		final String remote_path = item.remote_path;
		final LocalFile local_file = item.local_file;
		final String file_name = local_file.getName();

		try{

			if( ! local_file.prepareFile( log, true ) ){
				log.e( "%s//%s :skip. can not prepare local file.", item.remote_path, file_name );
				thread.record(item,SystemClock.elapsedRealtime() - time_start
					, DownloadRecord.STATE_LOCAL_FILE_PREPARE_ERROR
					, "can not prepare local file."
				);
				return;
			}

			final String get_url = thread.flashair_url + "v1/photos" + Uri.encode( remote_path, "/_" ) + "?size=full";
			byte[] data = thread.client.getHTTP( log, network, get_url, new HTTPClientReceiver(){

				final byte[] buf = new byte[ 2048 ];

				public byte[] onHTTPClientStream( LogWriter log, CancelChecker cancel_checker, InputStream in, int content_length ){
					try{

						OutputStream os = local_file.openOutputStream( service );
						if( os == null ){
							log.e( "cannot open local output file." );
						}else{
							try{
								for( ; ; ){
									if( cancel_checker.isCancelled() ){
										return null;
									}
									int delta = in.read( buf );
									if( delta <= 0 ) break;
									os.write( buf, 0, delta );
								}
								return buf;
							}finally{
								try{
									os.close();
								}catch( Throwable ignored ){
								}
							}
						}
					}catch( Throwable ex ){
						log.e( "HTTP read error. %s:%s"
							, ex.getClass().getSimpleName()
							, ex.getMessage()
						);
					}
					return null;
				}
			} );

			if( thread.isCancelled() ){
				local_file.delete();
				thread.record(
					item, SystemClock.elapsedRealtime() - time_start
					, DownloadRecord.STATE_CANCELLED
					, "download cancelled."
				);
			}else if( data == null ){
				local_file.delete();

				thread.checkHostError();
				log.e( "FILE %s :HTTP error %s", file_name, thread.client.last_error );

				thread.file_error = true;
				thread.record( item, SystemClock.elapsedRealtime() - time_start
					, DownloadRecord.STATE_DOWNLOAD_ERROR
					, thread.client.last_error
				);
			}else{
				thread.afterDownload( item, SystemClock.elapsedRealtime() - time_start );
			}

		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "error." );

			local_file.delete();

			thread.file_error = true;
			thread.record( item, SystemClock.elapsedRealtime() - time_start
				, DownloadRecord.STATE_DOWNLOAD_ERROR
				, thread.client.last_error
			);
		}

	}

	AtomicLong mCameraUpdateTime = new AtomicLong( 0L );
	AtomicLong mLastBusyTime = new AtomicLong( 0L );
	AtomicBoolean mIsCameraBusy = new AtomicBoolean( false );
	AtomicLong mLastFileListed = new AtomicLong( 0L );

	WebSocket ws_client;
	final WebSocketAdapter ws_listener = new WebSocketAdapter(){

		@Override public void onUnexpectedError( WebSocket websocket, WebSocketException ex ) throws Exception{
			super.onUnexpectedError( websocket, ex );
			log.e( ex, "WebSocket onUnexpectedError" );
		}

		@Override public void onError( WebSocket websocket, WebSocketException ex ) throws Exception{
			super.onError( websocket, ex );
			log.e( ex, "WebSocket onError" );
		}

		@Override public void onConnectError( WebSocket websocket, WebSocketException ex ) throws Exception{
			super.onConnectError( websocket, ex );
			log.e( ex, "WebSocket onConnectError();" );
		}

		@Override public void onTextMessageError( WebSocket websocket, WebSocketException ex, byte[] data ) throws Exception{
			super.onTextMessageError( websocket, ex, data );
			log.e( ex, "WebSocket onTextMessageError" );
		}

		@Override public void onDisconnected( WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer ) throws Exception{
			super.onDisconnected( websocket, serverCloseFrame, clientCloseFrame, closedByServer );
			log.d( "WebSocket onDisconnects" );
		}

		@Override public void onConnected( WebSocket websocket, Map<String, List<String>> headers ) throws Exception{
			super.onConnected( websocket, headers );
			log.d( "WebSocket onConnect();" );
		}

		boolean bBusy = false;

		@SuppressWarnings( "StatementWithEmptyBody" )
		@Override public void onTextMessage( WebSocket websocket, String text ) throws Exception{
			super.onTextMessage( websocket, text );
			try{
				JSONObject info = new JSONObject( text );
				if( 200 == info.optInt( "errCode", 0 ) ){
					String changed = info.optString( "changed" );
					if( "camera".equals( changed ) ){
						// 何もしていない状態でも定期的に発生する
					}else if( "cameraDirect".equals( changed ) ){
						bBusy = info.optBoolean( "capturing", false )
							|| ! "idle".equals( info.optString( "stateStill" ) )
							|| ! "idle".equals( info.optString( "stateMovie" ) )
						;
						if( bBusy && mIsCameraBusy.compareAndSet( false, true ) ){
							// busy になった
							mLastBusyTime.set( System.currentTimeMillis() );
						}else if( ! bBusy && mIsCameraBusy.compareAndSet( true, false ) ){
							// busyではなくなった
							mLastBusyTime.set( System.currentTimeMillis() );
						}
					}else if( "storage".equals( changed ) ){
						mLastFileListed.set( 0L );
						mCameraUpdateTime.set( System.currentTimeMillis() );
						thread.notifyEx();
					}else{
						log.d( "WebSocket onTextMessage %s", text );
					}
				}
			}catch( Throwable ex ){
				ex.printStackTrace();
				log.e( ex, "WebSocket message handling error." );
			}
		}
	};

	public void run(){

		while( ! thread.isCancelled() ){

			// WakeLockやWiFiLockを確保
			thread.callback.acquireWakeLock();

			// 通信状態の確認
			thread.setStatus( false, service.getString( R.string.network_check ) );
			long network_check_start = SystemClock.elapsedRealtime();
			Object network = null;
			while( ! thread.isCancelled() ){
				network = thread.getWiFiNetwork();
				if( network != null ) break;

				// 一定時間待機してもダメならスレッドを停止する
				// 通信状態変化でまた起こされる
				long er_now = SystemClock.elapsedRealtime();
				if( er_now - network_check_start >= 60 * 1000L ){
					// Pref.pref( service ).edit().putLong( Pref.LAST_IDLE_START, System.currentTimeMillis() ).apply();
					thread.job_queue = null;
					thread.cancel( service.getString( R.string.network_not_good ) );
					break;
				}

				// 少し待って再確認
				thread.waitEx( 10000L );
			}
			if( thread.isCancelled() ) break;

			// WebSocketが閉じられていたら後処理をする
			if( ws_client != null && ws_client.getState() == WebSocketState.CLOSED ){
				thread.setStatus( false, "WebSocket closing" );
				ws_client.removeListener( ws_listener );
				ws_client.disconnect();
				ws_client = null;
			}
			if( thread.isCancelled() ) break;

			// WebSocketがなければ開く
			if( ws_client == null ){
				thread.setStatus( false, "WebSocket creating" );
				try{
					WebSocketFactory factory = new WebSocketFactory();
					ws_client = factory.createSocket( thread.flashair_url + "v1/changes", 5000 );
					ws_client.addListener( ws_listener );
					ws_client.connect();
					thread.waitEx( 2000L );
				}catch( OpeningHandshakeException ex ){
					ex.printStackTrace();
					log.e( ex, "WebSocket connection failed(1)." );
					thread.waitEx( 5000L );
					continue;
				}catch( WebSocketException ex ){
					ex.printStackTrace();
					log.e( ex, "WebSocket connection failed(2)." );
					thread.waitEx( 5000L );
					continue;
				}catch( IOException ex ){
					ex.printStackTrace();
					log.e( ex, "WebSocket connection failed(3)." );
					thread.waitEx( 5000L );
					continue;
				}
			}
			if( thread.isCancelled() ) break;

			long now = System.currentTimeMillis();
			long remain;
			if( mIsCameraBusy.get() ){
				// ビジー状態なら待機を続ける
				thread.setStatus( false, "camera is busy." );
				remain = 2000L;
			}else if( now - mLastBusyTime.get() < 2000L ){
				// ビジーが終わっても数秒は待機を続ける
				thread.setStatus( false, "camera was busy." );
				remain = mLastBusyTime.get() + 2000L - now;
			}else if( now - mCameraUpdateTime.get() < 2000L ){
				// カメラが更新された後数秒は待機する
				thread.setStatus( false, "waiting camera storage." );
				remain = mCameraUpdateTime.get() + 2000L - now;
			}else if( thread.job_queue != null ){
				// キューにある項目を処理する
				remain = 0L;
			}else{
				// キューがカラなら、最後にファイル一覧を取得した時刻から一定は待つ
				remain = mLastFileListed.get() + thread.interval * 1000L - now;
				thread.setStatus( false, service.getString( R.string.wait_short, Utils.formatTimeDuration( remain ) ) );
			}
			if( remain > 0 ){
				thread.waitEx( remain < 1000L ? remain : 1000L );
				continue;
			}

			// ファイルスキャンの開始
			if( thread.job_queue == null ){
				Pref.pref( service ).edit().putLong( Pref.LAST_IDLE_START, System.currentTimeMillis() ).apply();

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

				// フォルダスキャン開始
				thread.setStatus( false, service.getString( R.string.remote_file_listing ) );
				thread.job_queue = new LinkedList<>();
				if( ! loadFolder( network ) ){
					thread.job_queue = null;
					continue;
				}
				mLastFileListed.set( System.currentTimeMillis() );
				thread.file_error = false;
				thread.queued_byte_count_max.set( Long.MAX_VALUE );
			}

			// ファイルスキャンの終了
			if( thread.job_queue.isEmpty() ){
				thread.job_queue = null;
				thread.setStatus( false, service.getString( R.string.file_scan_completed ) );
				if( ! thread.file_error ){
					log.i( "ファイルスキャン完了" );
					if( ! thread.repeat ){
						Pref.pref( service ).edit().putInt( Pref.LAST_MODE, Pref.LAST_MODE_STOP ).apply();
						thread.cancel( service.getString( R.string.repeat_off ) );
						thread.allow_stop_service = true;
					}
				}
				continue;
			}

			try{
				final QueueItem head = thread.job_queue.getFirst();
				if( head.is_file ){
					// キューから除去するまえに残りサイズを計算したい
					thread.setStatus( true, service.getString( R.string.download_file, head.remote_path ) );
					thread.job_queue.removeFirst();
					loadFile( network, head );
				}else{
					thread.setStatus( false, service.getString( R.string.progress_folder, head.remote_path ) );
					thread.job_queue.removeFirst();

				}
			}catch( Throwable ex ){
				ex.printStackTrace();
				log.e( ex, "error." );
			}
		}

		// WebSocketの解除
		if( ws_client != null ){
			ws_client.removeListener( ws_listener );
			ws_client.disconnect();
			ws_client = null;
		}

	}
}
