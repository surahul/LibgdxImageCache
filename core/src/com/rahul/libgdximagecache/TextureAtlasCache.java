package com.rahul.libgdximagecache;
/**
 * Created by Rahul on 5/9/2015.
 */
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class TextureAtlasCache implements Disposable{

	private TextureAtlas atlas;
	private PixmapPacker packer;
	private int texCount;
	
	private Callback callback;
	
	public TextureAtlasCache(Callback texCallback) {
		atlas = new TextureAtlas();
	    packer = new PixmapPacker(512, 512, Format.RGB565, 2, false);
	    texCount = 0;
	    this.callback = texCallback;
	}
	
	@Override
	public void dispose() {
		if(atlas!=null){
			if(packer!=null)
			{
//			    try{
//			    	updatePacker();
//			    }catch(Exception e){
//			    	Debug.log("e","TextureAtlasCache","error");
//			    }	
			}
			atlas.dispose();
		}
	}
	
	public void addPixmapToPacker(String url,Pixmap pixmap){
		try{
			packer.pack(""+url.hashCode()+"im", pixmap);
			texCount++;
		}catch(Exception e){
			
		}
		
	}
	
	//only to be called on rendering thread
	public void updatePacker(){
		packer.updateTextureAtlas(atlas, TextureFilter.Nearest, TextureFilter.Nearest, false);
		if(texCount>35)
		{
			if(this.callback!=null)
				this.callback.onEvent();
			this.texCount = 0;
		}
	}
	
	public TextureRegion get(String url){
		try{
			if(url.equals(null))
				return null;
			return atlas.findRegion(""+url.hashCode()+"im");
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		
	}

	public void clear() {
		dispose();
		atlas = new TextureAtlas();
	    packer = new PixmapPacker(512, 512, Format.RGB565, 2, false);
	    this.texCount = 0;
	}

	
	
}
