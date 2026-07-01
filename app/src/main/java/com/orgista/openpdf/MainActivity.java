package com.orgista.openpdf;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends AppCompatActivity {
    private static final String STATE_URI = "state_uri";
    private static final String STATE_PAGE_INDEX = "state_page_index";
    private static final String STATE_WIDTH_PROGRESS = "state_width_progress";
    private static final String STATE_HEIGHT_PROGRESS = "state_height_progress";
    private static final String STATE_ZOOM_INDEX = "state_zoom_index";
    private static final String STATE_DARK_MODE = "state_dark_mode";
    private static final String PREFS_READER = "reader_prefs";
    private static final String PREF_BOOKMARK_URI = "bookmark_uri";
    private static final String PREF_BOOKMARK_PAGE = "bookmark_page";
    private static final String PREF_BOOKMARK_ZOOM = "bookmark_zoom";
    private static final String PREF_BOOKMARK_WIDTH = "bookmark_width";
    private static final String PREF_BOOKMARK_HEIGHT = "bookmark_height";
    private static final String PREF_DARK_MODE = "dark_mode";
    private static final float[] ZOOM_STEPS = new float[]{0.85f, 1.0f, 1.15f, 1.3f, 1.5f, 1.75f};
    private static final int DEFAULT_ZOOM_INDEX = 1;

    private final PdfDocumentSession documentSession = new PdfDocumentSession();
    private final PanelSizingCalculator panelSizingCalculator = new PanelSizingCalculator();
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger renderSequence = new AtomicInteger();

    private ActivityResultLauncher<Intent> openDocumentLauncher;

    private SharedPreferences readerPreferences;
    private View rootLayout;
    private View headerBar;
    private View toolbarBar;
    private View toolbarDivider;
    private View contentRoot;
    private TextView appTitleLabel;
    private TextView documentLabel;
    private TextView pageLabel;
    private TextView panelSizeLabel;
    private TextView zoomLabel;
    private TextView welcomeTitleLabel;
    private TextView welcomeCaptionLabel;
    private Button openDocumentButton;
    private Button closeDocumentButton;
    private Button previousPageButton;
    private Button nextPageButton;
    private MaterialButton bookmarkButton;
    private Button zoomOutButton;
    private Button zoomInButton;
    private MaterialButton themeToggleButton;
    private Button openSidePanelButton;
    private Button emptyStateOpenButton;
    private SeekBar panelWidthSeek;
    private SeekBar panelHeightSeek;
    private FrameLayout viewerStage;
    private FrameLayout viewerPanel;
    private ImageView pdfPageImage;
    private View emptyStateContainer;
    private View mainLoadingSkeleton;

    private ObjectAnimator loadingAnimator;
    private Bitmap currentBitmap;
    private int currentPageIndex;
    private int zoomStepIndex = DEFAULT_ZOOM_INDEX;
    private boolean darkModeEnabled;
    private boolean currentDocumentBookmarked;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readerPreferences = getSharedPreferences(PREFS_READER, MODE_PRIVATE);
        bindViews();
        registerOpenDocumentLauncher();
        wireControls();

        if (savedInstanceState != null) {
            panelWidthSeek.setProgress(savedInstanceState.getInt(STATE_WIDTH_PROGRESS, panelWidthSeek.getProgress()));
            panelHeightSeek.setProgress(savedInstanceState.getInt(STATE_HEIGHT_PROGRESS, panelHeightSeek.getProgress()));
            zoomStepIndex = clampZoomIndex(savedInstanceState.getInt(STATE_ZOOM_INDEX, DEFAULT_ZOOM_INDEX));
            darkModeEnabled = savedInstanceState.getBoolean(STATE_DARK_MODE, readerPreferences.getBoolean(PREF_DARK_MODE, false));
        } else {
            darkModeEnabled = readerPreferences.getBoolean(PREF_DARK_MODE, false);
        }

        viewerStage.post(this::applyPanelSize);
        applyZoom();
        applyTheme();

        if (!openFromIntent(getIntent())) {
            if (savedInstanceState != null) {
                String uriString = savedInstanceState.getString(STATE_URI);
                currentPageIndex = savedInstanceState.getInt(STATE_PAGE_INDEX, 0);
                if (uriString != null) {
                    openPdf(Uri.parse(uriString), currentPageIndex);
                    return;
                }
            }
            if (restoreBookmarkedDocument()) {
                return;
            }
            updateChrome();
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
        rootLayout = findViewById(R.id.rootLayout);
        headerBar = findViewById(R.id.headerBar);
        toolbarBar = findViewById(R.id.toolbarBar);
        toolbarDivider = findViewById(R.id.toolbarDivider);
        contentRoot = findViewById(R.id.contentRoot);
        appTitleLabel = findViewById(R.id.appTitleLabel);
        documentLabel = findViewById(R.id.documentLabel);
        pageLabel = findViewById(R.id.pageLabel);
        panelSizeLabel = findViewById(R.id.panelSizeLabel);
        zoomLabel = findViewById(R.id.zoomLabel);
        welcomeTitleLabel = findViewById(R.id.welcomeTitleLabel);
        welcomeCaptionLabel = findViewById(R.id.welcomeCaptionLabel);
        openDocumentButton = findViewById(R.id.openDocumentButton);
        closeDocumentButton = findViewById(R.id.closeDocumentButton);
        previousPageButton = findViewById(R.id.previousPageButton);
        nextPageButton = findViewById(R.id.nextPageButton);
        bookmarkButton = findViewById(R.id.bookmarkButton);
        zoomOutButton = findViewById(R.id.zoomOutButton);
        zoomInButton = findViewById(R.id.zoomInButton);
        themeToggleButton = findViewById(R.id.themeToggleButton);
        openSidePanelButton = findViewById(R.id.openSidePanelButton);
        emptyStateOpenButton = findViewById(R.id.emptyStateOpenButton);
        panelWidthSeek = findViewById(R.id.panelWidthSeek);
        panelHeightSeek = findViewById(R.id.panelHeightSeek);
        viewerStage = findViewById(R.id.viewerStage);
        viewerPanel = findViewById(R.id.viewerPanel);
        pdfPageImage = findViewById(R.id.pdfPageImage);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        mainLoadingSkeleton = findViewById(R.id.mainLoadingSkeleton);
    }

    private void registerOpenDocumentLauncher() {
        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }

                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        persistUriPermission(uri, result.getData().getFlags());
                        openPdf(uri, 0);
                    }
                }
        );
    }

    private void wireControls() {
        openDocumentButton.setOnClickListener(view -> launchPicker());
        closeDocumentButton.setOnClickListener(view -> closeCurrentDocument());
        previousPageButton.setOnClickListener(view -> goToPage(currentPageIndex - 1));
        nextPageButton.setOnClickListener(view -> goToPage(currentPageIndex + 1));
        bookmarkButton.setOnClickListener(view -> toggleBookmark());
        zoomOutButton.setOnClickListener(view -> adjustZoom(-1));
        zoomInButton.setOnClickListener(view -> adjustZoom(1));
        themeToggleButton.setOnClickListener(view -> toggleDarkMode());
        openSidePanelButton.setOnClickListener(view -> launchSidePanel());
        emptyStateOpenButton.setOnClickListener(view -> launchPicker());

        SeekBar.OnSeekBarChangeListener resizeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                applyPanelSize();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                persistBookmarkIfCurrent();
                scheduleRender();
            }
        };

        panelWidthSeek.setOnSeekBarChangeListener(resizeListener);
        panelHeightSeek.setOnSeekBarChangeListener(resizeListener);
    }

    private boolean openFromIntent(@Nullable Intent intent) {
        Uri uri = PdfViewIntents.readUri(intent);
        if (uri == null) {
            return false;
        }

        openPdf(uri, PdfViewIntents.readPageIndex(intent));
        return true;
    }

    private void launchPicker() {
        Intent openIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        openIntent.addCategory(Intent.CATEGORY_OPENABLE);
        openIntent.setType("application/pdf");
        openDocumentLauncher.launch(openIntent);
    }

    private void launchSidePanel() {
        Uri documentUri = documentSession.getDocumentUri();
        if (documentUri == null) {
            return;
        }

        startActivity(PdfViewIntents.createSidePanelIntent(this, documentUri, currentPageIndex));
    }

    private boolean openPdf(Uri uri, int pageIndex) {
        try {
            documentSession.open(this, uri);
            currentPageIndex = Math.max(0, Math.min(pageIndex, documentSession.getPageCount() - 1));
            currentDocumentBookmarked = isStoredBookmarkUri(uri);
            showLoadingState(true);
            updateChrome();
            scheduleRender();
            return true;
        } catch (IOException e) {
            showError("Unable to open PDF: " + e.getMessage());
            clearDocumentState();
            return false;
        }
    }

    private void persistUriPermission(Uri uri, int resultFlags) {
        int grantFlags = resultFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (grantFlags == 0) {
            grantFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        }

        try {
            getContentResolver().takePersistableUriPermission(uri, grantFlags);
        } catch (SecurityException ignored) {
            // Some providers do not grant persistable permissions; reading still works for the current session.
        }
    }

    private void goToPage(int requestedPageIndex) {
        if (!documentSession.hasDocument()) {
            return;
        }

        currentPageIndex = Math.max(0, Math.min(requestedPageIndex, documentSession.getPageCount() - 1));
        persistBookmarkIfCurrent();
        updateChrome();
        scheduleRender();
    }

    private void closeCurrentDocument() {
        clearDocumentState();
        zoomStepIndex = DEFAULT_ZOOM_INDEX;
        applyZoom();
        updateChrome();
    }

    private void toggleBookmark() {
        Uri documentUri = documentSession.getDocumentUri();
        if (documentUri == null) {
            return;
        }

        if (currentDocumentBookmarked) {
            clearBookmark();
            currentDocumentBookmarked = false;
            Toast.makeText(this, R.string.bookmark_removed_message, Toast.LENGTH_SHORT).show();
        } else {
            persistBookmark(documentUri);
            currentDocumentBookmarked = true;
            Toast.makeText(this, R.string.bookmark_saved_message, Toast.LENGTH_SHORT).show();
        }

        updateChrome();
    }

    private void adjustZoom(int delta) {
        int nextZoomIndex = clampZoomIndex(zoomStepIndex + delta);
        if (nextZoomIndex == zoomStepIndex) {
            return;
        }

        zoomStepIndex = nextZoomIndex;
        applyZoom();
        persistBookmarkIfCurrent();
        updateChrome();
        if (documentSession.hasDocument()) {
            scheduleRender();
        }
    }

    private void toggleDarkMode() {
        darkModeEnabled = !darkModeEnabled;
        readerPreferences.edit().putBoolean(PREF_DARK_MODE, darkModeEnabled).apply();
        applyTheme();
        updateChrome();
    }

    private void applyPanelSize() {
        int availableWidth = Math.max(1, viewerStage.getWidth() - dp(64));
        int availableHeight = Math.max(1, viewerStage.getHeight() - dp(64));
        PanelSizingCalculator.Size size = panelSizingCalculator.calculate(
                availableWidth,
                availableHeight,
                panelWidthSeek.getProgress(),
                panelHeightSeek.getProgress()
        );

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) viewerPanel.getLayoutParams();
        layoutParams.width = size.width;
        layoutParams.height = size.height;
        viewerPanel.setLayoutParams(layoutParams);
        panelSizeLabel.setText(getString(R.string.panel_size_format, size.width, size.height));
    }

    private void scheduleRender() {
        if (!documentSession.hasDocument()) {
            pdfPageImage.setImageDrawable(null);
            viewerPanel.setVisibility(View.GONE);
            showLoadingState(false);
            emptyStateContainer.setVisibility(View.VISIBLE);
            return;
        }

        if (viewerPanel.getWidth() == 0 || viewerPanel.getHeight() == 0) {
            viewerPanel.post(this::scheduleRender);
            return;
        }

        final int renderId = renderSequence.incrementAndGet();
        final int pageToRender = currentPageIndex;
        final float zoomScale = getCurrentZoomScale();
        final int targetWidth = Math.max(1, Math.round((viewerPanel.getWidth() - dp(36)) * zoomScale));
        final int targetHeight = Math.max(1, Math.round((viewerPanel.getHeight() - dp(36)) * zoomScale));

        viewerPanel.setVisibility(View.VISIBLE);
        showLoadingState(true);
        emptyStateContainer.setVisibility(View.GONE);
        pageLabel.setText(getString(R.string.page_rendering_format, pageToRender + 1));

        renderExecutor.execute(() -> {
            try {
                Bitmap renderedBitmap = documentSession.renderPage(pageToRender, targetWidth, targetHeight);
                runOnUiThread(() -> {
                    if (renderSequence.get() != renderId || isFinishing()) {
                        renderedBitmap.recycle();
                        return;
                    }
                    swapBitmap(renderedBitmap);
                    showLoadingState(false);
                    updateChrome();
                });
            } catch (RuntimeException e) {
                runOnUiThread(() -> {
                    showLoadingState(false);
                    showError("Unable to render page: " + e.getMessage());
                });
            }
        });
    }

    private void updateChrome() {
        int pageCount = documentSession.getPageCount();
        boolean hasDocument = pageCount > 0;

        previousPageButton.setEnabled(hasDocument && currentPageIndex > 0);
        nextPageButton.setEnabled(hasDocument && currentPageIndex < pageCount - 1);
        bookmarkButton.setEnabled(hasDocument);
        zoomOutButton.setEnabled(hasDocument && zoomStepIndex > 0);
        zoomInButton.setEnabled(hasDocument && zoomStepIndex < ZOOM_STEPS.length - 1);
        openSidePanelButton.setEnabled(hasDocument);
        openDocumentButton.setVisibility(hasDocument ? View.VISIBLE : View.GONE);
        closeDocumentButton.setVisibility(hasDocument ? View.VISIBLE : View.GONE);
        toolbarBar.setVisibility(hasDocument ? View.VISIBLE : View.GONE);
        toolbarDivider.setVisibility(hasDocument ? View.VISIBLE : View.GONE);
        viewerPanel.setVisibility(hasDocument ? View.VISIBLE : View.GONE);
        openDocumentButton.setText(R.string.open_different_pdf);
        zoomLabel.setText(getString(R.string.zoom_percentage_format, Math.round(getCurrentZoomScale() * 100f)));
        updateBookmarkAppearance();
        themeToggleButton.setContentDescription(getString(darkModeEnabled ? R.string.theme_light : R.string.theme_dark));
        applyTheme();
        applyZoom();

        if (!hasDocument) {
            documentLabel.setText(R.string.no_document_loaded);
            documentLabel.setVisibility(View.GONE);
            pageLabel.setText(R.string.page_placeholder);
            emptyStateContainer.setVisibility(View.VISIBLE);
            return;
        }

        documentLabel.setText(PdfDocumentUtils.getDisplayName(this, documentSession.getDocumentUri()));
        documentLabel.setVisibility(View.VISIBLE);
        pageLabel.setText(getString(R.string.page_status_format, currentPageIndex + 1, pageCount));
    }

    private void clearDocumentState() {
        try {
            documentSession.close();
        } catch (IOException ignored) {
        }
        currentPageIndex = 0;
        currentDocumentBookmarked = false;
        showLoadingState(false);
        swapBitmap(null);
        viewerPanel.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.VISIBLE);
        updateChrome();
    }

    private void showLoadingState(boolean visible) {
        if (visible) {
            emptyStateContainer.setVisibility(View.GONE);
            mainLoadingSkeleton.setVisibility(View.VISIBLE);
            if (loadingAnimator == null) {
                loadingAnimator = LoadingSkeletonAnimator.create(mainLoadingSkeleton);
            }
            if (!loadingAnimator.isStarted()) {
                loadingAnimator.start();
            }
        } else {
            if (loadingAnimator != null) {
                loadingAnimator.cancel();
            }
            mainLoadingSkeleton.setAlpha(1f);
            mainLoadingSkeleton.setVisibility(View.GONE);
        }
    }

    private void swapBitmap(@Nullable Bitmap nextBitmap) {
        Bitmap previousBitmap = currentBitmap;
        currentBitmap = nextBitmap;
        pdfPageImage.setImageBitmap(nextBitmap);
        applyZoom();
        if (previousBitmap != null && previousBitmap != nextBitmap && !previousBitmap.isRecycled()) {
            previousBitmap.recycle();
        }
    }

    private void applyZoom() {
        float zoom = getCurrentZoomScale();
        pdfPageImage.setScaleX(zoom);
        pdfPageImage.setScaleY(zoom);
    }

    private void applyTheme() {
        int surfaceColor = 0xFF1A1F2E;
        int stageColor = 0xFF0E1116;
        int titleColor = 0xFFF9FAFB;
        int bodyColor = 0xFF9CA3AF;
        int dividerColor = 0xFF2D3748;

        rootLayout.setBackgroundColor(stageColor);
        contentRoot.setBackgroundColor(stageColor);
        headerBar.setBackgroundColor(surfaceColor);
        toolbarBar.setBackgroundColor(surfaceColor);
        toolbarDivider.setBackgroundColor(dividerColor);
        appTitleLabel.setTextColor(titleColor);
        documentLabel.setTextColor(bodyColor);
        pageLabel.setTextColor(0xFFE5E7EB);
        zoomLabel.setTextColor(0xFFE5E7EB);
        welcomeTitleLabel.setTextColor(titleColor);
        welcomeCaptionLabel.setTextColor(bodyColor);
        themeToggleButton.setIconResource(darkModeEnabled ? R.drawable.ic_sun : R.drawable.ic_moon);
        themeToggleButton.setIconTint(ColorStateList.valueOf(
                darkModeEnabled ? 0xFFFCD34D : 0xFF9CA3AF
        ));

        if (darkModeEnabled) {
            ColorMatrix colorMatrix = new ColorMatrix(new float[]{
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
            });
            pdfPageImage.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        } else {
            pdfPageImage.clearColorFilter();
        }
    }

    private void updateBookmarkAppearance() {
        if (currentDocumentBookmarked) {
            bookmarkButton.setText(R.string.bookmarked);
            bookmarkButton.setIconResource(R.drawable.ic_bookmark_added);
            bookmarkButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.accentGoldSoft)
            ));
            bookmarkButton.setTextColor(ContextCompat.getColor(this, R.color.accentGoldText));
            bookmarkButton.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.accentGoldText)
            ));
        } else {
            bookmarkButton.setText(R.string.bookmark);
            bookmarkButton.setIconResource(R.drawable.ic_bookmark_outline);
            bookmarkButton.setBackgroundTintList(null);
            bookmarkButton.setTextColor(ContextCompat.getColor(this, R.color.panelText));
            bookmarkButton.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.panelText)
            ));
        }
    }

    private float getCurrentZoomScale() {
        return ZOOM_STEPS[clampZoomIndex(zoomStepIndex)];
    }

    private int clampZoomIndex(int requestedIndex) {
        return Math.max(0, Math.min(requestedIndex, ZOOM_STEPS.length - 1));
    }

    private boolean restoreBookmarkedDocument() {
        String bookmarkUri = readerPreferences.getString(PREF_BOOKMARK_URI, null);
        if (bookmarkUri == null) {
            return false;
        }

        panelWidthSeek.setProgress(readerPreferences.getInt(PREF_BOOKMARK_WIDTH, panelWidthSeek.getProgress()));
        panelHeightSeek.setProgress(readerPreferences.getInt(PREF_BOOKMARK_HEIGHT, panelHeightSeek.getProgress()));
        zoomStepIndex = clampZoomIndex(readerPreferences.getInt(PREF_BOOKMARK_ZOOM, DEFAULT_ZOOM_INDEX));

        boolean restored = openPdf(Uri.parse(bookmarkUri), readerPreferences.getInt(PREF_BOOKMARK_PAGE, 0));
        if (!restored) {
            clearBookmark();
            currentDocumentBookmarked = false;
            Toast.makeText(this, R.string.bookmark_restore_failed, Toast.LENGTH_SHORT).show();
        }
        return restored;
    }

    private boolean isCurrentDocumentBookmarked() {
        Uri documentUri = documentSession.getDocumentUri();
        String bookmarkUri = readerPreferences.getString(PREF_BOOKMARK_URI, null);
        return documentUri != null && bookmarkUri != null && bookmarkUri.equals(documentUri.toString());
    }

    private boolean isStoredBookmarkUri(@Nullable Uri documentUri) {
        if (documentUri == null) {
            return false;
        }
        String bookmarkUri = readerPreferences.getString(PREF_BOOKMARK_URI, null);
        return bookmarkUri != null && bookmarkUri.equals(documentUri.toString());
    }

    private void persistBookmark(Uri documentUri) {
        readerPreferences.edit()
                .putString(PREF_BOOKMARK_URI, documentUri.toString())
                .putInt(PREF_BOOKMARK_PAGE, currentPageIndex)
                .putInt(PREF_BOOKMARK_ZOOM, zoomStepIndex)
                .putInt(PREF_BOOKMARK_WIDTH, panelWidthSeek.getProgress())
                .putInt(PREF_BOOKMARK_HEIGHT, panelHeightSeek.getProgress())
                .apply();
    }

    private void persistBookmarkIfCurrent() {
        Uri documentUri = documentSession.getDocumentUri();
        if (documentUri != null && isCurrentDocumentBookmarked()) {
            persistBookmark(documentUri);
        }
    }

    private void clearBookmark() {
        readerPreferences.edit()
                .remove(PREF_BOOKMARK_URI)
                .remove(PREF_BOOKMARK_PAGE)
                .remove(PREF_BOOKMARK_ZOOM)
                .remove(PREF_BOOKMARK_WIDTH)
                .remove(PREF_BOOKMARK_HEIGHT)
                .apply();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Uri documentUri = documentSession.getDocumentUri();
        if (documentUri != null) {
            outState.putString(STATE_URI, documentUri.toString());
        }
        outState.putInt(STATE_PAGE_INDEX, currentPageIndex);
        outState.putInt(STATE_WIDTH_PROGRESS, panelWidthSeek.getProgress());
        outState.putInt(STATE_HEIGHT_PROGRESS, panelHeightSeek.getProgress());
        outState.putInt(STATE_ZOOM_INDEX, zoomStepIndex);
        outState.putBoolean(STATE_DARK_MODE, darkModeEnabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        renderExecutor.shutdownNow();
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
        }
        swapBitmap(null);
        try {
            documentSession.close();
        } catch (IOException ignored) {
        }
    }
}
