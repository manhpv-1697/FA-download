package jp.juggler.fadownloader;

import java.io.InputStream;

//! HTTPClientのバッファ管理を独自に行いたい場合に使用する.
//! このインタフェースを実装したものをHTTPClient.getHTTP()の第二引数に指定する
public interface HTTPClientReceiver {
	byte[] onHTTPClientStream( LogWriter log,CancelChecker cancel_checker, InputStream in, int content_length);
}
