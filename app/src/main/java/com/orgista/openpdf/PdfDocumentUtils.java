package com.orgista.openpdf;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.Nullable;

public final class PdfDocumentUtils {
    private PdfDocumentUtils() {
    }

    public static String getDisplayName(Context context, @Nullable Uri uri) {
        if (uri == null) {
            return context.getString(R.string.no_document_loaded);
        }

        try (android.database.Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex >= 0) {
                    return cursor.getString(columnIndex);
                }
            }
        }

        return uri.getLastPathSegment() == null ? uri.toString() : uri.getLastPathSegment();
    }
}
