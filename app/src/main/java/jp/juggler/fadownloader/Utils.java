package jp.juggler.fadownloader;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.net.wifi.SupplicantState;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseBooleanArray;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.text.DecimalFormat;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Utils{

	@SuppressLint( "DefaultLocale" )
	public static String formatTimeDuration( long t ){
		StringBuilder sb = new StringBuilder();
		long n;
		// day
		n = t / 86400000L;
		if( n > 0 ){
			sb.append( String.format( Locale.JAPAN, "%dd", n ) );
			t -= n * 86400000L;
		}
		// h
		n = t / 3600000L;
		if( n > 0 || sb.length() > 0 ){
			sb.append( String.format( Locale.JAPAN, "%dh", n ) );
			t -= n * 3600000L;
		}
		// m
		n = t / 60000L;
		if( n > 0 || sb.length() > 0 ){
			sb.append( String.format( Locale.JAPAN, "%dm", n ) );
			t -= n * 60000L;
		}
		// s
		float f = t / 1000f;
		sb.append( String.format( Locale.JAPAN, "%.03fs", f ) );

		return sb.toString();
	}

	static DecimalFormat bytes_format = new DecimalFormat( "#,###" );

	public static String formatBytes( long t ){
		return bytes_format.format( t );

//		StringBuilder sb = new StringBuilder();
//		long n;
//		// giga
//		n = t / 1000000000L;
//		if( n > 0 ){
//			sb.append( String.format( Locale.JAPAN, "%dg", n ) );
//			t -= n * 1000000000L;
//		}
//		// Mega
//		n = t / 1000000L;
//		if( sb.length() > 0 ){
//			sb.append( String.format( Locale.JAPAN, "%03dm", n ) );
//			t -= n * 1000000L;
//		}else if( n > 0 ){
//			sb.append( String.format( Locale.JAPAN, "%dm", n ) );
//			t -= n * 1000000L;
//		}
//		// kilo
//		n = t / 1000L;
//		if( sb.length() > 0 ){
//			sb.append( String.format( Locale.JAPAN, "%03dk", n ) );
//			t -= n * 1000L;
//		}else if( n > 0 ){
//			sb.append( String.format( Locale.JAPAN, "%dk", n ) );
//			t -= n * 1000L;
//		}
//		// remain
//		if( sb.length() > 0 ){
//			sb.append( String.format( Locale.JAPAN, "%03d", t ) );
//		}else if( n > 0 ){
//			sb.append( String.format( Locale.JAPAN, "%d", t ) );
//		}
//
//		return sb.toString();
	}

	public static PendingIntent createAlarmPendingIntent( Context context ){
		Intent i = new Intent( context.getApplicationContext(), Receiver1.class );
		i.setAction( Receiver1.ACTION_ALARM );
		return PendingIntent.getBroadcast( context.getApplicationContext(), 0, i, 0 );
	}

	// 文字列とバイト列の変換
	public static byte[] encodeUTF8( String str ){
		try{
			return str.getBytes( "UTF-8" );
		}catch( Throwable ex ){
			return null; // 入力がnullの場合のみ発生
		}
	}

	// 文字列とバイト列の変換
	public static String decodeUTF8( byte[] data ){
		try{
			return new String( data, "UTF-8" );
		}catch( Throwable ex ){
			return null; // 入力がnullの場合のみ発生
		}
	}

	// 文字列と整数の変換
	public static int parse_int( String v, int defval ){
		try{
			return Integer.parseInt( v, 10 );
		}catch( Throwable ex ){
			return defval;
		}
	}

	static final char[] hex = new char[]{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static void addHex( StringBuilder sb, byte b ){
		sb.append( hex[ ( b >> 4 ) & 15 ] );
		sb.append( hex[ ( b ) & 15 ] );
	}

	public static int hex2int( int c ){
		switch( c ){
		default:
			return 0;
		case '0':
			return 0;
		case '1':
			return 1;
		case '2':
			return 2;
		case '3':
			return 3;
		case '4':
			return 4;
		case '5':
			return 5;
		case '6':
			return 6;
		case '7':
			return 7;
		case '8':
			return 8;
		case '9':
			return 9;
		case 'a':
			return 0xa;
		case 'b':
			return 0xb;
		case 'c':
			return 0xc;
		case 'd':
			return 0xd;
		case 'e':
			return 0xe;
		case 'f':
			return 0xf;
		case 'A':
			return 0xa;
		case 'B':
			return 0xb;
		case 'C':
			return 0xc;
		case 'D':
			return 0xd;
		case 'E':
			return 0xe;
		case 'F':
			return 0xf;
		}
	}

	// 16進ダンプ
	public static String encodeHex( byte[] data ){
		if( data == null ) return null;
		StringBuilder sb = new StringBuilder();
		for( byte b : data ){
			addHex( sb, b );
		}
		return sb.toString();
	}

	public static byte[] encodeSHA256( byte[] src ){
		try{
			MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
			digest.reset();
			return digest.digest( src );
		}catch( NoSuchAlgorithmException e1 ){
			return null;
		}
	}

	public static String encodeBase64Safe( byte[] src ){
		try{
			return Base64.encodeToString( src, Base64.URL_SAFE );
		}catch( Throwable ex ){
			return null;
		}
	}

	public static String url2name( String url ){
		if( url == null ) return null;
		return encodeBase64Safe( encodeSHA256( encodeUTF8( url ) ) );
	}

//	public static String name2url(String entry) {
//		if(entry==null) return null;
//		byte[] b = new byte[entry.length()/2];
//		for(int i=0,ie=b.length;i<ie;++i){
//			b[i]= (byte)((hex2int(entry.charAt(i*2))<<4)| hex2int(entry.charAt(i*2+1)));
//		}
//		return decodeUTF8(b);
//	}

	///////////////////////////////////////////////////

	// MD5ハッシュの作成
	public static String digestMD5( String s ){
		if( s == null ) return null;
		try{
			MessageDigest md = MessageDigest.getInstance( "MD5" );
			md.reset();
			return encodeHex( md.digest( s.getBytes( "UTF-8" ) ) );
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
		return null;
	}

	/////////////////////////////////////////////

	static HashMap<Character, String> taisaku_map = new HashMap<>();
	static SparseBooleanArray taisaku_map2 = new SparseBooleanArray();

	static void _taisaku_add_string( String z, String h ){
		for( int i = 0, e = z.length() ; i < e ; ++ i ){
			char zc = z.charAt( i );
			taisaku_map.put( zc, "" + Character.toString( h.charAt( i ) ) );
			taisaku_map2.put( (int) zc, true );
		}
	}

	static{
		taisaku_map = new HashMap<>();
		taisaku_map2 = new SparseBooleanArray();

		// tilde,wave dash,horizontal ellipsis,minus sign
		_taisaku_add_string(
			"\u2073\u301C\u22EF\uFF0D"
			, "\u007e\uFF5E\u2026\u2212"
		);
		// zenkaku to hankaku
		_taisaku_add_string(
			"　！”＃＄％＆’（）＊＋，－．／０１２３４５６７８９：；＜＝＞？＠ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ［］＾＿｀ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ｛｜｝"
			, " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}"
		);

	}

	static boolean isBadChar2( char c ){
		return c == 0xa || taisaku_map2.get( (int) c );
	}

	//! フォントによって全角文字が化けるので、その対策
	public static String font_taisaku( String text, boolean lf2br ){
		if( text == null ) return null;
		int l = text.length();
		StringBuilder sb = new StringBuilder( l );
		if( ! lf2br ){
			for( int i = 0 ; i < l ; ++ i ){
				int start = i;
				while( i < l && ! taisaku_map2.get( (int) text.charAt( i ) ) ) ++ i;
				if( i > start ){
					sb.append( text.substring( start, i ) );
					if( i >= l ) break;
				}
				sb.append( taisaku_map.get( text.charAt( i ) ) );
			}
		}else{
			for( int i = 0 ; i < l ; ++ i ){
				int start = i;
				while( i < l && ! isBadChar2( text.charAt( i ) ) ) ++ i;
				if( i > start ){
					sb.append( text.substring( start, i ) );
					if( i >= l ) break;
				}
				char c = text.charAt( i );
				if( c == 0xa ){
					sb.append( "<br/>" );
				}else{
					sb.append( taisaku_map.get( c ) );
				}
			}
		}
		return sb.toString();
	}

	////////////////////////////

	public static String toLower( String from ){
		if( from == null ) return null;
		return from.toLowerCase( Locale.US );
	}

	public static String toUpper( String from ){
		if( from == null ) return null;
		return from.toUpperCase( Locale.US );
	}

	public static String getString( Bundle b, String key, String defval ){
		try{
			String v = b.getString( key );
			if( v != null ) return v;
		}catch( Throwable ignored ){
		}
		return defval;
	}

	public static byte[] loadFile( File file ) throws IOException{
		int size = (int) file.length();
		byte[] data = new byte[ size ];
		FileInputStream in = new FileInputStream( file );
		try{
			int nRead = 0;
			while( nRead < size ){
				int delta = in.read( data, nRead, size - nRead );
				if( delta <= 0 ) break;
			}
			return data;
		}finally{
			try{
				in.close();
			}catch( Throwable ignored ){
			}
		}
	}

	public static String ellipsize( String t, int max ){
		return ( t.length() > max ? t.substring( 0, max - 1 ) + "…" : t );
	}

//	public static int getEnumStringId( String residPrefix, String name,Context context ) {
//		name = residPrefix + name;
//		try{
//			int iv = context.getResources().getIdentifier(name,"string",context.getPackageName() );
//			if( iv != 0 ) return iv;
//		}catch(Throwable ex){
//		}
//		log.e("missing resid for %s",name);
//		return R.string.Dialog_Cancel;
//	}

	public static String getConnectionResultErrorMessage( ConnectionResult connectionResult ){
		int code = connectionResult.getErrorCode();
		String msg = connectionResult.getErrorMessage();
		if( TextUtils.isEmpty( msg ) ){
			switch( code ){
			case ConnectionResult.SUCCESS:
				msg = "SUCCESS";
				break;
			case ConnectionResult.SERVICE_MISSING:
				msg = "SERVICE_MISSING";
				break;
			case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
				msg = "SERVICE_VERSION_UPDATE_REQUIRED";
				break;
			case ConnectionResult.SERVICE_DISABLED:
				msg = "SERVICE_DISABLED";
				break;
			case ConnectionResult.SIGN_IN_REQUIRED:
				msg = "SIGN_IN_REQUIRED";
				break;
			case ConnectionResult.INVALID_ACCOUNT:
				msg = "INVALID_ACCOUNT";
				break;
			case ConnectionResult.RESOLUTION_REQUIRED:
				msg = "RESOLUTION_REQUIRED";
				break;
			case ConnectionResult.NETWORK_ERROR:
				msg = "NETWORK_ERROR";
				break;
			case ConnectionResult.INTERNAL_ERROR:
				msg = "INTERNAL_ERROR";
				break;
			case ConnectionResult.SERVICE_INVALID:
				msg = "SERVICE_INVALID";
				break;
			case ConnectionResult.DEVELOPER_ERROR:
				msg = "DEVELOPER_ERROR";
				break;
			case ConnectionResult.LICENSE_CHECK_FAILED:
				msg = "LICENSE_CHECK_FAILED";
				break;
			case ConnectionResult.CANCELED:
				msg = "CANCELED";
				break;
			case ConnectionResult.TIMEOUT:
				msg = "TIMEOUT";
				break;
			case ConnectionResult.INTERRUPTED:
				msg = "INTERRUPTED";
				break;
			case ConnectionResult.API_UNAVAILABLE:
				msg = "API_UNAVAILABLE";
				break;
			case ConnectionResult.SIGN_IN_FAILED:
				msg = "SIGN_IN_FAILED";
				break;
			case ConnectionResult.SERVICE_UPDATING:
				msg = "SERVICE_UPDATING";
				break;
			case ConnectionResult.SERVICE_MISSING_PERMISSION:
				msg = "SERVICE_MISSING_PERMISSION";
				break;
			case ConnectionResult.RESTRICTED_PROFILE:
				msg = "RESTRICTED_PROFILE";
				break;

			}
		}
		return msg;
	}

	public static String getConnectionSuspendedMessage( int i ){
		switch( i ){
		default:
			return "?";
		case GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST:
			return "NETWORK_LOST";
		case GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
			return "SERVICE_DISCONNECTED";
		}
	}

	static HashMap<String,String> mime_type_ex = null;
	static final Object mime_type_ex_lock = new Object();
	static String findMimeTypeEx(String ext){
		synchronized( mime_type_ex_lock ){
			if( mime_type_ex == null ){
				HashMap<String, String> tmp = new HashMap<>();
				tmp.put( "BDM", "application/vnd.syncml.dm+wbxml" );
				tmp.put( "DAT", "" );
				tmp.put( "TID", "" );
				tmp.put( "js", "text/javascript" );
				tmp.put( "sh", "application/x-sh" );
				tmp.put( "lua", "text/x-lua" );
				mime_type_ex = tmp;
			}
			return mime_type_ex.get(ext);
		}
	}

	public static final String MIME_TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream";

	public static String getMimeType( LogWriter log, String src ){
		String ext = MimeTypeMap.getFileExtensionFromUrl( src );
		if( !TextUtils.isEmpty( ext  ) ){
			ext =ext.toLowerCase(Locale.US  );

			//
			String mime_type = MimeTypeMap.getSingleton().getMimeTypeFromExtension( ext );
			if( !TextUtils.isEmpty( mime_type  ) ) return mime_type;

			//
			mime_type = findMimeTypeEx(ext);
			if( !TextUtils.isEmpty( mime_type  ) )return mime_type;

			// 戻り値が空文字列の場合とnullの場合があり、空文字列の場合は既知でありログ出力しない

			if( mime_type == null && log != null ) log.w("getMimeType(): unknown file extension '%s'",ext);
		}
		return MIME_TYPE_APPLICATION_OCTET_STREAM;
	}

	static class FileInfo{

		Uri uri;
		String mime_type;

		FileInfo( String any_uri ){
			if( any_uri == null ) return;

			if( any_uri.startsWith( "/" ) ){
				uri = Uri.fromFile( new File( any_uri ) );
			}else{
				uri = Uri.parse( any_uri );
			}

			String ext = MimeTypeMap.getFileExtensionFromUrl( any_uri );
			if( ext != null ){
				mime_type = MimeTypeMap.getSingleton().getMimeTypeFromExtension( ext.toLowerCase() );
			}
		}
	}

	static @NonNull Map<String, String> getSecondaryStorageVolumesMap( Context context ){
		Map<String, String> result = new HashMap<>();
		try{

			StorageManager sm = (StorageManager) context.getApplicationContext().getSystemService( Context.STORAGE_SERVICE );

			// SDカードスロットのある7.0端末が手元にないから検証できない
//			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ){
//				for(StorageVolume volume : sm.getStorageVolumes() ){
//					// String path = volume.getPath();
//					String state = volume.getState();
//
//				}
//			}

			Method getVolumeList = sm.getClass().getMethod( "getVolumeList" );
			Object[] volumes = (Object[]) getVolumeList.invoke( sm );
			//
			for( Object volume : volumes ){
				Class<?> volume_clazz = volume.getClass();

				String path = (String) volume_clazz.getMethod( "getPath" ).invoke( volume );
				String state = (String) volume_clazz.getMethod( "getState" ).invoke( volume );
				if( ! TextUtils.isEmpty( path ) && "mounted".equals( state ) ){
					//
					boolean isPrimary = (Boolean) volume_clazz.getMethod( "isPrimary" ).invoke( volume );
					if( isPrimary ) result.put( "primary", path );
					//
					String uuid = (String) volume_clazz.getMethod( "getUuid" ).invoke( volume );
					if( ! TextUtils.isEmpty( uuid ) ) result.put( uuid, path );
				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
		return result;
	}

	public static String toCamelCase( String src ){
		StringBuilder sb = new StringBuilder();
		for( String s : src.split( "_" ) ){
			if( TextUtils.isEmpty( s ) ) continue;
			sb.append( Character.toUpperCase( s.charAt( 0 ) ) );
			sb.append( s.substring( 1, s.length() ).toLowerCase() );
		}
		return sb.toString();
	}

	private static DocumentBuilder xml_builder;

	public static Element parseXml( byte[] src ){
		if( xml_builder == null ){
			try{
				xml_builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			}catch( Throwable ex ){
				ex.printStackTrace();
				return null;
			}
		}
		try{
			return xml_builder.parse( new ByteArrayInputStream( src ) ).getDocumentElement();
		}catch( Throwable ex ){
			ex.printStackTrace();
			return null;
		}
	}

	public static String getAttribute( NamedNodeMap attr_map, String name, String defval ){
		Node node = attr_map.getNamedItem( name );
		if( node != null ) return node.getNodeValue();
		return defval;
	}



	public static void runOnMainThread( @NonNull Runnable proc ){
		if( Looper.getMainLooper().getThread() == Thread.currentThread() ){
			proc.run();
		}else{
			new Handler( Looper.getMainLooper() ).post( proc );
		}
	}

	public static void showToast( final Context context, final boolean bLong, final String fmt, final Object... args ){
		runOnMainThread( new Runnable(){
			@Override public void run(){
				Toast.makeText(
					context
					, ( args.length == 0 ? fmt : String.format( fmt, args ) )
					, bLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
				).show();
			}
		} );
	}

	public static void showToast( final Context context, final Throwable ex, final String fmt, final Object... args ){
		runOnMainThread( new Runnable(){
			@Override public void run(){
				Toast.makeText(
					context
					, LogWriter.formatError( ex, fmt, args )
					, Toast.LENGTH_LONG
				).show();
			}
		} );
	}

	public static void showToast( final Context context, final boolean bLong, final int string_id, final Object... args ){
		runOnMainThread( new Runnable(){
			@Override public void run(){

				Toast.makeText(
					context
					, context.getString( string_id, args )
					, bLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
				).show();
			}
		} );
	}

	public static void showToast( final Context context, final Throwable ex, final int string_id, final Object... args ){
		runOnMainThread( new Runnable(){
			@Override public void run(){
				Toast.makeText(
					context
					, LogWriter.formatError( ex, context.getResources(), string_id, args )
					, Toast.LENGTH_LONG
				).show();
			}
		} );
	}


	public static boolean isExternalStorageDocument( Uri uri ){
		return "com.android.externalstorage.documents".equals( uri.getAuthority() );
	}
	private static final String PATH_TREE = "tree";
	private static final String PATH_DOCUMENT = "document";

	public static String getDocumentId(Uri documentUri) {
		final List<String> paths = documentUri.getPathSegments();
		if (paths.size() >= 2 && PATH_DOCUMENT.equals(paths.get(0))) {
			// document
			return paths.get(1);
		}
		if (paths.size() >= 4 && PATH_TREE.equals(paths.get(0))
			&& PATH_DOCUMENT.equals(paths.get(2))) {
			// document in tree
			return paths.get(3);
		}
		if (paths.size() >= 2 && PATH_TREE.equals(paths.get(0))) {
			// tree
			return paths.get(1);
		}
		throw new IllegalArgumentException("Invalid URI: " + documentUri);
	}

	public static @Nullable File getFile( Context context, @NonNull String path ){
		try{
			if( path.startsWith( "/" )) return new File(path);
			Uri uri = Uri.parse( path );
			if( "file".equals( uri.getScheme() )) return new File(uri.getPath());

			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ){
				if( isExternalStorageDocument( uri ) ){
					try{
						final String docId = getDocumentId( uri );
						final String[] split = docId.split( ":" );
						if( split.length >= 2 ){
							final String uuid = split[ 0 ];
							if( "primary".equalsIgnoreCase( uuid ) ){
								return new File( Environment.getExternalStorageDirectory() + "/" + split[ 1 ] );
							}else{
								Map<String, String> volume_map = Utils.getSecondaryStorageVolumesMap( context );
								String volume_path = volume_map.get( uuid );
								if( volume_path != null ){
									return new File( volume_path + "/" + split[ 1 ] );
								}
							}
						}
					}catch( Throwable ex2 ){
						ex2.printStackTrace();
					}
				}
			}
			// MediaStore Uri
			Cursor cursor = context.getContentResolver().query( uri, null, null, null, null );
			if( cursor != null ){
				try{
					if( cursor.moveToFirst() ){
						int col_count = cursor.getColumnCount();
						for( int i = 0 ; i < col_count ; ++ i ){
							int type = cursor.getType( i );
							if( type != Cursor.FIELD_TYPE_STRING ) continue;
							String name = cursor.getColumnName( i );
							String value = cursor.isNull( i ) ? null : cursor.getString( i );
							if( ! TextUtils.isEmpty( value ) ){
								if( "filePath".equals( name ) ){
									return new File( value );
								}
							}
						}
					}
				}finally{
					cursor.close();
				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
		return null;
	}



}

