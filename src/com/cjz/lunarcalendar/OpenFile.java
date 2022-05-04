package com.cjz.lunarcalendar;

import java.io.IOException;
import java.io.InputStream;

public interface OpenFile {
    InputStream openFile() throws IOException;
}
