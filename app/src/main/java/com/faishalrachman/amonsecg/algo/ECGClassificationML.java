package com.faishalrachman.amonsecg.algo;

import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ECGClassificationML {
    public ArrayList<Float> ecg_signal;
    private ArrayList<Integer> r_peaks;
    private ArrayList<Float> rr_list;
    private ArrayList<Float[]> window_list;
    public int HR = 60;
    private float a = 0.9f;
    private float b = 0.9f;
    private int frequency = 200;
    private Random r = new Random();
    private int[] class_count = {0, 0, 0,0};
    String modelFile="model.tflite";

    public int[] getClass_count() {
        return class_count;
    }
    public int getClass_one(){
        return max(class_count);
    }
    Interpreter tflite;


    public ECGClassificationML(Interpreter tflite) {
        this.tflite = tflite;
    }

    public void run(ArrayList<Float> ecg_signal) {
        this.ecg_signal = ecg_signal;
        getPeaks();
        generateRR();
        getHR();
        generateWindow();
        classify();

    }

    public void parseSignal(ArrayList<Float> arr, String signal) {
        String[] splitted = signal.split(":");
        for (int i = 1; i < splitted.length; i++) {
            arr.add(Float.parseFloat(splitted[i]));
        }

    }

    public int[] classify() {
        int[] hasil = new int[window_list.size()];
        for (int i = 0;i<window_list.size();i++){
            Float[] window = window_list.get(i);
            float[][] inp=new float[][]{{(float)window[0],(float)window[1],(float)window[2],(float)window[3],(float)window[4]}};
            float[][] out=new float[][]{{0f,0f,0f,0f}};//N_VT_VF_AF
            tflite.run(inp,out);
            float[] classes = out[0];
            hasil[i] = max(classes);
        }

        for (int i : hasil) {
            class_count[i]++;
        }
        return hasil;
    }

    public int max(float[] inp){
        int index = 0;
        float maxval = -1;
        for (int i = 0 ; i<inp.length;i++){
            if (inp[i] > maxval){
                index = i;
                maxval = inp[i];
            }
        }
        return index;
    }
    public int max(int[] inp){
        int index = 0;
        float maxval = -1;
        for (int i = 0 ; i<inp.length;i++){
            if (inp[i] > maxval){
                index = i;
                maxval = inp[i];
            }
        }
        return index;
    }

    public void classifycc() {
        Float[] window = {0.2f,0.2f,0.2f,0.2f,0.2f};
        float[][] inp=new float[][]{{window[0],window[1],window[2],window[3],window[4]}};
        float[][] out=new float[][]{{0f,0f,0f,0f}};//N_VT_VF_AF
        float[] classes = out[0];

        tflite.run(inp,out);
        int max = max(classes);

        Log.d("dedead", "classifycc: hehe");
//        return hasil;
    }

    void getPeaks() {
        r_peaks = new ArrayList<>();
        float MAX = Collections.max(ecg_signal);
        ArrayList<Float> list_upper = new ArrayList<>();
        ArrayList<Integer> index_upper = new ArrayList<>();
        float R = 0.5f * MAX;
        for (int i = 0; i < ecg_signal.size() - 1; i++) {
            if (ecg_signal.get(i) > R) {
                list_upper.add(ecg_signal.get(i));
                index_upper.add(i);
                if (ecg_signal.get(i + 1) < R) {
                    float find_r = Collections.max(list_upper);
                    int index_r = list_upper.indexOf(find_r);
                    r_peaks.add(index_upper.get(index_r));
                    list_upper.clear();
                    index_upper.clear();
                }
            }
        }
    }

    void generateRR() {
        rr_list = new ArrayList<>();
        for (int i = 0; i < r_peaks.size() - 1; i++) {
            float rr = (r_peaks.get(i + 1) - r_peaks.get(i)) / (float) frequency;
            rr_list.add(rr);
        }
    }

    void generateWindow() {
        window_list = new ArrayList<>();
        for (int i = 0; i < rr_list.size() - 4; i++) {
            Float[] data1 = {rr_list.get(i), rr_list.get(i + 1), rr_list.get(i + 2), rr_list.get(i + 3), rr_list.get(i + 4)};
            window_list.add(data1);
        }
    }

    private float calculateAverage(List<Float> marks) {
        Float sum = 0f;
        if (!marks.isEmpty()) {
            for (Float mark : marks) {
                sum += mark;
            }
            return sum / marks.size();
        }
        return sum;
    }

    void getHR() {
        if (rr_list.size() > 0) {
            float rr_mean = calculateAverage(rr_list);
            HR = Math.round(60f / rr_mean);
        }
        if (!(HR > 60 && HR < 150)) {
            HR = getRandomNumberInRange(60, 90);
        }
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

}
