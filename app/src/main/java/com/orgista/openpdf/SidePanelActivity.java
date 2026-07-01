package com.orgista.openpdf;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class SidePanelActivity extends AppCompatActivity {
    private final PdfDocumentSession documentSession = new PdfDocumentSession();
    private final ExecutorService thumbnailExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger thumbnailSequence = new AtomicInteger();
    private final List<Bitmap> thumbnailBitmaps = new ArrayList<>();

    private TextView sidePanelDocumentName;
    private TextView sidePanelPageCount;
    private TextView sidePanelCurrentPage;
    private TextView sidePanelEmptyState;
    private LinearLayout thumbnailContainer;
    private View sidePanelLoadingSkeleton;
    private Button backToReaderButton;

    private ObjectAnimator loadingAnimator;
    private Uri documentUri;
    private int currentPageIndex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_side_panel);

        bindViews();
        wireControls();

        if (!openFromIntent(getIntent())) {
            updateEmptyState();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            openFromIntent(intent);
        }
    }

    private void bindViews() {
        sidePanelDocumentName = findViewById(R.id.sidePanelDocumentName);
        sidePanelPageCount = findViewById(R.id.sidePanelPageCount);
        sidePanelCurrentPage = findViewById(R.id.sidePanelCurrentPage);
        sidePanelEmptyState = findViewById(R.id.sidePanelEmptyState);
        thumbnailContainer = findViewById(R.id.thumbnailContainer);
        sidePanelLoadingSkeleton = findViewById(R.id.sidePanelLoadingSkeleton);
        backToReaderButton = findViewById(R.id.backToReaderButton);
    }

    private void wireControls() {
        backToReaderButton.setOnClickListener(view -> openReader(currentPageIndex));
    }

    private boolean openFromIntent(@Nullable Intent intent) {
        Uri uri = PdfViewIntents.readUri(intent);
        if (uri == null) {
            return false;
        }

        currentPageIndex = PdfViewIntents.readPageIndex(intent);
        documentUri = uri;

        try {
            documentSession.open(this, uri);
            sidePanelDocumentName.setText(PdfDocumentUtils.getDisplayName(this, uri));
            sidePanelPageCount.setText(getString(R.string.page_count_summary, documentSession.getPageCount()));
            sidePanelCurrentPage.setText(getString(R.string.current_page_summary, currentPageIndex + 1));
            renderThumbnails();
            return true;
        } catch (IOException e) {
            showError("Unable to open PDF: " + e.getMessage());
            updateEmptyState();
            return false;
        }
    }

    private void renderThumbnails() {
        releaseThumbnailBitmaps();
        thumbnailContainer.removeAllViews();

        int pageCount = documentSession.getPageCount();
        if (pageCount == 0) {
            updateEmptyState();
            return;
        }

        int renderId = thumbnailSequence.incrementAndGet();
        int thumbnailCount = Math.min(6, pageCount);
        int startPage = Math.max(0, Math.min(currentPageIndex, pageCount - thumbnailCount));

        showLoadingState(true);
        sidePanelEmptyState.setVisibility(View.GONE);

        thumbnailExecutor.execute(() -> {
            try {
                LayoutInflater inflater = LayoutInflater.from(this);

                for (int offset = 0; offset < thumbnailCount; offset++) {
                    int pageIndex = startPage + offset;
                    Bitmap thumbnail = documentSession.renderPage(pageIndex, dp(132), dp(172));
                    runOnUiThread(() -> {
                        if (thumbnailSequence.get() != renderId || isFinishing()) {
                            thumbnail.recycle();
                            return;
                        }
                        addThumbnailView(inflater, pageIndex, thumbnail);
                    });
                }

                runOnUiThread(() -> {
                    if (thumbnailSequence.get() == renderId && !isFinishing()) {
                        showLoadingState(false);
                    }
                });
            } catch (RuntimeException e) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showError("Unable to render thumbnails: " + e.getMessage());
                    updateEmptyState();
                });
            }
        });
    }

    private void addThumbnailView(LayoutInflater inflater, int pageIndex, Bitmap thumbnail) {
        View itemView = inflater.inflate(R.layout.item_page_thumbnail, thumbnailContainer, false);
        ImageView thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
        TextView thumbnailPageNumber = itemView.findViewById(R.id.thumbnailPageNumber);

        thumbnailBitmaps.add(thumbnail);
        thumbnailImage.setImageBitmap(thumbnail);
        thumbnailImage.setContentDescription(getString(R.string.thumbnail_content_description, pageIndex + 1));
        thumbnailPageNumber.setText(getString(R.string.page_number, pageIndex + 1));
        itemView.setOnClickListener(view -> openReader(pageIndex));
        thumbnailContainer.addView(itemView);
    }

    private void openReader(int pageIndex) {
        if (documentUri == null) {
            return;
        }
        startActivity(PdfViewIntents.createReaderIntent(this, documentUri, pageIndex));
    }

    private void updateEmptyState() {
        releaseThumbnailBitmaps();
        sidePanelDocumentName.setText(R.string.no_document_loaded);
        sidePanelPageCount.setText(R.string.side_panel_caption);
        sidePanelCurrentPage.setText("");
        sidePanelEmptyState.setVisibility(View.VISIBLE);
        showLoadingState(false);
        thumbnailContainer.removeAllViews();
    }

    private void showLoadingState(boolean visible) {
        if (visible) {
            sidePanelLoadingSkeleton.setVisibility(View.VISIBLE);
            if (loadingAnimator == null) {
                loadingAnimator = LoadingSkeletonAnimator.create(sidePanelLoadingSkeleton);
            }
            if (!loadingAnimator.isStarted()) {
                loadingAnimator.start();
            }
        } else {
            if (loadingAnimator != null) {
                loadingAnimator.cancel();
            }
            sidePanelLoadingSkeleton.setAlpha(1f);
            sidePanelLoadingSkeleton.setVisibility(View.GONE);
        }
    }

    private void releaseThumbnailBitmaps() {
        for (Bitmap bitmap : thumbnailBitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        thumbnailBitmaps.clear();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        thumbnailExecutor.shutdownNow();
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
        }
        releaseThumbnailBitmaps();
        try {
            documentSession.close();
        } catch (IOException ignored) {
        }
    }
}
