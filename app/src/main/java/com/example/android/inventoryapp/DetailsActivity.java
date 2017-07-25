package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.example.android.inventoryapp.data.ProductProvider.LOG_TAG;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /** Content uri of the current product */
    private Uri currentProductUri;

    /** Uri of the picture of the current product */
    private Uri pictureUri;

    /** Identifier for the product data loader */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    private static final String STATE_PICTURE_URI = "STATE_PICTURE_URI";

    private int quantity;

    private String supplier;

    private String name;

    /** Views to display detailed information */
    private TextView nameTextView;
    private TextView priceTextView;
    private TextView quantityTextView;
    private TextView supplierTextView;
    private ImageView pictureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // get and store the uri received from intent
        currentProductUri = getIntent().getData();

        // find view to display detailed information
        nameTextView = (TextView) findViewById(R.id.details_name);
        priceTextView = (TextView) findViewById(R.id.details_price);
        quantityTextView = (TextView) findViewById(R.id.details_quantity);
        supplierTextView = (TextView) findViewById(R.id.details_supplier);
        pictureView = (ImageView) findViewById(R.id.details_picture);

        // find the buttons to allow user to perform some operations on the data
        Button decreaseQtyBtn = (Button) findViewById(R.id.details_decrease_button);
        Button increaseQtyBtn = (Button) findViewById(R.id.details_increase_button);
        Button deleteProductBtn = (Button) findViewById(R.id.details_delete_button);
        Button orderBtn = (Button) findViewById(R.id.details_order_button);

        // initialize the loader
        getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);

        // set an OnClickListener onto the decrease qty button
        decreaseQtyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // set an OnClickListener onto the increase qty button
        increaseQtyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // set an OnClickListener onto the delete button
        deleteProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });

        // set an OnClickListener onto the order button to send an email to the supplier
        orderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String [] emailAdress = {supplier};
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("*/*");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, emailAdress);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, name + " order");
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(emailIntent);
                }
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define a projection that specifies the columns.
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QTY,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_PRODUCT_PICTURE};

        // run ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                currentProductUri,      // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // sanity check
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Move to the first row of the cursor and read data
        if (cursor.moveToFirst()) {
            // Find the columns of current product attributes
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QTY);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER);
            int pictureColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PICTURE);

            // extract values from the cursor
            name = cursor.getString(nameColumnIndex);
            Float price = cursor.getFloat(priceColumnIndex);
            quantity = cursor.getInt(quantityColumnIndex);
            supplier = cursor.getString(supplierColumnIndex);
            pictureUri = Uri.parse(cursor.getString(pictureColumnIndex));

            // set the views text with the value from the database
            nameTextView.setText(name);
            priceTextView.setText(String.valueOf(price));
            quantityTextView.setText(String.valueOf(quantity));
            supplierTextView.setText(supplier);

            ViewTreeObserver viewTreeObserver= pictureView.getViewTreeObserver();
            viewTreeObserver
                    .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {

                            // convert image uri to bitmap and set it onto the view
                            pictureView.setImageBitmap(getBitmapFromUri(pictureUri));

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                                pictureView.getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);
                            else
                                pictureView.getViewTreeObserver()
                                        .removeGlobalOnLayoutListener(this);
                        }
                    });
        }
    }

    /** Get a bitmap image from the current product Uri
     *
     * I used this post as inspiration :
     * https://discussions.udacity.com/t/unofficial-how-to-pick-an-image-from-the-gallery/314971
     * It was of a great help, thanks a million ! :)
     *
     * @param imageUri uri forom the current product
     * @return a Bitmap image
     */
    public Bitmap getBitmapFromUri(Uri imageUri) {

        // sanity check
        if (imageUri == null || imageUri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int destinationWidth = pictureView.getWidth();
        int destinationHeight = pictureView.getHeight();

        InputStream input = null;

        try {
            input = this.getContentResolver().openInputStream(imageUri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / destinationWidth, photoH / destinationHeight);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button and dismiss deleting the product
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (currentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            int rowsDeleted = getContentResolver().delete(currentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }
}
