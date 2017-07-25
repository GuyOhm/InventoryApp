package com.example.android.inventoryapp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import static com.example.android.inventoryapp.data.ProductProvider.LOG_TAG;

public class EditorActivity extends AppCompatActivity {

    private static final int CHOOSE_PICTURE_REQUEST = 0;

    private Uri pictureUri;

    private Float priceFloat;

    private Integer quantityInt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        final EditText nameEditText = (EditText) findViewById(R.id.edit_name);
        final EditText priceEditText = (EditText) findViewById(R.id.edit_price);
        final EditText quantityEditText = (EditText) findViewById(R.id.edit_quantity);
        final EditText supplierEditText = (EditText) findViewById(R.id.edit_supplier);

        // find the button
        Button choosePicture = (Button) findViewById(R.id.btn_edit_pic);
        Button saveProduct = (Button) findViewById(R.id.btn_edit_save);

        // set an OnClickListener onto the button and choose a picture
        // I used this post as inspiration :
        // https://discussions.udacity.com/t/unofficial-how-to-pick-an-image-from-the-gallery/314971
        // It was of a great help, thanks a million ! :)
        choosePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Declare intent
                Intent intent;

                // check API version and create intent with the proper action
                if (Build.VERSION.SDK_INT < 19) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                } else {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                }

                // set type of data as image
                intent.setType("image/*");

                // start activity and wait for result
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), CHOOSE_PICTURE_REQUEST);
            }
        });


        // set an OnClickListener onto the save button and save the product
        saveProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check if the picture was taken
                if (pictureUri != null) {
                    // get values from the edit texts
                    String name = nameEditText.getText().toString().trim();
                    String price = priceEditText.getText().toString().trim();
                    String quantity = quantityEditText.getText().toString().trim();
                    String supplier = supplierEditText.getText().toString().trim();

                    if (dataIsValid(name, price, quantity, supplier, pictureUri.toString())) {
                        // Create a ContentValues object
                        ContentValues values = new ContentValues();
                        values.put(ProductEntry.COLUMN_PRODUCT_NAME, name);
                        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceFloat);
                        values.put(ProductEntry.COLUMN_PRODUCT_QTY, quantityInt);
                        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplier);
                        values.put(ProductEntry.COLUMN_PRODUCT_PICTURE, pictureUri.toString());

                        // Insert a new row for the product into the provider using the ContentResolver.
                        getContentResolver().insert(ProductEntry.CONTENT_URI, values);

                        // close activity
                        finish();

                    } else {
                        Toast.makeText(EditorActivity.this, R.string.invalid_input_toast_message, Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(EditorActivity.this, R.string.no_picture_toast_message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        // check if the request code and the result code match our request
        if (requestCode == CHOOSE_PICTURE_REQUEST && resultCode == Activity.RESULT_OK) {

            // get the uri from the received intent
            if (resultData != null) {
                pictureUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + pictureUri.toString());

            }
        }
    }

    private boolean dataIsValid(String name, String price, String quantity, String supplier, String picture) {
        // check if price is empty
        if(!TextUtils.isEmpty(price)){
            // convert to float
            priceFloat = Float.parseFloat(price);
        } else {
            return false;
        }

        // check if quantity is empty
        if(!TextUtils.isEmpty(quantity)){
            // convert to integer
            quantityInt = Integer.parseInt(quantity);
        } else {
            return false;
        }

        if (name == null) {
            return false;
        } else if (priceFloat < 0) {
            return false;
        } else if (quantityInt < 0) {
            return false;
        } else if (supplier == null) {
            return false;
        } else if (picture == null) {
            return false;
        } else {
            return true;
        }
    }
}
