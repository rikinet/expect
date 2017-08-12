/*
 * Copyright (c) 2017 Riki Network Systems Inc.
 * All rights reserved.
 */

package jp.co.rikinet.expect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * InputStream と OutputStream を備えた対象を相手に
 * コマンドラインインターフェースを想定した文字列による対話を行う。
 */
public class Expect {

    private static Logger logger = LoggerFactory.getLogger(Expect.class);

    /** 対話対象の出力を読み込むストリーム */
    private InputStream inputStream;
    /** 対話対象の入力へ書き出すストリーム */
    private OutputStream outputStream;
    /** ストリームの入出力に使われる文字セット */
    private Charset charset;

    /** 内部クラス Reader から結果を受け取るため */
    private String readerResult;

    /**
     * タイムアウトは気にせず、与えられたパターンが出現するまで InputStream を読み込む。
     */
    private class Reader implements Runnable {
        private byte[] waitPattern;
        public Reader(byte[] waitPattern) {
            this.waitPattern = waitPattern;
        }
        @Override
        public void run() {
            readerResult = null;
            byte[] lastTail = new byte[waitPattern.length];
            int lastAvail = 0; // lastTail 中の有効バイト数
            byte[] currTail = new byte[waitPattern.length];
            byte[] bytes = new byte[2048];
            // 読み取れる長さが事前に分からないため、バッファに OutputStream を利用する。
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            for (;;) {
                int nRead;
                try {
                    nRead = inputStream.read(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                if (nRead < 0) {
                    break;
                }
                buffer.write(bytes, 0, nRead);
                // 以下 waitPattern を検出する。コマンドラインのプロンプトを意図しているので、
                // 読み込んだバイト列の末尾にのみ注目する。
                if (nRead >= waitPattern.length) {
                    // bytes の末尾に pattern が現れているかどうか
                    System.arraycopy(bytes, nRead - waitPattern.length, currTail, 0, waitPattern.length);
                    if (Arrays.equals(currTail, waitPattern)) {
                        // buffer への取り込みは充分。ここまでを返す
                        break;
                    }
                    // 次回の比較に備えて末尾を保存しておく
                    System.arraycopy(bytes, nRead - lastTail.length, lastTail, 0, lastTail.length);
                    lastAvail = lastTail.length;
                    continue;
                }
                // 今回受信分は短めなので前回受信した物と連結してから末尾を調べる
                // まずは連結して充分な長さになるかどうか
                if (lastAvail + nRead < waitPattern.length) {
                    // 比較できる長さに足らない。さらに読み込むべき。
                    System.arraycopy(bytes, 0, lastTail, lastAvail, nRead);
                    lastAvail += nRead;
                    continue;
                }
                // 前回の末尾と今回を連結して比較用バイト列を作成する。
                System.arraycopy(lastTail, lastAvail - (waitPattern.length - nRead), currTail, 0, waitPattern.length - nRead);
                System.arraycopy(bytes, 0, currTail, waitPattern.length - nRead, nRead);
                if (Arrays.equals(waitPattern, currTail)) {
                    break;
                }
                System.arraycopy(currTail, 0, lastTail, 0, currTail.length);
                lastAvail = currTail.length;
            }
            readerResult = new String(buffer.toByteArray(), charset);
        }
    }

    public Expect() {
        charset = Charset.forName("UTF-8");
    }

    public Expect(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        charset = Charset.forName("UTF-8");
    }

    /**
     * コマンド文字列を送信する。
     * 引数 line を現在の Charset で変換して送信する。それ以外の修飾は行わない。
     * @param line 送信する文字列。コマンドラインの終端文字も含めること
     */
    public void sendLine(String line) {
        try {
            byte[] bytes = line.getBytes(charset);
            outputStream.write(bytes);
            outputStream.flush();
        } catch (UnsupportedEncodingException e) {
            logger.warn("Failed to encode the command string. {}: {}", line, e.getMessage());
        } catch (IOException e) {
            logger.warn("Failed to write command string. {}: {}", line, e.getMessage());
        }
    }

    /**
     * コマンドラインインターフェースでプロンプトを待つ。
     * @param pattern 待ち受ける文字列
     * @param timeout 時間切れまでのミリ秒
     * @return 時間切れにならずに読み取ったバイト列から構成した文字列
     */
    public String expect(String pattern, long timeout) {
        byte[] waitPattern = pattern.getBytes(charset);
        Reader reader = new Reader(waitPattern);
        Thread th = new Thread(reader);
        th.start();
        try {
            th.join(timeout);
            if (th.isAlive()) {
                th.interrupt();
                th.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return readerResult;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
