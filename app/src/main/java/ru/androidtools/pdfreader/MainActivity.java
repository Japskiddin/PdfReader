package ru.androidtools.pdfreader;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {

  private ListView listView;
  private ArrayList<PdfFile> list = new ArrayList<>();
  private static final int REQUEST_PERMISSION = 1;
  private ScrollView content_create;
  private MenuItem btn_create;
  private LinearLayout pdf_layout;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle =
        new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
            R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    listView = findViewById(R.id.listView);
    content_create = findViewById(R.id.content_create);
    pdf_layout = findViewById(R.id.ll_pdflayout);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      checkPermission();
    } else {
      initViews();
    }
  }

  private BaseAdapter adapter = new BaseAdapter() {
    @Override public int getCount() {
      return list.size();
    }

    @Override public PdfFile getItem(int i) {
      return list.get(i);
    }

    @Override public long getItemId(int i) {
      return i;
    }

    @Override public View getView(int i, View view, ViewGroup viewGroup) {
      View v = view;
      if (v == null) {
        v = getLayoutInflater().inflate(R.layout.list_item, viewGroup, false);
      }

      PdfFile pdfFile = getItem(i);
      TextView name = v.findViewById(R.id.txtFileName);
      name.setText(pdfFile.getFileName());
      return v;
    }
  };

  private void initViews() {
    list.clear();
    String path = Environment.getExternalStorageDirectory().getAbsolutePath();
    initList(path);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(MainActivity.this, PdfActivity.class);
        intent.putExtra("keyName", list.get(i).getFileName());
        intent.putExtra("fileName", list.get(i).getFilePath());
        startActivity(intent);
      }
    });
  }

  private void initList(String path) {
    try {
      File file = new File(path);
      File[] fileList = file.listFiles();
      String fileName;
      for (File f : fileList) {
        if (f.isDirectory()) {
          initList(f.getAbsolutePath());
        } else {
          fileName = f.getName();
          if (fileName.endsWith(".pdf")) {
            list.add(new PdfFile(fileName, f.getAbsolutePath()));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void checkPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED) {
      initViews();
    } else {
      ActivityCompat.requestPermissions(this, new String[] {
          Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
      }, REQUEST_PERMISSION);
    }
  }

  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_PERMISSION: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          initViews();
        } else {
          //permission is denied (this is the first time, when "never ask again" is not checked)
          if (ActivityCompat.shouldShowRequestPermissionRationale(this,
              Manifest.permission.READ_EXTERNAL_STORAGE)) {
            finish();
          }
          //permission is denied (and never ask again is  checked)
          else {
            //shows the dialog describing the importance of permission, so that user should grant
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(
                "Вы откзаались предоставлять разрешение на чтение хранилища.\n\nЭто необходимо для работы приложения."
                    + "\n\n"
                    + "Нажмите \"Предоставить\", чтобы предоставить приложения разрешения.")
                //This will open app information where user can manually grant requested permission
                .setPositiveButton("Предоставить", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                    finish();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                  }
                })
                //close the app
                .setNegativeButton("Отказаться", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                    finish();
                  }
                });
            builder.setCancelable(false);
            builder.create().show();
          }
        }
        break;
      }
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater menuInflater = getMenuInflater();
    menuInflater.inflate(R.menu.pdf_menu, menu);
    btn_create = menu.findItem(R.id.pdf_create);
    btn_create.setVisible(false);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.pdf_create) {
      generatePdf();
    }

    return super.onOptionsItemSelected(item);
  }

  @Override public void onBackPressed() {
    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @SuppressWarnings("StatementWithEmptyBody") @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    switch (id) {
      case R.id.nav_create: {
        btn_create.setVisible(true);
        listView.setVisibility(View.GONE);
        content_create.setVisibility(View.VISIBLE);
        break;
      }
      case R.id.nav_read: {
        btn_create.setVisible(false);
        listView.setVisibility(View.VISIBLE);
        content_create.setVisibility(View.GONE);
        break;
      }
    }

    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  private void generatePdf() {
    DisplayMetrics displaymetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
    float height = displaymetrics.heightPixels;
    float width = displaymetrics.widthPixels;

    int convertHeight = (int) height, convertWidth = (int) width;

    PdfDocument document = new PdfDocument();
    PdfDocument.PageInfo pageInfo =
        new PdfDocument.PageInfo.Builder(convertWidth, convertHeight, 1).create();
    PdfDocument.Page page = document.startPage(pageInfo);

    Canvas canvas = page.getCanvas();

    Paint paint = new Paint();
    canvas.drawPaint(paint);

    Bitmap bitmap = loadBitmapFromView(pdf_layout, pdf_layout.getWidth(), pdf_layout.getHeight());
    bitmap = Bitmap.createScaledBitmap(bitmap, convertWidth, convertHeight, true);

    paint.setColor(Color.BLUE);
    canvas.drawBitmap(bitmap, 0, 0, null);
    document.finishPage(page);

    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PDF");
    if (!dir.exists()) {
      dir.mkdirs();
    }

    // write the document content
    String targetPdf = dir.getAbsolutePath() + "/test.pdf";
    File filePath = new File(targetPdf);
    try {
      document.writeTo(new FileOutputStream(filePath));
      Toast.makeText(getApplicationContext(), "PDf сохранён в " + filePath.getAbsolutePath(),
          Toast.LENGTH_SHORT).show();
      initViews();
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(this, "Something wrong: " + e.toString(), Toast.LENGTH_LONG).show();
    }

    // close the document
    document.close();
  }

  public static Bitmap loadBitmapFromView(View v, int width, int height) {
    Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    v.draw(c);

    return b;
  }
}
