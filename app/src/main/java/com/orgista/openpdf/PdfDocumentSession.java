package com.orgista.openpdf;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

public final class PdfDocumentSession implements AutoCloseable {
    private ParcelFileDescriptor fileDescriptor;
    private PdfRenderer renderer;
    private Uri documentUri;

    public synchronized void open(Context context, Uri uri) throws IOException {
        close();
        ContentResolver resolver = context.getContentResolver();
        fileDescriptor = resolver.openFileDescriptor(uri, "r");
        if (fileDescriptor == null) {
            throw new IOException("Unable to open PDF descriptor.");
        }
        renderer = new PdfRenderer(fileDescriptor);
        documentUri = uri;
    }

    public synchronized boolean hasDocument() {
        return renderer != null;
    }

    public synchronized int getPageCount() {
        return renderer == null ? 0 : renderer.getPageCount();
    }

    public synchronized Uri getDocumentUri() {
        return documentUri;
    }

    public synchronized Bitmap renderPage(int pageIndex, int targetWidth, int targetHeight) {
        if (renderer == null) {
            throw new IllegalStateException("No PDF is open.");
        }

        int safePageIndex = Math.max(0, Math.min(pageIndex, renderer.getPageCount() - 1));
        try (PdfRenderer.Page page = renderer.openPage(safePageIndex)) {
            float widthScale = (float) targetWidth / (float) page.getWidth();
            float heightScale = (float) targetHeight / (float) page.getHeight();
            float scale = Math.max(0.1f, Math.min(widthScale, heightScale));

            int bitmapWidth = Math.max(1, Math.round(page.getWidth() * scale));
            int bitmapHeight = Math.max(1, Math.round(page.getHeight() * scale));

            Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(0xFFFFFFFF);

            Matrix transform = new Matrix();
            transform.setScale(scale, scale);
            page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        IOException failure = null;

        if (renderer != null) {
            renderer.close();
            renderer = null;
        }

        if (fileDescriptor != null) {
            try {
                fileDescriptor.close();
            } catch (IOException e) {
                failure = e;
            } finally {
                fileDescriptor = null;
            }
        }

        documentUri = null;

        if (failure != null) {
            throw failure;
        }
    }
}
