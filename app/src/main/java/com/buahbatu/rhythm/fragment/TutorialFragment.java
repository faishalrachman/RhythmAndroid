package com.buahbatu.rhythm.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.buahbatu.rhythm.R;

/**
 * Created by maakbar on 8/12/17.
 */

public class TutorialFragment extends Fragment {
    private static final String IMAGE_KEY = "image";
    private static final String STRING_KEY = "string";

    public static TutorialFragment newInstance(int imageResId, int stringResId){
        TutorialFragment fragment = new TutorialFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(IMAGE_KEY, imageResId);
        bundle.putInt(STRING_KEY, stringResId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutorial, container, false);
        ImageView imageTutor = (ImageView) view.findViewById(R.id.image_tutorial);
        TextView textTutor = (TextView) view.findViewById(R.id.text_tutorial);

        Bundle bundle = getArguments();

        imageTutor.setImageResource(bundle.getInt(IMAGE_KEY));
        textTutor.setText(bundle.getInt(STRING_KEY));

        return view;
    }
}
