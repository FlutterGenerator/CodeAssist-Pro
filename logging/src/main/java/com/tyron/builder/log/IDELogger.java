package com.tyron.builder.log;

import android.app.Activity;
import android.content.Context;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

public class IDELogger {
    private static LogViewModel mLogViewModel;
    public static void init(ViewModelStoreOwner ctx){
         mLogViewModel = new ViewModelProvider(ctx).get(LogViewModel.class);
    }
    public static void warn(String s){
        mLogViewModel.w(LogViewModel.IDE,s);
    }
    public static void warn(String s,Object ...objs){
        warn(String.format(s,objs));
    }
    public static void debug(String s){
        mLogViewModel.d(LogViewModel.IDE,s);
    }
    public static void debug(String s,Object ...objs){
        debug(String.format(s,objs));
    }
    public static void error(String s){
        mLogViewModel.e(LogViewModel.IDE,s);
    }
    public static void error(String s,Object ...objs){
        error(String.format(s,objs));
    }
    public static void info(String s){
        mLogViewModel.i(LogViewModel.IDE,s);
    }
    public static void info(String s,Object ...objs){
        info(String.format(s,objs));
    }
}
