package com.konfuse;

import com.konfuse.bean.Entry;
import com.konfuse.bean.NonLeafNode;

import java.util.ArrayList;

/**
 * @Author: Konfuse
 * @Date: 2019/11/26 16:05
 */
public class Test {
    private ArrayList<Integer> arrayList;

    public Test(ArrayList<Integer> arrayList) {
        this.arrayList = arrayList;
    }

    public ArrayList<Integer> getArrayList() {
        return arrayList;
    }

    public void setArrayList(ArrayList<Integer> arrayList) {
        this.arrayList = arrayList;
    }

    @Override
    public String toString() {
        return "Test{" +
                "arrayList=" + arrayList +
                '}';
    }

    public static void main(String[] args) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(1); arrayList.add(2); arrayList.add(3);
        Test test = new Test(arrayList);
        System.out.println(test);
        arrayList = test.getArrayList();
        arrayList.add(4);
        System.out.println(test);
        Entry entry = new NonLeafNode(1, 1);
        System.out.println(entry instanceof NonLeafNode);
    }
}
