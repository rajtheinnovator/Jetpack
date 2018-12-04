package com.enpassio.jetpack;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class WorkManagerActivity extends AppCompatActivity {


    private final int GALLERY_REQUEST_CODE = 300;
    private final int PERMISSIONS_REQUEST_CODE = 301;

    private final int MAX_NUMBER_REQUEST_PERMISSIONS = 2;

    private final String IMAGE_TYPE = "image/*";
    private final String IMAGE_CHOOSER_TITLE = "Select Picture";

    private final String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private int permissionRequestCount = 0;

    private Button pickPhotosButton;
    private android.support.constraint.Group uploadGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_manager);
        initUi();
        requestPermissionsIfNecessary();
    }

    private void requestPermissionsIfNecessary() {
        if (!hasRequiredPermissions()) {
            askForPermissions();
        }
    }

    private void askForPermissions() {
        if (permissionRequestCount < MAX_NUMBER_REQUEST_PERMISSIONS) {
            permissionRequestCount += 1;

            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        } else {
            pickPhotosButton.setEnabled(false);
        }
    }

    private boolean hasRequiredPermissions() {

        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            requestPermissionsIfNecessary();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data != null && resultCode == Activity.RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initUi() {
        uploadGroup = findViewById(R.id.uploadGroup);
        uploadGroup.setVisibility(View.GONE);
        pickPhotosButton = findViewById(R.id.pickPhotosButton);
        pickPhotosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPhotoPicker();
            }
        });
    }

    private void showPhotoPicker() {
        Intent intent = new Intent();
        intent.setType(IMAGE_TYPE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        startActivityForResult(Intent.createChooser(intent, IMAGE_CHOOSER_TITLE), GALLERY_REQUEST_CODE);
    }

}
