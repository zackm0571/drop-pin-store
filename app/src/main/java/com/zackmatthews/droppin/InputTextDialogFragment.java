package com.zackmatthews.droppin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by zachmathews on 8/20/15.
 */
public class InputTextDialogFragment extends DialogFragment {

    public String getTargetID() {
        return targetID;
    }

    public void setTargetID(String targetID) {
        this.targetID = targetID;
    }

    public String getInitialText() {
        return initialText;
    }

    public void setInitialText(String initialText) {
        this.initialText = initialText;
    }

    public interface InputTextDialogListener{
        void onInputTextDialogSubmitted(InputTextDialogFragment sender, String text);
    }



    InputTextDialogListener mListener;
    private String targetID;
    private String initialText;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());



        LayoutInflater inflater = getActivity().getLayoutInflater();

       final  View v = inflater.inflate(R.layout.enter_pintext, null);

        if(initialText != null) {
            ((EditText) v.findViewById(R.id.pinName)).setText(initialText);
        }
        builder.setMessage("Name pin:").setView(v).
                setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText inputText = (EditText)v.findViewById(R.id.pinName);
                        mListener.onInputTextDialogSubmitted(InputTextDialogFragment.this, inputText.getText().toString());
                    }
                });
        return builder.create();
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (InputTextDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement InputTextDialogListener");
        }
    }
}
