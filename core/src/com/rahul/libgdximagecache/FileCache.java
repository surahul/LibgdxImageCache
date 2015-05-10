package com.rahul.libgdximagecache;
/**
 * Created by Rahul on 5/9/2015.
 */
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;


public class FileCache {

    private FileHandle cacheDir;
    
    public FileCache(){
        if (Gdx.app.getType().equals(ApplicationType.Android)&&Gdx.files.isLocalStorageAvailable())
            cacheDir=Gdx.files.local("/cache");
        else
            cacheDir=Gdx.files.external("/cache");
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }
    
    public FileHandle getFile(String url){
        String filename=String.valueOf(url.hashCode());
        FileHandle f = cacheDir.child(filename);
        return f;
    }
    
    public void clear(){
        FileHandle[] files=cacheDir.list();
        if(files==null)
            return;
        for(FileHandle f:files)
            f.delete();
    }

}