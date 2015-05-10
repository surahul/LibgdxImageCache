package com.rahul.libgdximagecache;
/**
 * Created by Rahul on 5/9/2015.
 */
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;

public class ImageLoader implements Disposable{
    
    MemoryCache memoryCache=new MemoryCache();
    TextureAtlasCache atlasCache;
    FileCache fileCache;
    private Map<Image, String> images=Collections.synchronizedMap(new WeakHashMap<Image, String>());
    ExecutorService executorService;
    private Array<Callback> callbacks = new Array<Callback>();
    
    public ImageLoader(){
        fileCache=new FileCache();
        executorService=Executors.newFixedThreadPool(5);
        atlasCache = new TextureAtlasCache(new Callback() {
			
			@Override
			public void onEvent() {
				for(Callback callback : callbacks){
					callback.onEvent();
				}
			}
		});
    }
    public void addTextureAtlasWriteOverflowCallback(Callback callback){
    	this.callbacks.add(callback);
    }
    
    
    
    private TextureRegion stub_region;
    public void setStubRegion(TextureRegion region){
    	this.stub_region = region;
    }
    
    //call from ui thread please
    public void DisplayImage(String url, Image image)
    {
        images.put(image, url);
        TextureRegion region = atlasCache.get(url);
        if(region!=null){
        	image.setScaling(Scaling.fill);
        	image.setDrawable(new TextureRegionDrawable(region));
        	fadeIn(image);
        }else{
        	Pixmap pixmap=memoryCache.get(url);
            if(pixmap!=null){
            	atlasCache.addPixmapToPacker(url, pixmap);
            	atlasCache.updatePacker();
            	region = atlasCache.get(url);
                if(region!=null){
                	image.setScaling(Scaling.fill);
                	image.setDrawable(new TextureRegionDrawable(region));
                	fadeIn(image);
                }else{
                	queuePhoto(url, image);
                    if(stub_region!=null)
                    	image.setDrawable(new TextureRegionDrawable(stub_region));
                }
            }
            else
            {
            	queuePhoto(url, image);
                if(stub_region!=null)
                	image.setDrawable(new TextureRegionDrawable(stub_region));
            }
                
            	
        }
        
    }
        
    private void queuePhoto(String url, Image image)
    {
        PhotoToLoad p=new PhotoToLoad(url, image);
        executorService.submit(new PhotosLoader(p));
    }
    
    
    private void getPixmap(String url,GetPixmapListener listener) 
    {
    	try {
        FileHandle f=fileCache.getFile(url);
        
        //from Local cache
        Pixmap p = decodeFile(f);
        if(p!=null)
            listener.onSuccess(p);
        
        //from web
        
            HttpRequest request = new HttpRequest(Net.HttpMethods.GET);
    	    request.setUrl(url);
    	    Gdx.net.sendHttpRequest(request, new MyHttpResponseListener(listener,url));
            
        } catch (Throwable ex){
           ex.printStackTrace();
           if(ex instanceof OutOfMemoryError)
               {
        	       memoryCache.clear();
        	       atlasCache.clear();
               }
           listener.onError();
        }
    }
    private interface GetPixmapListener{
    	void onSuccess(Pixmap pixmap);
    	void onError();
    }
    private class MyHttpResponseListener implements HttpResponseListener{
        GetPixmapListener listener;
        String url;
    	public MyHttpResponseListener(GetPixmapListener listener, String url) {
			this.listener = listener;
			this.url = url;
		}
		@Override
		public void handleHttpResponse(HttpResponse httpResponse) {
			if(httpResponse.getStatus().getStatusCode()!=200){
				listener.onError();
				return;
			}
			FileHandle f=fileCache.getFile(url);
			f.write(httpResponse.getResultAsStream(),false);
			listener.onSuccess(new Pixmap(fileCache.getFile(url)));
		}

		@Override
		public void failed(Throwable t) {
			listener.onError();
		}

		@Override
		public void cancelled() {
			listener.onError();
		}
    	
    }

    private Pixmap decodeFile(FileHandle f){
        try {
            return new Pixmap(f);
        } catch (Exception e) {
        }
        return null;
    }
    
    //Task for the queue
    private class PhotoToLoad
    {
        public String url;
        public Image image;
        public PhotoToLoad(String u, Image i){
            url=u; 
            image=i;
        }
    }
    
    class PhotosLoader implements Runnable, GetPixmapListener {
        PhotoToLoad photoToLoad;
        Pixmap result = null;
        private boolean complete;
        PhotosLoader(PhotoToLoad photoToLoad){
            this.photoToLoad=photoToLoad;
            this.complete = false;
        }
        
        @Override
        public void run() {
            try{
                if(imageViewReused(photoToLoad))
                    return;
                getPixmap(photoToLoad.url,this);
                int i = 0;
                while(true){
                	try{
                	Thread.sleep(1000);
                	}catch(Exception e){
                		e.printStackTrace();
                		break;
                	}
                	//timeout sort of :P
                	i++;
                	if(i>=60||complete){
                		complete = false;
                	    break;
                	}
                }
                
                
            }catch(Throwable th){
                th.printStackTrace();
            }
        }

		@Override
		public void onSuccess(Pixmap pixmap) {
			
			memoryCache.put(photoToLoad.url, pixmap);
            if(imageViewReused(photoToLoad))
                return;
            atlasCache.addPixmapToPacker(photoToLoad.url, pixmap);
            PixmapDisplayer pd=new PixmapDisplayer(photoToLoad);
            Gdx.app.postRunnable(pd);
            
            this.complete = true;
		}

		@Override
		public void onError() {
			this.complete = true;
		}
    }
    
    boolean imageViewReused(PhotoToLoad photoToLoad){
        String tag=images.get(photoToLoad.image);
        if(tag==null || !tag.equals(photoToLoad.url))
            return true;
        return false;
    }
    
    //Used to display pixmap in the UI thread
    class PixmapDisplayer implements Runnable
    {
        PhotoToLoad photoToLoad;
        public PixmapDisplayer(PhotoToLoad photo){photoToLoad = photo;}
        public void run()
        {
            if(imageViewReused(photoToLoad))
                return;
            atlasCache.updatePacker();
            TextureRegion region = atlasCache.get(photoToLoad.url);
            if(region!=null){
            	photoToLoad.image.setScaling(Scaling.fill);
                photoToLoad.image.setDrawable(new TextureRegionDrawable(region));
                fadeIn(photoToLoad.image);
            }
            else{
                photoToLoad.image.setDrawable(new TextureRegionDrawable(stub_region));
                photoToLoad.image.setScaling(Scaling.fill);
            }
        }
    }

    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
        this.atlasCache.clear();
    }
    public void clearAtlasAndMemoryCache(){
    	atlasCache.clear();
    	memoryCache.clear();
    }
    public void clearAtlasCache(){
    	atlasCache.clear();
    }

	@Override
	public void dispose() {
		this.memoryCache.clear();
		this.atlasCache.clear();
		
	}
	
	
	private void fadeIn(Image image){
		Object lastFadeInTime = image.getUserObject();
		if(lastFadeInTime!=null){
			Long lastFadeInTimeLong = (Long) lastFadeInTime;
			if(System.currentTimeMillis()-lastFadeInTimeLong.longValue()<3000)
			    return;
		}
		image.clearActions();
		image.addAction(Actions.sequence(Actions.alpha(0),Actions.fadeIn(.15f)));
		image.setUserObject(new Long(System.currentTimeMillis()));
	}

}
