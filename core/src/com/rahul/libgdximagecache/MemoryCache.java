package com.rahul.libgdximagecache;
/**
 * Created by Rahul on 5/9/2015.
 */
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;

public class MemoryCache {
    
    private static final String TAG = "MemoryCache";
    private Map<String, Pixmap> cache=Collections.synchronizedMap(
            new LinkedHashMap<String, Pixmap>(10,1.5f,true));//Last argument true for LRU ordering
    private long size=0;//current allocated size
    private long limit=1000000;//max memory in bytes

    public MemoryCache(){
        //use 25% of available heap size
        setLimit(Runtime.getRuntime().maxMemory()/8);
    }
    
    public void setLimit(long new_limit){
        limit=new_limit;
    }

    public Pixmap get(String id){
        try{
            if(!cache.containsKey(id))
                return null;
            return cache.get(id);
        }catch(NullPointerException ex){
            ex.printStackTrace();
            return null;
        }
    }

    public void put(String id, Pixmap pixmap){
        try{
            if(cache.containsKey(id))
                size-=getSizeInBytes(cache.get(id));
            cache.put(id, pixmap);
            size+=getSizeInBytes(pixmap);
            checkSize();
        }catch(Throwable th){
            th.printStackTrace();
        }
    }
    
    private void checkSize() {
        if(size>limit){
            Iterator<Entry<String, Pixmap>> iter=cache.entrySet().iterator();//least recently accessed item will be the first one iterated  
            while(iter.hasNext()){
                Entry<String, Pixmap> entry=iter.next();
                size-=getSizeInBytes(entry.getValue());
                iter.remove();
                if(size<=limit)
                    break;
            }
        }
    }

    public void clear() {
        try{
            cache.clear();
            size=0;
        }catch(NullPointerException ex){
            ex.printStackTrace();
        }
    }

    long getSizeInBytes(Pixmap pixmap) {
        if(pixmap==null)
            return 0;
        return pixmap.getWidth() * pixmap.getHeight()*(pixmap.getFormat()==Format.RGBA8888?4:(pixmap.getFormat()==Format.RGBA4444?2:(pixmap.getFormat()==Format.RGB888?3:(pixmap.getFormat()==Format.RGB565?2:4))));
    }
}