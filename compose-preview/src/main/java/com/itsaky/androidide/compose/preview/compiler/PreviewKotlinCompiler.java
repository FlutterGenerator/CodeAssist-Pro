package com.itsaky.androidide.compose.preview.compiler;

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;

public class PreviewKotlinCompiler {
    public static K2JVMCompiler compiler;
    public static void init(){
        compiler = new K2JVMCompiler();
    }
}
