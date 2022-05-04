package com.cjz.lunarcalendar;

import java.io.IOException;
import java.io.InputStream;

public class JavaOpenFile implements OpenFile{
    @Override
    public InputStream openFile() throws IOException {
        return getClass().getClassLoader().getResourceAsStream("lunars");
//        return getClass().getResourceAsStream("lunars");
    }
}
