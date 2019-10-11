package com.esdevices.backbeater.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alina Kholcheva on 2017-07-10.
 */

public class WindowQueue {
    
    private int capacity;
    
    private List<Double> buffer;
    private double sum = 0;

    private int avg;
    private int position;


    public WindowQueue(int capacity) {
        this.capacity = capacity;
        position = 0;
        avg = 0;
        buffer = new ArrayList(capacity);
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        clear();
        this.capacity = capacity;
    }
    
    public void clear() {
        sum = 0;
        buffer.clear();
        position = 0;
        avg = 0;
    }
    
    
    
    public WindowQueue enqueue(double value){
        if (buffer.size() == 0){
            sum = 0;
        }
        // remove old value at the beginning
        double valueRemoved = 0;
        if (buffer.size() == capacity) {
            valueRemoved = buffer.remove(0).doubleValue();
        }
        position += 1;
        // add new value to the end
        buffer.add(value);
        
        sum = sum - valueRemoved + value;
        
        return this;
    }
    
    public int average() {
        int size = buffer.size();
        if (size == 0){
            return 0;
        }
        if (position == capacity) {
            position = 0;
            avg = (int)(sum / size);
        }
        return avg;
    }
}
