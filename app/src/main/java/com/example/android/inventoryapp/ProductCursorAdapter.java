package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

/**
 * {@link CursorAdapter} for inventory app.
 * Create list item from products database.
 */

public class ProductCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = ProductCursorAdapter.class.getSimpleName();

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);

        // Find the columns of product attributes that we're interested in
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QTY);

        // identify the current product ID
        int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        final int currentProductId = cursor.getInt(idColumnIndex);

        // Read the product attributes from the Cursor for the current product
        String productName = cursor.getString(nameColumnIndex);
        String productPrice = String.valueOf(cursor.getFloat(priceColumnIndex));
        int quantityValue = cursor.getInt(quantityColumnIndex);
        String productQuantity = String.valueOf(quantityValue);

        // Update the TextViews with the attributes for the current product
        nameTextView.setText(productName);
        priceTextView.setText(productPrice);
        quantityTextView.setText(productQuantity);

        // declare temporary value for quantity
        final int tempQuantity = quantityValue;

        // set an OnClickListener onto the sale button
        Button saleButton = (Button) view.findViewById(R.id.sale_btn);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int qty = tempQuantity;
                // make sure qty cannot be negative
                if (qty > 0) {
                    // decrement quantity of the current product
                    qty--;

                    // Create a ContentValues object with the updated value of the quantity
                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_PRODUCT_QTY, qty);

                    // create the Uri of the current product
                    Uri updateUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, currentProductId);

                    // update quantity in the database for current product
                    context.getContentResolver().update(updateUri, values, null, null);

                    Log.v(LOG_TAG, "new quantity is: " + qty + " uri is: " + updateUri);
                }
            }
        });
    }
}
