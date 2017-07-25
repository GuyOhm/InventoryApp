package com.example.android.inventoryapp;

import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (pictureUri != null)
            outState.putString(STATE_PICTURE_URI, pictureUri.toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_PICTURE_URI) &&
                !savedInstanceState.getString(STATE_PICTURE_URI).equals("")) {
            pictureUri = Uri.parse(savedInstanceState.getString(STATE_PICTURE_URI));

            ViewTreeObserver viewTreeObserver = pictureView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @TargetApi(16)
                @Override
                public void onGlobalLayout() {
                    pictureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    pictureView.setImageBitmap(getBitmapFromUri(pictureUri));
                }
            });
        }
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
            String name = cursor.getString(nameColumnIndex);
            Float price = cursor.getFloat(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            pictureUri = Uri.parse(cursor.getString(pictureColumnIndex));

            // set the views text with the value from the database
            nameTextView.setText(name);
            priceTextView.setText(String.valueOf(price));
            quantityTextView.setText(String.valueOf(quantity));
            supplierTextView.setText(supplier);

            // convert image uri to bitmap and set it onto the view
            pictureView.setImageBitmap(getBitmapFromUri(pictureUri));
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
}
