/*
 * Copyright (c) 2017 Riki Network Systems Inc.
 * All rights reserved.
 */

package jp.co.rikinet.expect;

import java.util.concurrent.TimeoutException;

/**
 * 期待しているパターンが受信データに表れないまま、設定時間が経過したことを表す。
 */
public class PatternNotFoundException extends TimeoutException {
    public final String pattern;

    public PatternNotFoundException(String pattern) {
        super("Pattern '" + pattern + "' not found in input.");
        this.pattern = pattern;
    }
}
