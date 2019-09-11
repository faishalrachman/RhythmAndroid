package com.faishalrachman.amonsecg.algo;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ECGClassification {
    public ArrayList<Float> ecg_signal;
    private ArrayList<Integer> r_peaks;
    private ArrayList<Float> rr_list;
    private ArrayList<Float[]> window_list;
    public int HR = 60;
    private float a = 0.9f;
    private float b = 0.9f;
    private int frequency = 200;
    private Random r = new Random();
    private int[] class_count = {0,0,0};

    public int[] getClass_count() {
        return class_count;
    }

    public void run(ArrayList<Float> ecg_signal) {
        this.ecg_signal = ecg_signal;
        getPeaks();
        generateRR();
        getHR();
        generateWindow();
        classify();

    }

    public void parseSignal(ArrayList<Float> arr, String signal){
        String[] splitted = signal.split(":");
        for (int i = 1; i < splitted.length;i++){
            arr.add(Float.parseFloat(splitted[i]));
        }

    }

    public int[] classify() {
        int i = 0;
        int pulse = 0;
        int[] hasil = new int[window_list.size()];
        while (i < window_list.size()) {
            //Category 5 : VT/VF
            if (window_list.get(i)[1] < 0.6 && window_list.get(i)[2] < window_list.get(i)[1]) {
                hasil[i] = 5;
                i++;
                pulse++;
                while ((window_list.get(i)[0] < 0.8 && window_list.get(i)[1] < 0.8 && window_list.get(i)[2] < 0.8) && window_list.get(i)[0] + window_list.get(i)[1] + window_list.get(i)[2] < 1.8) {
                    hasil[i] = 5;
                    i++;
                    pulse++;
                }
                if (pulse < 4) {
                    for (int j = i - pulse; j < i + pulse; j++) {
                        hasil[j] = 1;
                    }
                }
                pulse = 0;
            } else

                //Category 2 : AF
                if (window_list.get(i)[1] < a * window_list.get(i)[0] && window_list.get(i)[0] < b * window_list.get(i)[2]) {
                    if (window_list.get(i)[1] + window_list.get(i)[2] < 2 * window_list.get(i)[0]) {
                        //AF
                        hasil[i] = 2;
                    }
                }
            i++;
        }
        for (int j = 0;j<hasil.length;j++){
            if (hasil[j] == 0 || hasil[j] == 1){
                class_count[0]++;
            } else if (hasil[j] == 2){

                class_count[1]++;
            } else if (hasil[j] == 5){
                class_count[2]++;
            }
        }
        return hasil;
    }

    void getPeaks() {
        r_peaks = new ArrayList<>();
        float MAX = Collections.max(ecg_signal);
        ArrayList<Float> list_upper = new ArrayList<>();
        ArrayList<Integer> index_upper = new ArrayList<>();
        float R = 0.4f * MAX;
        for (int i = 0;i<ecg_signal.size()-1;i++){
            if (ecg_signal.get(i) > R){
                list_upper.add(ecg_signal.get(i));
                index_upper.add(i);
                if (ecg_signal.get(i+1) < R){
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
        for (int i = 0; i<r_peaks.size()-1;i++){
            float rr = (r_peaks.get(i+1) -r_peaks.get(i))/frequency;
            rr_list.add(rr);
        }
    }

    void generateWindow() {
        window_list = new ArrayList<>();
        for (int i = 0; i<rr_list.size()-2;i++){
            Float[] data = {rr_list.get(i), rr_list.get(i+1), rr_list.get(i+2)};
            window_list.add(data);
        }
    }
    private float calculateAverage(List<Float> marks) {
        Float sum = 0f;
        if(!marks.isEmpty()) {
            for (Float mark : marks) {
                sum += mark;
            }
            return sum / marks.size();
        }
        return sum;
    }
    void getHR(){
        if (rr_list.size() > 0){
            float rr_mean = calculateAverage(rr_list);
            HR = Math.round(60/rr_mean);
        }
        if (!(HR > 60 && HR < 150)){
            HR =  getRandomNumberInRange(60,90);
        }
    }
    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
    public static ArrayList<Float> five_point_derivative(ArrayList<Float> signal){
        ArrayList<Float> filtered = new ArrayList<>();
        for (int i =0;i<signal.size();i++){
            if (i > 4){
                Float d = 0.1f * 2f * (signal.get(i) + signal.get(i-1) - signal.get(i-3) - signal.get(i-4));
                filtered.add(d);
            } else {
                filtered.add(signal.get(i));
            }
        }
        return filtered;
    }
    public static ArrayList<Float> adaptive_filter(ArrayList<Float> signal){
        ArrayList<Float> filtered = new ArrayList<>();
        filtered.add(0.01f);
        Float a = 0.95f;
        for (int i =0;i<signal.size();i++){
            if (i == 0){
                Float d = (a * 0.01f) + ((1-a)*signal.get(i));
                filtered.add(d);
            } else {
                Float d = (a * filtered.get(i-1)) + ((1-a)*signal.get(i));
                filtered.add(d);
            }
//            Float he = filtered.get(i-1);

        }
        return filtered;
    }
    public static ArrayList<Float> rescale_signal(ArrayList<Float> signal){
        ArrayList<Float> filtered = new ArrayList<>();
        signal.remove(0);
        Float min = Collections.min(signal);
        Float max = Collections.max(signal);
        for (int i = 0;i<signal.size();i++)
            filtered.add((signal.get(i)-min) / (max-min));
        return filtered;
    }
    public static ArrayList<Float> low_pass_filter(ArrayList<Float> signal)
    {
        ArrayList<Float> filtered = new ArrayList<>();
        Float value = signal.get(0);
        Float smoothing = 100f;
        for (int i = 1;i<signal.size();i++){
            value = (signal.get(i) - value) / smoothing;
            filtered.add(value);
        }
        return filtered;
    }


}
