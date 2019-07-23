package com.ipat.dhakarhythmppgganteng.model;


import android.util.Log;

import static java.lang.Math.*;
import java.util.ArrayList;

import uk.me.berndporr.iirj.Butterworth;

import static java.util.Collections.*;

/**
 *
 * @author ipat
 */
public class Classifier {

    double duration(double input){
        return input*150;
    }

    boolean C3(double RR1,double RR2, double RR3){
        return (1.15*RR2) < RR1 && (1.15*RR2) < RR3;
    }
    boolean C4(double RR1,double RR2, double RR3){
        return abs(RR1-RR2) < duration(0.3) && (RR1 < duration(0.8) || RR2 < duration(0.8)) && (RR3 > 1.2 * ((RR1+RR2) / 2));
    }
    boolean C5(double RR1,double RR2, double RR3){
        return abs(RR2-RR3) < duration(0.3) && (RR2 < duration(0.8) || RR3 < duration(0.8)) && (RR1 > 1.2 * ((RR1+RR2) / 2));
    }
    boolean C6(double RR1,double RR2, double RR3){
        return (duration(2.2) < RR2 && RR2 < duration(3.0)) && (abs(RR1-RR2) < duration(0.2) || abs(RR2-RR3) < duration(0.2));
    }


    public ArrayList get_rr_list(ArrayList<Double> raw_signal){
        Butterworth butterworth = new Butterworth();
//        butterworth.bandPass(5,200,200,5);
        ArrayList<Double> filteredsignal = raw_signal;
//        for (int i = 0;i<filteredsignal.size();i++){
//            double nilai = butterworth.filter(filteredsignal.get(i));
//            filteredsignal.set(i,nilai);
//        }
        double MAX = (double) max(filteredsignal);
        double R = 0.2 * MAX;
        ArrayList<Double> list_upper = new ArrayList<>();
        ArrayList<Integer> index_upper = new ArrayList<>();
        ArrayList<Integer> r_peaks = new ArrayList<>();
        ArrayList<Integer> rr_list = new ArrayList<>();
        for (int i = 0;i<filteredsignal.size()-1;i++){
            if (filteredsignal.get(i) > R){
                if (list_upper.size() == 0){
                    list_upper.add(filteredsignal.get(i));
                }
                else{
                    list_upper.add(filteredsignal.get(i));
                    if (filteredsignal.get(i+1) < R){
                        double find_r = max(list_upper);
                        int find_r_in = filteredsignal.indexOf(find_r);
                        r_peaks.add(find_r_in);
                        list_upper.clear();
                    }
                }
            }
        }
        if (r_peaks.size() > 2){
            for (int i = 0;i<r_peaks.size()-1;i++){
                int rr = r_peaks.get(i+1) - r_peaks.get(i);
                rr_list.add(rr);
            }
        }
        return rr_list;
    }
    public int[] beatClassifier(ArrayList<Integer> rr_list){
//    i = 0
        int i = 0;
        int[] rr_category = new int[rr_list.size()];
        for (int x = 0;x<rr_category.length;x++){
            rr_category[x] = 1;
        }
        while (i < rr_list.size() - 2){
            int rr1 = rr_list.get(i);
            int rr2 = rr_list.get(i+1);
            int rr3 = rr_list.get(i+2);
            int pulse = 0;
            if (rr2 < duration(0.6) && 1.8*rr2 < rr1){
                rr_category[i] = 3;
                if (i >= rr_list.size() - 3) {
                    break;
                }
                i += 1;
                if (i >= rr_list.size() - 3) {
                    break;
                }

                rr1 = rr_list.get(i);
                rr2 = rr_list.get(i + 1);
                rr3 = rr_list.get(i + 2);
                pulse = 1;
                while (rr1 + rr2 + rr3 < duration(1.7)) {
                    rr_category[i] = 3;
                    i++;
                    if (i >= rr_list.size() - 3) {
                        break;
                    }

                    rr1 = rr_list.get(i);
                    rr2 = rr_list.get(i + 1);
                    rr3 = rr_list.get(i + 2);
                    pulse++;

                }
                if (pulse < 4) {
                    rr_category[i] = 1;
                    for (int j = 0; j < pulse; j++) {
                        i--;
                    }
                }
            }
            if (C3(rr1,rr2,rr3) || C4(rr1,rr2,rr3) || C5(rr1,rr2,rr3))
                rr_category[i] = 2;
            if (C6(rr1,rr2,rr3))
                rr_category[i] = 4;
            i++;
        }
        int N = 0;
        int VF = 0;
        int PVC = 0;
        int HB = 0;
        for (int j = 0;i<rr_category.length;i++){
            int kategori = rr_category[j];
            if (kategori == 1)
                N++;
            else if (kategori == 2)
                PVC++;
            else if (kategori == 3)
                VF++;
            else if (kategori == 4)
                HB++;
        }
        int[] returnvalue = {N,PVC,VF,HB};
        String hasil = "Normal = " + N +" PVC = " + PVC + " VF = " + VF + " HB = " + HB;
        String arr = "";
        for (int k = 0;k<rr_category.length;k++){
            arr = rr_category[k] + ",";
        }
        Log.e("Detection",arr);
        Log.e("Hasil",hasil);
        return returnvalue;
    }
}