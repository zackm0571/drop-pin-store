package com.zackmatthews.droppin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by zachmathews on 8/20/15.
 */
public class PinOptionsDialogFragment extends DialogFragment {

    public String getTagID() {
        if(tagID == null){
            return this.getTag();
        }
        return tagID;
    }

    public void setTagID(String tagID) {
        this.tagID = tagID;
    }

    public interface PinOptionsDialogListener{
        void onPinOptionsItemSelected(PinOptionsDialogFragment sender, String option);
    }

    PinOptionsDialogListener mListener;

    private String tagID;

    final CharSequence[] items = {"Navigate", "Delete"};
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setItems(items, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onPinOptionsItemSelected(PinOptionsDialogFragment.this, items[which].toString());
            }
        });
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try{
            mListener = (PinOptionsDialogListener)activity;
        }

        catch(ClassCastException e){
            throw new ClassCastException(activity.toString()
                    + " must implement InputTextDialogListener");
        }

    }
}
