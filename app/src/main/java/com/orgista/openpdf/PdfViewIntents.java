package com.orgista.openpdf;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

public final class PdfViewIntents {
    public static final String EXTRA_URI = "com.orgista.openpdf.extra.URI";
    public static final String EXTRA_PAGE_INDEX = "com.orgista.openpdf.extra.PAGE_INDEX";

    private PdfViewIntents() {
    }

    public static Intent createReaderIntent(Context context, Uri uri, int pageIndex) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setData(uri);
        intent.putExtra(EXTRA_URI, uri.toString());
        intent.putExtra(EXTRA_PAGE_INDEX, pageIndex);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    public static Intent createSidePanelIntent(Context context, Uri uri, int pageIndex) {
        Intent intent = new Intent(context, SidePanelActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(EXTRA_URI, uri.toString());
        intent.putExtra(EXTRA_PAGE_INDEX, pageIndex);
        intent.addCategory("com.orgista.openpdf.SIDE_PANEL");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    @Nullable
    public static Uri readUri(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }

        String uriString = intent.getStringExtra(EXTRA_URI);
        if (uriString != null && !uriString.isEmpty()) {
            return Uri.parse(uriString);
        }

        return intent.getData();
    }

    public static int readPageIndex(@Nullable Intent intent) {
        if (intent == null) {
            return 0;
        }
        return Math.max(0, intent.getIntExtra(EXTRA_PAGE_INDEX, 0));
    }
}
