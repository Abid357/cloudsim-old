package org.cloudsimfe;

import java.util.ArrayList;
import java.util.List;

public class Payload {
    private List<Object> data;

    public Payload(List<Object> data){
        this.data = data;
    }

    public Payload(Object singleData){
       data = new ArrayList<>();
       data.add(singleData);
    }

    public Payload(){
        this(new ArrayList<>());
    }

    public List<Object> getData() {
        return data;
    }

    public void setData(List<Object> data) {
        this.data = data;
    }

    public void addData(Object content){
        data.add(content);
    }

    public void removeData(int index){
        data.remove(index);
    }

    @Override
    public String toString() {
        return "Payload{" +
                "data=" + data +
                '}';
    }
}
