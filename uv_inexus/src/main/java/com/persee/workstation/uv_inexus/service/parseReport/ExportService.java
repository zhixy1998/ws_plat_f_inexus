package com.persee.workstation.uv_inexus.service.parseReport;

import java.io.OutputStream;
import java.io.IOException;

public interface ExportService {
    void exportAllTables(OutputStream outputStream) throws IOException;
}
