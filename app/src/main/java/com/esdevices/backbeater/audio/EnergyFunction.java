package com.esdevices.backbeater.audio;

/**
 * Created by Alina Kholcheva on 2017-07-10.
 */

class EnergyFunction {
    
    private static final int COUNT = 4;
    
    private double buffer[] = new double[COUNT];
    private int index = -1;
    private double energy = 0f;
    
    public void clear() {
        index = -1;
        energy = 0f;
        for (int i=0; i<COUNT; i++){
            buffer[i] = 0f;
        }
    }
    
    public double push(double value){
        index = (index+1) % COUNT;
    
        double square = value * value;
        double oldValue = buffer[index];
        buffer[index] = square;
        
        energy = energy - oldValue + square;
        
        return energy;
    }
}
