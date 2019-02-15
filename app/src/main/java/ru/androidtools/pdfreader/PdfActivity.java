package ru.androidtools.pdfreader;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;

public class PdfActivity extends AppCompatActivity implements View.OnClickListener {

  private String path;
  private ImageView imgView;
  private Button btnPrevious, btnNext;
  private PdfRenderer pdfRenderer;
  private PdfRenderer.Page curPage;
  private ParcelFileDescriptor descriptor;
  private int currentPage = 0;
  private static final String CURRENT_PAGE = "CURRENT_PAGE";
  private float currentZoomLevel = 5;
  private ImageButton btn_zoomin, btn_zoomout;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_pdf);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    path = getIntent().getStringExtra("fileName");
    setTitle(getIntent().getStringExtra("keyName"));

    if (savedInstanceState != null) {
      currentPage = savedInstanceState.getInt(CURRENT_PAGE, 0);
    }

    imgView = findViewById(R.id.imgView);
    btnPrevious = findViewById(R.id.btnPrevious);
    btnNext = findViewById(R.id.btnNext);
    btn_zoomin = findViewById(R.id.zoomin);
    btn_zoomout = findViewById(R.id.zoomout);
    //set click listener on buttons
    btnPrevious.setOnClickListener(this);
    btnNext.setOnClickListener(this);
    btn_zoomin.setOnClickListener(this);
    btn_zoomout.setOnClickListener(this);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (curPage != null) {
      outState.putInt(CURRENT_PAGE, curPage.getIndex());
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public void onStart() {
    super.onStart();
    try {
      openPdfRenderer();
      displayPage(currentPage);
    } catch (Exception e) {
      Toast.makeText(this, "PDF-файл защищен паролем.", Toast.LENGTH_SHORT).show();
    }
  }

  @Override public void onStop() {
    try {
      closePdfRenderer();
    } catch (IOException e) {
      e.printStackTrace();
    }
    super.onStop();
  }

  private void openPdfRenderer() {
    File file = new File(path);
    descriptor = null;
    pdfRenderer = null;
    try {
      descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
      pdfRenderer = new PdfRenderer(descriptor);
    } catch (Exception e) {
      Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show();
    }
  }

  private void closePdfRenderer() throws IOException {
    if (curPage != null) curPage.close();
    if (pdfRenderer != null) pdfRenderer.close();
    if (descriptor != null) descriptor.close();
  }

  private void displayPage(int index) {
    if (pdfRenderer.getPageCount() <= index) return;
    //close the current page
    if (curPage != null) curPage.close();
    //open the specified page
    curPage = pdfRenderer.openPage(index);

    int newWidth = (int) (getResources().getDisplayMetrics().widthPixels * curPage.getWidth() / 72
        * currentZoomLevel / 40);
    int newHeight =
        (int) (getResources().getDisplayMetrics().heightPixels * curPage.getHeight() / 72
            * currentZoomLevel / 64);
    Bitmap bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
    Matrix matrix = new Matrix();

    float dpiAdjustedZoomLevel = currentZoomLevel * DisplayMetrics.DENSITY_MEDIUM
        / getResources().getDisplayMetrics().densityDpi;
    matrix.setScale(dpiAdjustedZoomLevel, dpiAdjustedZoomLevel);
    curPage.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

    //display the bitmap
    imgView.setImageBitmap(bitmap);
    //enable or disable the button accordingly
    int pageCount = pdfRenderer.getPageCount();
    btnPrevious.setEnabled(0 != index);
    btnNext.setEnabled(index + 1 < pageCount);
    btn_zoomout.setEnabled(currentZoomLevel != 2);
    btn_zoomin.setEnabled(currentZoomLevel != 12);
  }

  @Override public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btnPrevious: {
        //get the index of previous page
        int index = curPage.getIndex() - 1;
        displayPage(index);
        break;
      }
      case R.id.btnNext: {
        //get the index of previous page
        int index = curPage.getIndex() + 1;
        displayPage(index);
        break;
      }
      case R.id.zoomout: {
        // Move to the next page
        --currentZoomLevel;
        displayPage(curPage.getIndex());
        break;
      }
      case R.id.zoomin: {
        // Move to the next page
        ++currentZoomLevel;
        displayPage(curPage.getIndex());
        break;
      }
    }
  }
}
